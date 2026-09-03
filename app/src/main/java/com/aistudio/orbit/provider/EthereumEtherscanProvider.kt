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

class EthereumEtherscanProvider(
    private val apiKeyPrimary: String = "",
    private val apiKeySecondary: String = ""
) : BlockchainProvider {

    override val id: String = "etherscan_eth"
    override val name: String = "Etherscan & Blockscout (Ethereum & USDT ERC-20)"
    override val network: BlockchainNetwork = BlockchainNetwork.ETHEREUM
    override val isFreeTier: Boolean = true
    override val requiresApiKey: Boolean = false
    override val rateLimitPerMinute: Int = 60

    private val client = ForensicHttpClientFactory.createProviderClient("Etherscan", connectTimeoutSec = 15, readTimeoutSec = 20)

    private val json = Json { ignoreUnknownKeys = true }

    // USDT ERC-20 Contract address on Ethereum Mainnet
    companion object {
        const val USDT_ERC20_CONTRACT = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
        private const val ETHERSCAN_API_URL = "https://api.etherscan.io/api"
        private const val BLOCKSCOUT_API_URL = "https://eth.blockscout.com/api"
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
            val url = if (activeKey.isNotBlank()) {
                "$ETHERSCAN_API_URL?module=proxy&action=eth_blockNumber&apikey=$activeKey"
            } else {
                "$BLOCKSCOUT_API_URL?module=block&action=eth_block_number"
            }
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
            val effectiveUrl = if (activeKey.isNotBlank()) ETHERSCAN_API_URL else BLOCKSCOUT_API_URL
            val keyParam = if (activeKey.isNotBlank()) "&apikey=$activeKey" else ""

            // 1. Fetch native ETH balance
            val ethUrl = "$effectiveUrl?module=account&action=balance&address=$trimmed&tag=latest$keyParam"
            val ethReq = Request.Builder().url(ethUrl).build()
            val ethResp = client.newCall(ethReq).execute()

            var ethBalanceSat = 0L
            if (ethResp.isSuccessful) {
                val body = ethResp.body?.string() ?: ""
                if (body.isNotBlank()) {
                    val root = json.parseToJsonElement(body).jsonObject
                    val resultWei = root["result"]?.jsonPrimitive?.contentOrNull ?: "0"
                    val weiVal = resultWei.toDoubleOrNull() ?: 0.0
                    val ethAmount = weiVal / 1e18
                    ethBalanceSat = (ethAmount * 100_000_000.0).toLong()
                }
            }

            // 2. Fetch USDT ERC-20 token balance
            val usdtUrl = "$effectiveUrl?module=account&action=tokenbalance&contractaddress=$USDT_ERC20_CONTRACT&address=$trimmed&tag=latest$keyParam"
            val usdtReq = Request.Builder().url(usdtUrl).build()
            val usdtResp = client.newCall(usdtReq).execute()

            var usdtBalanceMicro = 0L
            if (usdtResp.isSuccessful) {
                val body = usdtResp.body?.string() ?: ""
                if (body.isNotBlank()) {
                    val root = json.parseToJsonElement(body).jsonObject
                    val resultUnits = root["result"]?.jsonPrimitive?.contentOrNull ?: "0"
                    val microUnits = resultUnits.toLongOrNull() ?: 0L
                    usdtBalanceMicro = microUnits
                }
            }

            Result.success(
                AddressOverviewDto(
                    address = trimmed,
                    network = BlockchainNetwork.ETHEREUM,
                    balanceSat = ethBalanceSat,
                    totalReceivedSat = 0L,
                    totalSentSat = 0L,
                    txCount = 0,
                    providerName = if (activeKey.isNotBlank()) name else "Blockscout Open API",
                    notes = if (usdtBalanceMicro > 0) "USDT (ERC-20) Balance: ${(usdtBalanceMicro.toDouble() / 1e6)} USDT" else "Native ETH"
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
            val effectiveUrl = if (activeKey.isNotBlank()) ETHERSCAN_API_URL else BLOCKSCOUT_API_URL
            val keyParam = if (activeKey.isNotBlank()) "&apikey=$activeKey" else ""
            val txList = mutableListOf<ForensicTransaction>()

            // 1. Fetch USDT ERC-20 Token Transfers
            val tokenUrl = "$effectiveUrl?module=account&action=tokentx&contractaddress=$USDT_ERC20_CONTRACT&address=$trimmed&page=1&offset=$limit&sort=desc$keyParam"
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
                            val tokenDecimal = obj["tokenDecimal"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 6
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
                                sourceName = if (activeKey.isNotBlank()) name else "Blockscout Ethereum",
                                sourceType = DataSourceType.PUBLIC_EXPLORER_API,
                                retrievalTimestamp = System.currentTimeMillis(),
                                endpointUrl = tokenUrl,
                                providerId = id,
                                providerName = if (activeKey.isNotBlank()) name else "Blockscout Ethereum",
                                endpointQuery = tokenUrl,
                                network = "ETHEREUM",
                                rawTxHash = hash,
                                isCache = false
                            )

                            txList.add(
                                ForensicTransaction(
                                    txId = hash,
                                    network = BlockchainNetwork.ETHEREUM,
                                    timestamp = timeStamp,
                                    blockHeight = blockNumber,
                                    confirmations = 24,
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
                                    tokenContract = USDT_ERC20_CONTRACT,
                                    tokenStandard = "ERC-20",
                                    notes = "USDT ERC-20 transfer of $tokenUnits USDT",
                                    provenance = provenance
                                )
                            )
                        }
                    }
                }
            }

            // 2. Fetch Native Ethereum Transactions
            if (txList.size < limit) {
                val nativeUrl = "$effectiveUrl?module=account&action=txlist&address=$trimmed&page=1&offset=${limit - txList.size}&sort=desc$keyParam"
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

                                val ethAmount = (valueWei.toDoubleOrNull() ?: 0.0) / 1e18
                                val internalSat = (ethAmount * 100_000_000.0).toLong()

                                val isSender = from.equals(trimmed, ignoreCase = true)
                                val isReceiver = to.equals(trimmed, ignoreCase = true)
                                val direction = when {
                                    isSender && isReceiver -> TxDirection.SELF_TRANSFER
                                    isSender -> TxDirection.OUTGOING
                                    else -> TxDirection.INCOMING
                                }

                                val provenance = ProvenanceRecord(
                                    sourceName = if (activeKey.isNotBlank()) name else "Blockscout Ethereum",
                                    sourceType = DataSourceType.PUBLIC_EXPLORER_API,
                                    retrievalTimestamp = System.currentTimeMillis(),
                                    endpointUrl = nativeUrl,
                                    providerId = id,
                                    providerName = if (activeKey.isNotBlank()) name else "Blockscout Ethereum",
                                    endpointQuery = nativeUrl,
                                    network = "ETHEREUM",
                                    rawTxHash = hash,
                                    isCache = false
                                )

                                txList.add(
                                    ForensicTransaction(
                                        txId = hash,
                                        network = BlockchainNetwork.ETHEREUM,
                                        timestamp = timeStamp,
                                        blockHeight = blockNumber,
                                        confirmations = 24,
                                        direction = direction,
                                        feeSat = (gasUsed * gasPrice) / 10_000_000_000L,
                                        relevantAmountSat = internalSat,
                                        totalInputValueSat = internalSat,
                                        totalOutputValueSat = internalSat,
                                        inputs = listOf(TxInput(txId = hash, vout = 0, prevOutAddress = from, prevOutValueSat = internalSat)),
                                        outputs = listOf(TxOutput(n = 0, address = to, valueSat = internalSat)),
                                        counterpartyAddresses = if (isSender) listOf(to) else listOf(from),
                                        assetSymbol = "ETH",
                                        isTokenTransfer = false,
                                        notes = "Native ETH transfer of $ethAmount ETH",
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
