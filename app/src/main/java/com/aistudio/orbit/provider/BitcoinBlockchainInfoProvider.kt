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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

class BitcoinBlockchainInfoProvider(
    private val baseUrl: String = "https://blockchain.info",
    private val client: OkHttpClient = ForensicHttpClientFactory.createProviderClient("Blockchain.info", connectTimeoutSec = 15, readTimeoutSec = 15)
) : BlockchainProvider {

    override val id: String = "blockchain_info_btc"
    override val name: String = "Blockchain.info (Bitcoin Public API)"
    override val network: BlockchainNetwork = BlockchainNetwork.BITCOIN
    override val isFree: Boolean = true
    override val requiresApiKey: Boolean = false

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun validateAddress(address: String): AddressValidationResult {
        return AddressValidator.validate(address, BlockchainNetwork.BITCOIN)
    }

    override suspend fun fetchAddressOverview(address: String): Result<AddressOverviewDto> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/rawaddr/${address.trim()}?limit=1"
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

                val nTx = root["n_tx"]?.jsonPrimitive?.intOrNull ?: 0
                val totalReceived = root["total_received"]?.jsonPrimitive?.longOrNull ?: 0L
                val totalSent = root["total_sent"]?.jsonPrimitive?.longOrNull ?: 0L
                val finalBalance = root["final_balance"]?.jsonPrimitive?.longOrNull ?: 0L

                Result.success(
                    AddressOverviewDto(
                        address = address.trim(),
                        network = BlockchainNetwork.BITCOIN,
                        balanceSat = finalBalance,
                        totalReceivedSat = totalReceived,
                        totalSentSat = totalSent,
                        transactionCount = nTx,
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
            val lowerTarget = trimmed.lowercase()
            val resultList = mutableListOf<ForensicTransaction>()
            val targetCap = if (limit <= 0) Int.MAX_VALUE else limit
            var currentOffset = offset
            var hasMore = true

            while (hasMore && resultList.size < targetCap) {
                val fetchLimit = (targetCap - resultList.size).coerceAtMost(50)
                val url = "$baseUrl/rawaddr/$trimmed?limit=$fetchLimit&offset=$currentOffset"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Orbit-Forensics-Client/1.0")
                    .build()

                val count = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        if (resultList.isNotEmpty()) return@use 0
                        return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                    }
                    val body = response.body?.string() ?: return@use 0
                    val root = json.parseToJsonElement(body).jsonObject
                    val txArray = root["txs"]?.jsonArray ?: JsonArray(emptyList())

                    if (txArray.isEmpty()) return@use 0

                    for (elem in txArray) {
                        val txObj = elem.jsonObject
                        val hash = txObj["hash"]?.jsonPrimitive?.contentOrNull ?: continue
                        val fee = txObj["fee"]?.jsonPrimitive?.longOrNull ?: 0L
                        val time = txObj["time"]?.jsonPrimitive?.longOrNull ?: (System.currentTimeMillis() / 1000)
                        val blockHeight = txObj["block_height"]?.jsonPrimitive?.longOrNull
                        val size = txObj["size"]?.jsonPrimitive?.intOrNull ?: 0

                        val inputsArray = txObj["inputs"]?.jsonArray ?: JsonArray(emptyList())
                        val inputs = mutableListOf<TxInput>()
                        var targetInputSum = 0L
                        val senderAddresses = mutableListOf<String>()

                        for ((idx, inElem) in inputsArray.withIndex()) {
                            val inObj = inElem.jsonObject
                            val prevOut = inObj["prev_out"]?.jsonObject
                            val prevAddr = prevOut?.get("addr")?.jsonPrimitive?.contentOrNull
                            val prevVal = prevOut?.get("value")?.jsonPrimitive?.longOrNull ?: 0L

                            val isTarget = prevAddr?.lowercase() == lowerTarget
                            if (isTarget) targetInputSum += prevVal
                            if (!prevAddr.isNullOrBlank() && prevAddr.lowercase() != lowerTarget) {
                                senderAddresses.add(prevAddr)
                            }

                            inputs.add(
                                TxInput(
                                    txId = hash,
                                    vout = idx,
                                    prevOutAddress = prevAddr,
                                    prevOutValueSat = prevVal,
                                    isTargetAddress = isTarget
                                )
                            )
                        }

                        val outArray = txObj["out"]?.jsonArray ?: JsonArray(emptyList())
                        val outputs = mutableListOf<TxOutput>()
                        var targetOutputSum = 0L
                        val recipientAddresses = mutableListOf<String>()

                        for ((idx, outElem) in outArray.withIndex()) {
                            val outObj = outElem.jsonObject
                            val outAddr = outObj["addr"]?.jsonPrimitive?.contentOrNull
                            val outVal = outObj["value"]?.jsonPrimitive?.longOrNull ?: 0L
                            val script = outObj["script"]?.jsonPrimitive?.contentOrNull

                            val isTarget = outAddr?.lowercase() == lowerTarget
                            if (isTarget) targetOutputSum += outVal
                            if (!outAddr.isNullOrBlank() && outAddr.lowercase() != lowerTarget) {
                                recipientAddresses.add(outAddr)
                            }

                            outputs.add(
                                TxOutput(
                                    n = idx,
                                    scriptPubKey = script,
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
                                txId = hash,
                                network = BlockchainNetwork.BITCOIN,
                                timestamp = time,
                                blockHeight = blockHeight,
                                confirmations = if (blockHeight != null && blockHeight > 0) 6 else 0,
                                isConfirmed = blockHeight != null && blockHeight > 0,
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
                        if (resultList.size >= targetCap) break
                    }
                    txArray.size
                }

                currentOffset += count
                if (count < 50) {
                    hasMore = false
                }
            }

            Result.success(resultList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/latestblock")
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
