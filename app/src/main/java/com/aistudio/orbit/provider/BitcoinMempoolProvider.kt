package com.aistudio.orbit.provider

import com.aistudio.orbit.network.ForensicHttpClientFactory
import com.aistudio.orbit.forensics.AddressValidator
import com.aistudio.orbit.model.AddressValidationResult
import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.ForensicTransaction
import com.aistudio.orbit.model.TxDirection
import com.aistudio.orbit.model.TxInput
import com.aistudio.orbit.model.TxOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

class BitcoinMempoolProvider(
    private val baseUrl: String = "https://mempool.space/api",
    private val client: OkHttpClient = ForensicHttpClientFactory.createProviderClient("Mempool.space", connectTimeoutSec = 15, readTimeoutSec = 15)
) : BlockchainProvider {

    override val id: String = "mempool_space_btc"
    override val name: String = "Mempool.space (Bitcoin Mainnet)"
    override val network: BlockchainNetwork = BlockchainNetwork.BITCOIN
    override val isFree: Boolean = true
    override val requiresApiKey: Boolean = false

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun validateAddress(address: String): AddressValidationResult {
        return AddressValidator.validate(address, BlockchainNetwork.BITCOIN)
    }

    override suspend fun fetchAddressOverview(address: String): Result<AddressOverviewDto> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/address/${address.trim()}"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Orbit-Forensics-Client/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val root = json.parseToJsonElement(body).jsonObject

                val chainStats = root["chain_stats"]?.jsonObject
                val mempoolStats = root["mempool_stats"]?.jsonObject

                val fundedSum = chainStats?.get("funded_txo_sum")?.jsonPrimitive?.longOrNull ?: 0L
                val spentSum = chainStats?.get("spent_txo_sum")?.jsonPrimitive?.longOrNull ?: 0L
                val balance = fundedSum - spentSum
                val txCount = chainStats?.get("tx_count")?.jsonPrimitive?.intOrNull ?: 0

                val unconfirmedFunded = mempoolStats?.get("funded_txo_sum")?.jsonPrimitive?.longOrNull ?: 0L
                val unconfirmedSpent = mempoolStats?.get("spent_txo_sum")?.jsonPrimitive?.longOrNull ?: 0L
                val unconfirmedBalance = unconfirmedFunded - unconfirmedSpent
                val unconfirmedTxCount = mempoolStats?.get("tx_count")?.jsonPrimitive?.intOrNull ?: 0

                Result.success(
                    AddressOverviewDto(
                        address = address.trim(),
                        network = BlockchainNetwork.BITCOIN,
                        balanceSat = balance,
                        totalReceivedSat = fundedSum,
                        totalSentSat = spentSum,
                        transactionCount = txCount,
                        unconfirmedBalanceSat = unconfirmedBalance,
                        unconfirmedTxCount = unconfirmedTxCount,
                        providerName = name
                    )
                )
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
        try {
            val trimmed = address.trim()
            val url = "$baseUrl/address/$trimmed/txs"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Orbit-Forensics-Client/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val txArray = json.parseToJsonElement(body).jsonArray

                val lowerTarget = trimmed.lowercase()
                val resultList = mutableListOf<ForensicTransaction>()

                for (elem in txArray) {
                    val txObj = elem.jsonObject
                    val txid = txObj["txid"]?.jsonPrimitive?.contentOrNull ?: continue
                    val fee = txObj["fee"]?.jsonPrimitive?.longOrNull ?: 0L
                    val size = txObj["size"]?.jsonPrimitive?.intOrNull ?: 0

                    val statusObj = txObj["status"]?.jsonObject
                    val confirmed = statusObj?.get("confirmed")?.jsonPrimitive?.booleanOrNull ?: false
                    val blockHeight = statusObj?.get("block_height")?.jsonPrimitive?.longOrNull
                    val blockTime = statusObj?.get("block_time")?.jsonPrimitive?.longOrNull ?: (System.currentTimeMillis() / 1000)

                    // Inputs
                    val vinArray = txObj["vin"]?.jsonArray ?: JsonArray(emptyList())
                    val inputs = mutableListOf<TxInput>()
                    var targetInputSum = 0L
                    val senderAddresses = mutableListOf<String>()

                    for (vinElem in vinArray) {
                        val vinObj = vinElem.jsonObject
                        val prevTxid = vinObj["txid"]?.jsonPrimitive?.contentOrNull ?: ""
                        val vout = vinObj["vout"]?.jsonPrimitive?.intOrNull ?: 0
                        val prevout = vinObj["prevout"]?.jsonObject
                        val prevAddr = prevout?.get("scriptpubkey_address")?.jsonPrimitive?.contentOrNull
                        val prevVal = prevout?.get("value")?.jsonPrimitive?.longOrNull ?: 0L

                        val isTarget = prevAddr?.lowercase() == lowerTarget
                        if (isTarget) targetInputSum += prevVal
                        if (!prevAddr.isNullOrBlank() && prevAddr.lowercase() != lowerTarget) {
                            senderAddresses.add(prevAddr)
                        }

                        inputs.add(
                            TxInput(
                                txId = prevTxid,
                                vout = vout,
                                prevOutAddress = prevAddr,
                                prevOutValueSat = prevVal,
                                isTargetAddress = isTarget
                            )
                        )
                    }

                    // Outputs
                    val voutArray = txObj["vout"]?.jsonArray ?: JsonArray(emptyList())
                    val outputs = mutableListOf<TxOutput>()
                    var targetOutputSum = 0L
                    val recipientAddresses = mutableListOf<String>()

                    for ((idx, voutElem) in voutArray.withIndex()) {
                        val voutObj = voutElem.jsonObject
                        val outAddr = voutObj["scriptpubkey_address"]?.jsonPrimitive?.contentOrNull
                        val scriptPub = voutObj["scriptpubkey"]?.jsonPrimitive?.contentOrNull
                        val outVal = voutObj["value"]?.jsonPrimitive?.longOrNull ?: 0L

                        val isTarget = outAddr?.lowercase() == lowerTarget
                        if (isTarget) targetOutputSum += outVal
                        if (!outAddr.isNullOrBlank() && outAddr.lowercase() != lowerTarget) {
                            recipientAddresses.add(outAddr)
                        }

                        outputs.add(
                            TxOutput(
                                n = idx,
                                scriptPubKey = scriptPub,
                                address = outAddr,
                                valueSat = outVal,
                                isTargetAddress = isTarget
                            )
                        )
                    }

                    val direction = when {
                        targetInputSum > 0 && targetOutputSum == 0L -> TxDirection.OUTGOING
                        targetInputSum == 0L && targetOutputSum > 0L -> TxDirection.INCOMING
                        targetInputSum > 0 && targetOutputSum > 0L && targetInputSum == targetOutputSum -> TxDirection.SELF_TRANSFER
                        targetInputSum > 0 && targetOutputSum > 0L -> TxDirection.MIXED
                        else -> TxDirection.INCOMING
                    }

                    val relevantAmount = when (direction) {
                        TxDirection.INCOMING -> targetOutputSum
                        TxDirection.OUTGOING -> (targetInputSum - targetOutputSum).coerceAtLeast(0L)
                        TxDirection.SELF_TRANSFER -> targetOutputSum
                        TxDirection.MIXED -> (targetInputSum - targetOutputSum).coerceAtLeast(targetOutputSum)
                        TxDirection.CONTRACT_CALL -> targetOutputSum
                    }

                    val counterparties = (senderAddresses + recipientAddresses).distinct()

                    resultList.add(
                        ForensicTransaction(
                            txId = txid,
                            network = BlockchainNetwork.BITCOIN,
                            timestamp = blockTime,
                            blockHeight = blockHeight,
                            confirmations = if (confirmed) 6 else 0,
                            isConfirmed = confirmed,
                            feeSat = fee,
                            inputs = inputs,
                            outputs = outputs,
                            direction = direction,
                            relevantAmountSat = relevantAmount,
                            counterpartyAddresses = counterparties,
                            isDirectFact = true,
                            rawSize = size
                        )
                    )
                }

                Result.success(resultList.take(limit))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/blocks/tip/height")
                .header("User-Agent", "Orbit-Forensics-Client/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(true)
                else Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
