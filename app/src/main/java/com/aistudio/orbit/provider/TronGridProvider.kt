package com.aistudio.orbit.provider

import com.aistudio.orbit.network.ForensicHttpClientFactory
import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.DataSourceType
import com.aistudio.orbit.model.ForensicTransaction
import com.aistudio.orbit.model.ProvenanceRecord
import com.aistudio.orbit.model.TxDirection
import com.aistudio.orbit.model.TxInput
import com.aistudio.orbit.model.TxOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Request

class TronGridProvider(
    private val apiKeyPrimary: String = "",
    private val apiKeySecondary: String = ""
) : BlockchainProvider {

    override val id: String = "trongrid_tron"
    override val name: String = "TronGrid (TRON & USDT TRC-20)"
    override val network: BlockchainNetwork = BlockchainNetwork.TRON
    override val isFreeTier: Boolean = true
    override val requiresApiKey: Boolean = false
    override val rateLimitPerMinute: Int = 30

    private val client = ForensicHttpClientFactory.createProviderClient("TronGrid", connectTimeoutSec = 15, readTimeoutSec = 20)

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        // USDT TRC-20 Contract on TRON Mainnet
        const val USDT_TRC20_CONTRACT = "TR7NHqJEKQxGTCi8q8ZY4pL8otSzgjLj6t"
        private const val TRONGRID_API_URL = "https://api.trongrid.io"
    }

    private fun getActiveApiKey(): String {
        return when {
            apiKeyPrimary.isNotBlank() -> apiKeyPrimary
            apiKeySecondary.isNotBlank() -> apiKeySecondary
            else -> ""
        }
    }

    private fun addAuthHeaders(builder: Request.Builder): Request.Builder {
        val key = getActiveApiKey()
        if (key.isNotBlank()) {
            builder.addHeader("TRON-PRO-API-KEY", key)
        }
        return builder
    }

    override suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "$TRONGRID_API_URL/wallet/getnowblock"
            val reqBuilder = Request.Builder().url(url)
            val request = addAuthHeaders(reqBuilder).build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchAddressOverview(address: String): Result<AddressOverviewDto> = withContext(Dispatchers.IO) {
        try {
            val trimmed = address.trim()
            val url = "$TRONGRID_API_URL/v1/accounts/$trimmed"
            val reqBuilder = Request.Builder().url(url)
            val request = addAuthHeaders(reqBuilder).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("TronGrid HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: ""
            var balanceSun = 0L
            var usdtTrc20Balance = 0.0

            if (body.isNotBlank()) {
                val root = json.parseToJsonElement(body).jsonObject
                val dataArray = root["data"]?.jsonArray
                if (dataArray != null && dataArray.isNotEmpty()) {
                    val accountObj = dataArray[0].jsonObject
                    balanceSun = accountObj["balance"]?.jsonPrimitive?.longOrNull ?: 0L

                    // Check TRC-20 token balances
                    val trc20Array = accountObj["trc20"]?.jsonArray
                    if (trc20Array != null) {
                        for (tokenElem in trc20Array) {
                            val tokenObj = tokenElem.jsonObject
                            val usdtRaw = tokenObj[USDT_TRC20_CONTRACT]?.jsonPrimitive?.contentOrNull
                            if (usdtRaw != null) {
                                val units = usdtRaw.toDoubleOrNull() ?: 0.0
                                usdtTrc20Balance = units / 1e6
                            }
                        }
                    }
                }
            }

            // Convert Sun (1 TRX = 1,000,000 Sun) to Satoshi-scale (10^8)
            val trxAmount = balanceSun.toDouble() / 1e6
            val internalSat = (trxAmount * 100_000_000.0).toLong()

            Result.success(
                AddressOverviewDto(
                    address = trimmed,
                    network = BlockchainNetwork.TRON,
                    balanceSat = internalSat,
                    totalReceivedSat = 0L,
                    totalSentSat = 0L,
                    txCount = 0,
                    providerName = name,
                    notes = if (usdtTrc20Balance > 0) "USDT (TRC-20) Balance: $usdtTrc20Balance USDT" else "Native TRX"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchTransactions(
        address: String,
        limit: Int,
        offset: Int
    ): Result<List<ForensicTransaction>> = withContext(Dispatchers.IO) {
        try {
            val trimmed = address.trim()
            val txList = mutableListOf<ForensicTransaction>()

            // 1. Fetch TRC-20 USDT Token Transfers
            val trc20Url = "$TRONGRID_API_URL/v1/accounts/$trimmed/transactions/trc20?contract_address=$USDT_TRC20_CONTRACT&limit=$limit"
            val trc20Req = addAuthHeaders(Request.Builder().url(trc20Url)).build()
            val trc20Resp = client.newCall(trc20Req).execute()

            if (trc20Resp.isSuccessful) {
                val body = trc20Resp.body?.string() ?: ""
                if (body.isNotBlank()) {
                    val root = json.parseToJsonElement(body).jsonObject
                    val dataArray = root["data"]?.jsonArray ?: JsonArray(emptyList())
                    for (elem in dataArray) {
                        val obj = elem.jsonObject
                        val txId = obj["transaction_id"]?.jsonPrimitive?.contentOrNull ?: continue
                        val from = obj["from"]?.jsonPrimitive?.contentOrNull ?: ""
                        val to = obj["to"]?.jsonPrimitive?.contentOrNull ?: ""
                        val valueStr = obj["value"]?.jsonPrimitive?.contentOrNull ?: "0"
                        val blockTs = obj["block_timestamp"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
                        val tokenInfo = obj["token_info"]?.jsonObject
                        val symbol = tokenInfo?.get("symbol")?.jsonPrimitive?.contentOrNull ?: "USDT"
                        val decimals = tokenInfo?.get("decimals")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 6

                        val rawVal = valueStr.toDoubleOrNull() ?: 0.0
                        val divisor = Math.pow(10.0, decimals.toDouble())
                        val tokenAmount = rawVal / divisor
                        val internalSat = (tokenAmount * 100_000_000.0).toLong()

                        val isSender = from.equals(trimmed, ignoreCase = true)
                        val isReceiver = to.equals(trimmed, ignoreCase = true)
                        val direction = when {
                            isSender && isReceiver -> TxDirection.SELF_TRANSFER
                            isSender -> TxDirection.OUTGOING
                            else -> TxDirection.INCOMING
                        }

                        val provenance = ProvenanceRecord(
                            sourceName = name,
                            sourceType = DataSourceType.PUBLIC_EXPLORER_API,
                            retrievalTimestamp = System.currentTimeMillis(),
                            endpointUrl = trc20Url,
                            providerId = id,
                            providerName = name,
                            endpointQuery = trc20Url,
                            network = "TRON",
                            rawTxHash = txId,
                            isCache = false
                        )

                        txList.add(
                            ForensicTransaction(
                                txId = txId,
                                network = BlockchainNetwork.TRON,
                                timestamp = blockTs / 1000L,
                                confirmations = 19,
                                direction = direction,
                                relevantAmountSat = internalSat,
                                totalInputValueSat = internalSat,
                                totalOutputValueSat = internalSat,
                                inputs = listOf(TxInput(txId = txId, vout = 0, prevOutAddress = from, prevOutValueSat = internalSat)),
                                outputs = listOf(TxOutput(n = 0, address = to, valueSat = internalSat)),
                                counterpartyAddresses = if (isSender) listOf(to) else listOf(from),
                                assetSymbol = symbol,
                                isTokenTransfer = true,
                                tokenContract = USDT_TRC20_CONTRACT,
                                tokenStandard = "TRC-20",
                                notes = "TRC-20 Transfer of $tokenAmount $symbol",
                                provenance = provenance
                            )
                        )
                    }
                }
            }

            // 2. Fetch Native TRX Transactions if quota remains
            if (txList.size < limit) {
                val nativeUrl = "$TRONGRID_API_URL/v1/accounts/$trimmed/transactions?limit=${limit - txList.size}"
                val nativeReq = addAuthHeaders(Request.Builder().url(nativeUrl)).build()
                val nativeResp = client.newCall(nativeReq).execute()

                if (nativeResp.isSuccessful) {
                    val body = nativeResp.body?.string() ?: ""
                    if (body.isNotBlank()) {
                        val root = json.parseToJsonElement(body).jsonObject
                        val dataArray = root["data"]?.jsonArray ?: JsonArray(emptyList())
                        for (elem in dataArray) {
                            val obj = elem.jsonObject
                            val txId = obj["txID"]?.jsonPrimitive?.contentOrNull ?: continue
                            if (txList.any { it.txId == txId }) continue

                            val rawData = obj["raw_data"]?.jsonObject
                            val blockTs = rawData?.get("timestamp")?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
                            val fee = obj["net_fee"]?.jsonPrimitive?.longOrNull ?: 0L

                            // Parse transfer contract value
                            val contractArray = rawData?.get("contract")?.jsonArray
                            var amountSun = 0L
                            var ownerAddress = ""
                            var toAddress = ""

                            if (contractArray != null && contractArray.isNotEmpty()) {
                                val contractParam = contractArray[0].jsonObject["parameter"]?.jsonObject?.get("value")?.jsonObject
                                amountSun = contractParam?.get("amount")?.jsonPrimitive?.longOrNull ?: 0L
                                ownerAddress = contractParam?.get("owner_address")?.jsonPrimitive?.contentOrNull ?: ""
                                toAddress = contractParam?.get("to_address")?.jsonPrimitive?.contentOrNull ?: ""
                            }

                            val trxAmount = amountSun.toDouble() / 1e6
                            val internalSat = (trxAmount * 100_000_000.0).toLong()

                            val isSender = ownerAddress.equals(trimmed, ignoreCase = true)
                            val isReceiver = toAddress.equals(trimmed, ignoreCase = true)
                            val direction = when {
                                isSender && isReceiver -> TxDirection.SELF_TRANSFER
                                isSender -> TxDirection.OUTGOING
                                else -> TxDirection.INCOMING
                            }

                            val provenance = ProvenanceRecord(
                                sourceName = name,
                                sourceType = DataSourceType.PUBLIC_EXPLORER_API,
                                retrievalTimestamp = System.currentTimeMillis(),
                                endpointUrl = nativeUrl,
                                providerId = id,
                                providerName = name,
                                endpointQuery = nativeUrl,
                                network = "TRON",
                                rawTxHash = txId,
                                isCache = false
                            )

                            txList.add(
                                ForensicTransaction(
                                    txId = txId,
                                    network = BlockchainNetwork.TRON,
                                    timestamp = blockTs / 1000L,
                                    confirmations = 19,
                                    direction = direction,
                                    feeSat = fee,
                                    relevantAmountSat = internalSat,
                                    totalInputValueSat = internalSat,
                                    totalOutputValueSat = internalSat,
                                    inputs = listOf(TxInput(txId = txId, vout = 0, prevOutAddress = ownerAddress, prevOutValueSat = internalSat)),
                                    outputs = listOf(TxOutput(n = 0, address = toAddress, valueSat = internalSat)),
                                    counterpartyAddresses = if (isSender) listOf(toAddress) else listOf(ownerAddress),
                                    assetSymbol = "TRX",
                                    isTokenTransfer = false,
                                    notes = "Native TRX transfer of $trxAmount TRX",
                                    provenance = provenance
                                )
                            )
                        }
                    }
                }
            }

            Result.success(txList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
