package com.aistudio.orbit.provider

import com.aistudio.orbit.network.ForensicHttpClientFactory
import com.aistudio.orbit.network.ProviderHealthRegistry
import com.aistudio.orbit.network.ProviderHealthStatus
import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.ForensicTransaction
import com.aistudio.orbit.security.SecureStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

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

    private val client = ForensicHttpClientFactory.createProviderClient("CryptoAPIs", connectTimeoutSec = 15, readTimeoutSec = 20)

    private fun getApiKey(): String {
        return secureStorageManager?.getApiKeyPrimary(id) ?: ""
    }

    override suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isBlank()) return@withContext Result.failure(IllegalStateException("CryptoAPIs API Key missing"))

        val testUrl = "$baseUrl/blockchain-data/bitcoin/mainnet/addresses/1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa/balance"
        val request = Request.Builder()
            .url(testUrl)
            .addHeader("X-API-Key", key)
            .addHeader("Content-Type", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 200) {
                    Result.success(true)
                } else {
                    Result.failure(IllegalStateException("CryptoAPIs connection failed with HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchAddressOverview(address: String): Result<AddressOverviewDto> = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isBlank()) {
            return@withContext Result.failure(IllegalStateException("CryptoAPIs Key not configured"))
        }

        val chain = when (network) {
            BlockchainNetwork.BITCOIN -> "bitcoin/mainnet"
            BlockchainNetwork.ETHEREUM -> "ethereum/mainnet"
            BlockchainNetwork.TRON -> "tron/mainnet"
            BlockchainNetwork.BNB_CHAIN -> "binance-smart-chain/mainnet"
            else -> "bitcoin/mainnet"
        }

        val url = "$baseUrl/blockchain-data/$chain/addresses/$address/balance"
        val request = Request.Builder()
            .url(url)
            .addHeader("X-API-Key", key)
            .addHeader("Content-Type", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("CryptoAPIs returned HTTP ${response.code}"))
                }
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val item = json.optJSONObject("data")?.optJSONObject("item")

                val balanceStr = item?.optJSONObject("confirmedBalance")?.optString("amount") ?: "0"
                val balanceSat = try {
                    (balanceStr.toDouble() * 100_000_000).toLong()
                } catch (e: Exception) {
                    0L
                }

                val dto = AddressOverviewDto(
                    address = address,
                    network = network,
                    balanceSat = balanceSat,
                    totalReceivedSat = balanceSat,
                    totalSentSat = 0L,
                    transactionCount = 1,
                    providerName = name,
                    notes = "CryptoAPIs Verified Ledger Query"
                )
                Result.success(dto)
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
            return@withContext Result.failure(IllegalStateException("CryptoAPIs Key missing"))
        }

        val chain = when (network) {
            BlockchainNetwork.BITCOIN -> "bitcoin/mainnet"
            BlockchainNetwork.ETHEREUM -> "ethereum/mainnet"
            BlockchainNetwork.TRON -> "tron/mainnet"
            BlockchainNetwork.BNB_CHAIN -> "binance-smart-chain/mainnet"
            else -> "bitcoin/mainnet"
        }

        val url = "$baseUrl/blockchain-data/$chain/addresses/$address/transactions?limit=$limit"
        val request = Request.Builder()
            .url(url)
            .addHeader("X-API-Key", key)
            .addHeader("Content-Type", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("CryptoAPIs HTTP ${response.code}"))
                }
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val items = json.optJSONObject("data")?.optJSONArray("items")

                val txList = mutableListOf<ForensicTransaction>()
                if (items != null) {
                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val txid = item.optString("transactionId", item.optString("hash", "tx_$i"))
                        val timestamp = item.optLong("timestamp", System.currentTimeMillis() / 1000)

                        txList.add(
                            ForensicTransaction(
                                txId = txid,
                                network = network,
                                timestamp = timestamp,
                                feeSat = 0L,
                                isConfirmed = true,
                                blockHeight = item.optLong("minedInBlockHash", 0L),
                                inputs = emptyList(),
                                outputs = emptyList()
                            )
                        )
                    }
                }
                Result.success(txList)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
