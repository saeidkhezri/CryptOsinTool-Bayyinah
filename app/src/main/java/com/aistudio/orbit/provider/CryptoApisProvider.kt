package com.aistudio.orbit.provider

import com.aistudio.orbit.network.ForensicHttpClientFactory
import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.ForensicTransaction
import com.aistudio.orbit.model.TxInput
import com.aistudio.orbit.model.TxOutput
import com.aistudio.orbit.security.SecureStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/**
 * CryptoApisProvider
 * Official CryptoAPIs 2.0 multi-chain adapter (Prompt 3 §5, Master Instruction §26)
 * Strictly separates block hash from block height, respects provider indexing,
 * handles HTTP error status codes (401, 403, 429, 500), and parses transaction inputs/outputs.
 */
class CryptoApisProvider(
    private val secureStorageManager: SecureStorageManager?,
    override val network: BlockchainNetwork = BlockchainNetwork.BITCOIN
) : BlockchainProvider {

    override val id: String = "cryptoapis_multi"
    override val name: String = "CryptoAPIs (Multi-Chain & AML)"
    override val isFreeTier: Boolean = false
    override val requiresApiKey: Boolean = true
    override val rateLimitPerMinute: Int = 120

    private val baseUrl = "https://rest.cryptoapis.io/v2"

    private val client = ForensicHttpClientFactory.createProviderClient("CryptoAPIs", connectTimeoutSec = 15, readTimeoutSec = 25)

    private fun getApiKey(): String {
        return secureStorageManager?.getApiKeyPrimary(id) ?: ""
    }

    private fun getChainPath(): String {
        return when (network) {
            BlockchainNetwork.BITCOIN -> "bitcoin/mainnet"
            BlockchainNetwork.ETHEREUM -> "ethereum/mainnet"
            BlockchainNetwork.TRON -> "tron/mainnet"
            BlockchainNetwork.BNB_CHAIN -> "binance-smart-chain/mainnet"
            else -> "bitcoin/mainnet"
        }
    }

    override suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isBlank()) return@withContext Result.failure(IllegalStateException("CryptoAPIs API Key not configured"))

        // Uses metadata/info endpoint instead of querying arbitrary addresses
        val testUrl = "$baseUrl/blockchain-data/${getChainPath()}/blocks/last"
        val request = Request.Builder()
            .url(testUrl)
            .addHeader("X-API-Key", key.trim())
            .addHeader("Accept", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200, 201 -> Result.success(true)
                    401, 403 -> Result.failure(IllegalStateException("CryptoAPIs API Key is invalid or unauthorized (HTTP ${response.code})"))
                    429 -> Result.failure(IllegalStateException("CryptoAPIs rate limit or credit quota exceeded (HTTP 429)"))
                    else -> Result.failure(IllegalStateException("CryptoAPIs connection test failed with HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchAddressOverview(address: String): Result<AddressOverviewDto> = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isBlank()) {
            return@withContext Result.failure(IllegalStateException("CryptoAPIs API Key is required"))
        }

        val url = "$baseUrl/blockchain-data/${getChainPath()}/addresses/${address.trim()}/balance"
        val request = Request.Builder()
            .url(url)
            .addHeader("X-API-Key", key.trim())
            .addHeader("Accept", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val bodyStr = response.body?.string() ?: ""
                        val json = JSONObject(bodyStr)
                        val item = json.optJSONObject("data")?.optJSONObject("item")

                        val confirmedBalance = item?.optJSONObject("confirmedBalance")
                        val balanceStr = confirmedBalance?.optString("amount", "0") ?: "0"
                        val balanceSat = try {
                            (balanceStr.toDouble() * 100_000_000).toLong()
                        } catch (e: Exception) {
                            0L
                        }

                        val totalReceivedStr = item?.optJSONObject("totalReceived")?.optString("amount", balanceStr) ?: balanceStr
                        val totalReceivedSat = try {
                            (totalReceivedStr.toDouble() * 100_000_000).toLong()
                        } catch (e: Exception) {
                            balanceSat
                        }

                        val totalSpentStr = item?.optJSONObject("totalSpent")?.optString("amount", "0") ?: "0"
                        val totalSentSat = try {
                            (totalSpentStr.toDouble() * 100_000_000).toLong()
                        } catch (e: Exception) {
                            0L
                        }

                        val txCount = item?.optInt("transactionsCount", 1) ?: 1

                        val dto = AddressOverviewDto(
                            address = address.trim(),
                            network = network,
                            balanceSat = balanceSat,
                            totalReceivedSat = totalReceivedSat,
                            totalSentSat = totalSentSat,
                            transactionCount = txCount,
                            providerName = name,
                            notes = "CryptoAPIs Verified Ledger Query (Chain: ${getChainPath()})"
                        )
                        Result.success(dto)
                    }
                    401, 403 -> Result.failure(IllegalStateException("CryptoAPIs Authentication failed: API Key invalid or expired"))
                    404 -> Result.failure(IllegalStateException("Address ${address.take(8)}... not found or has no transactions"))
                    429 -> Result.failure(IllegalStateException("CryptoAPIs quota exceeded (HTTP 429 Rate Limit)"))
                    else -> Result.failure(IllegalStateException("CryptoAPIs returned HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchTransactions(
        address: String,
        limit: Int,
        offset: Int
    ): Result<List<ForensicTransaction>> = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isBlank()) {
            return@withContext Result.failure(IllegalStateException("CryptoAPIs API Key missing"))
        }

        val clampedLimit = limit.coerceIn(1, 50)
        val url = "$baseUrl/blockchain-data/${getChainPath()}/addresses/${address.trim()}/transactions?limit=$clampedLimit&offset=$offset"
        val request = Request.Builder()
            .url(url)
            .addHeader("X-API-Key", key.trim())
            .addHeader("Accept", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val bodyStr = response.body?.string() ?: ""
                        val json = JSONObject(bodyStr)
                        val items = json.optJSONObject("data")?.optJSONArray("items")

                        val txList = mutableListOf<ForensicTransaction>()
                        if (items != null) {
                            for (i in 0 until items.length()) {
                                val item = items.optJSONObject(i) ?: continue
                                val txid = item.optString("transactionId", item.optString("transactionHash", item.optString("hash", "tx_$i")))
                                val timestamp = item.optLong("timestamp", System.currentTimeMillis() / 1000)
                                
                                // Strictly distinguish block hash from block height (Master Instruction §26)
                                val blockHeight = item.optLong("minedInBlockHeight", 0L)
                                val blockHash = item.optString("minedInBlockHash", "")

                                val feeObj = item.optJSONObject("fee")
                                val feeSat = try {
                                    val feeAmount = feeObj?.optString("amount", "0")?.toDouble() ?: 0.0
                                    (feeAmount * 100_000_000).toLong()
                                } catch (e: Exception) {
                                    0L
                                }

                                // Parse senders as inputs
                                val senders = item.optJSONArray("senders")
                                val inputs = mutableListOf<TxInput>()
                                if (senders != null) {
                                    for (j in 0 until senders.length()) {
                                        val s = senders.optJSONObject(j) ?: continue
                                        val sAddr = s.optString("address", "")
                                        val sAmount = try {
                                            (s.optString("amount", "0").toDouble() * 100_000_000).toLong()
                                        } catch (e: Exception) { 0L }
                                        inputs.add(TxInput(prevOutAddress = sAddr, prevOutValueSat = sAmount))
                                    }
                                }

                                // Parse recipients as outputs
                                val recipients = item.optJSONArray("recipients")
                                val outputs = mutableListOf<TxOutput>()
                                if (recipients != null) {
                                    for (k in 0 until recipients.length()) {
                                        val r = recipients.optJSONObject(k) ?: continue
                                        val rAddr = r.optString("address", "")
                                        val rAmount = try {
                                            (r.optString("amount", "0").toDouble() * 100_000_000).toLong()
                                        } catch (e: Exception) { 0L }
                                        outputs.add(TxOutput(address = rAddr, valueSat = rAmount))
                                    }
                                }

                                txList.add(
                                    ForensicTransaction(
                                        txId = txid,
                                        network = network,
                                        timestamp = timestamp,
                                        feeSat = feeSat,
                                        isConfirmed = blockHeight > 0L,
                                        blockHeight = blockHeight,
                                        inputs = inputs,
                                        outputs = outputs
                                    )
                                )
                            }
                        }
                        Result.success(txList)
                    }
                    401, 403 -> Result.failure(IllegalStateException("CryptoAPIs Authentication failed: Invalid API key"))
                    429 -> Result.failure(IllegalStateException("CryptoAPIs Rate limit exceeded (HTTP 429)"))
                    404 -> Result.success(emptyList()) // No transactions yet
                    else -> Result.failure(IllegalStateException("CryptoAPIs returned HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
