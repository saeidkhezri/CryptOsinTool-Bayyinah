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

class BscScanProvider(
    private val apiKeyPrimary: String = "",
    private val apiKeySecondary: String = ""
) : BlockchainProvider {

    override val id: String = "bscscan_bnb"
    override val name: String = "BscScan (BNB Smart Chain & USDT BEP-20)"
    override val network: BlockchainNetwork = BlockchainNetwork.BNB_CHAIN
    override val isFreeTier: Boolean = true
    override val requiresApiKey: Boolean = false
    override val rateLimitPerMinute: Int = 60

    private val client = ForensicHttpClientFactory.createProviderClient("BscScan", connectTimeoutSec = 15, readTimeoutSec = 20)

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        // USDT BEP-20 Contract on BNB Smart Chain
        const val USDT_BEP20_CONTRACT = "0x55d398326f99059fF775485246999027B3197955"
        private const val BSCSCAN_API_URL = "https://api.bscscan.com/api"
    }

    private fun getActiveApiKey(): String {
        return when {
            apiKeyPrimary.isNotBlank() -> apiKeyPrimary
            apiKeySecondary.isNotBlank() -> apiKeySecondary
            else -> ""
        }
    }

    override suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val activeKey = getActiveApiKey()
            val url = "$BSCSCAN_API_URL?module=proxy&action=eth_blockNumber${if (activeKey.isNotBlank()) "&apikey=$activeKey" else ""}"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchAddressOverview(address: String): Result<AddressOverviewDto> = withContext(Dispatchers.IO) {
        try {
            val trimmed = address.trim()
            val activeKey = getActiveApiKey()
            val keyParam = if (activeKey.isNotBlank()) "&apikey=$activeKey" else ""

            // 1. Fetch native BNB Balance
            val bnbUrl = "$BSCSCAN_API_URL?module=account&action=balance&address=$trimmed&tag=latest$keyParam"
            val bnbReq = Request.Builder().url(bnbUrl).build()
            val bnbResp = client.newCall(bnbReq).execute()

            var bnbBalanceSat = 0L
            if (bnbResp.isSuccessful) {
                val body = bnbResp.body?.string() ?: ""
                if (body.isNotBlank()) {
                    val root = json.parseToJsonElement(body).jsonObject
                    val resultWei = root["result"]?.jsonPrimitive?.contentOrNull ?: "0"
                    val weiVal = resultWei.toDoubleOrNull() ?: 0.0
                    val bnbAmount = weiVal / 1e18
                    bnbBalanceSat = (bnbAmount * 100_000_000.0).toLong()
                }
            }

            // 2. Fetch BEP-20 USDT Balance
            val usdtUrl = "$BSCSCAN_API_URL?module=account&action=tokenbalance&contractaddress=$USDT_BEP20_CONTRACT&address=$trimmed&tag=latest$keyParam"
            val usdtReq = Request.Builder().url(usdtUrl).build()
            val usdtResp = client.newCall(usdtReq).execute()

            var usdtBalanceMicro = 0.0
            if (usdtResp.isSuccessful) {
                val body = usdtResp.body?.string() ?: ""
                if (body.isNotBlank()) {
                    val root = json.parseToJsonElement(body).jsonObject
                    val resultUnits = root["result"]?.jsonPrimitive?.contentOrNull ?: "0"
                    val rawVal = resultUnits.toDoubleOrNull() ?: 0.0
                    usdtBalanceMicro = rawVal / 1e18 // BEP-20 USDT has 18 decimals
                }
            }

            Result.success(
                AddressOverviewDto(
                    address = trimmed,
                    network = BlockchainNetwork.BNB_CHAIN,
                    balanceSat = bnbBalanceSat,
                    totalReceivedSat = 0L,
                    totalSentSat = 0L,
                    txCount = 0,
                    providerName = name,
                    notes = if (usdtBalanceMicro > 0) "USDT (BEP-20) Balance: $usdtBalanceMicro USDT" else "Native BNB"
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
            val activeKey = getActiveApiKey()
            val keyParam = if (activeKey.isNotBlank()) "&apikey=$activeKey" else ""
            val txList = mutableListOf<ForensicTransaction>()

            // 1. Fetch BEP-20 USDT Token Transfers
            val tokenUrl = "$BSCSCAN_API_URL?module=account&action=tokentx&contractaddress=$USDT_BEP20_CONTRACT&address=$trimmed&page=1&offset=$limit&sort=desc$keyParam"
            val tokenReq = Request.Builder().url(tokenUrl).build()
            val tokenResp = client.newCall(tokenReq).execute()

            if (tokenResp.isSuccessful) {
                val body = tokenResp.body?.string() ?: ""
                if (body.isNotBlank()) {
                    val root = json.parseToJsonElement(body).jsonObject
                    val status = root["status"]?.jsonPrimitive?.contentOrNull ?: "0"
                    if (status == "1") {
                        val resultArray = root["result"]?.jsonArray ?: JsonArray(emptyList())
                        for (item in resultArray) {
                            val obj = item.jsonObject
                            val hash = obj["hash"]?.jsonPrimitive?.contentOrNull ?: continue
                            val from = obj["from"]?.jsonPrimitive?.contentOrNull ?: ""
                            val to = obj["to"]?.jsonPrimitive?.contentOrNull ?: ""
                            val valueStr = obj["value"]?.jsonPrimitive?.contentOrNull ?: "0"
                            val timeStamp = obj["timeStamp"]?.jsonPrimitive?.longOrNull ?: (System.currentTimeMillis() / 1000)
                            val tokenSymbol = obj["tokenSymbol"]?.jsonPrimitive?.contentOrNull ?: "USDT"
                            val tokenDecimal = obj["tokenDecimal"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 18
                            val blockNumber = obj["blockNumber"]?.jsonPrimitive?.longOrNull
                            val gasUsed = obj["gasUsed"]?.jsonPrimitive?.longOrNull ?: 0L
                            val gasPrice = obj["gasPrice"]?.jsonPrimitive?.longOrNull ?: 0L

                            val rawValue = valueStr.toDoubleOrNull() ?: 0.0
                            val divisor = Math.pow(10.0, tokenDecimal.toDouble())
                            val tokenUnits = rawValue / divisor
                            val internalSat = (tokenUnits * 100_000_000.0).toLong()

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
                                endpointUrl = tokenUrl,
                                providerId = id,
                                providerName = name,
                                endpointQuery = tokenUrl,
                                network = "BNB_CHAIN",
                                rawTxHash = hash,
                                isCache = false
                            )

                            txList.add(
                                ForensicTransaction(
                                    txId = hash,
                                    network = BlockchainNetwork.BNB_CHAIN,
                                    timestamp = timeStamp,
                                    blockHeight = blockNumber,
                                    confirmations = 15,
                                    direction = direction,
                                    feeSat = (gasUsed * gasPrice) / 10_000_000_000L,
                                    relevantAmountSat = internalSat,
                                    totalInputValueSat = internalSat,
                                    totalOutputValueSat = internalSat,
                                    inputs = listOf(TxInput(txId = hash, vout = 0, prevOutAddress = from, prevOutValueSat = internalSat)),
                                    outputs = listOf(TxOutput(n = 0, address = to, valueSat = internalSat)),
                                    counterpartyAddresses = if (isSender) listOf(to) else listOf(from),
                                    assetSymbol = tokenSymbol,
                                    isTokenTransfer = true,
                                    tokenContract = USDT_BEP20_CONTRACT,
                                    tokenStandard = "BEP-20",
                                    notes = "BEP-20 Transfer of $tokenUnits USDT",
                                    provenance = provenance
                                )
                            )
                        }
                    }
                }
            }

            // 2. Fetch Native BNB Transactions
            if (txList.size < limit) {
                val nativeUrl = "$BSCSCAN_API_URL?module=account&action=txlist&address=$trimmed&page=1&offset=${limit - txList.size}&sort=desc$keyParam"
                val nativeReq = Request.Builder().url(nativeUrl).build()
                val nativeResp = client.newCall(nativeReq).execute()

                if (nativeResp.isSuccessful) {
                    val body = nativeResp.body?.string() ?: ""
                    if (body.isNotBlank()) {
                        val root = json.parseToJsonElement(body).jsonObject
                        val status = root["status"]?.jsonPrimitive?.contentOrNull ?: "0"
                        if (status == "1") {
                            val resultArray = root["result"]?.jsonArray ?: JsonArray(emptyList())
                            for (item in resultArray) {
                                val obj = item.jsonObject
                                val hash = obj["hash"]?.jsonPrimitive?.contentOrNull ?: continue
                                if (txList.any { it.txId == hash }) continue

                                val from = obj["from"]?.jsonPrimitive?.contentOrNull ?: ""
                                val to = obj["to"]?.jsonPrimitive?.contentOrNull ?: ""
                                val valueWei = obj["value"]?.jsonPrimitive?.contentOrNull ?: "0"
                                val timeStamp = obj["timeStamp"]?.jsonPrimitive?.longOrNull ?: (System.currentTimeMillis() / 1000)
                                val blockNumber = obj["blockNumber"]?.jsonPrimitive?.longOrNull
                                val gasUsed = obj["gasUsed"]?.jsonPrimitive?.longOrNull ?: 0L
                                val gasPrice = obj["gasPrice"]?.jsonPrimitive?.longOrNull ?: 0L

                                val bnbAmount = (valueWei.toDoubleOrNull() ?: 0.0) / 1e18
                                val internalSat = (bnbAmount * 100_000_000.0).toLong()

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
                                    endpointUrl = nativeUrl,
                                    providerId = id,
                                    providerName = name,
                                    endpointQuery = nativeUrl,
                                    network = "BNB_CHAIN",
                                    rawTxHash = hash,
                                    isCache = false
                                )

                                txList.add(
                                    ForensicTransaction(
                                        txId = hash,
                                        network = BlockchainNetwork.BNB_CHAIN,
                                        timestamp = timeStamp,
                                        blockHeight = blockNumber,
                                        confirmations = 15,
                                        direction = direction,
                                        feeSat = (gasUsed * gasPrice) / 10_000_000_000L,
                                        relevantAmountSat = internalSat,
                                        totalInputValueSat = internalSat,
                                        totalOutputValueSat = internalSat,
                                        inputs = listOf(TxInput(txId = hash, vout = 0, prevOutAddress = from, prevOutValueSat = internalSat)),
                                        outputs = listOf(TxOutput(n = 0, address = to, valueSat = internalSat)),
                                        counterpartyAddresses = if (isSender) listOf(to) else listOf(from),
                                        assetSymbol = "BNB",
                                        isTokenTransfer = false,
                                        notes = "Native BNB transfer of $bnbAmount BNB",
                                        provenance = provenance
                                    )
                                )
                            }
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
