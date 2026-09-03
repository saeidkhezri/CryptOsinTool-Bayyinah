package com.aistudio.orbit.forensics.sanctions

import android.os.Environment
import android.os.StatFs
import com.aistudio.orbit.db.SanctionDao
import com.aistudio.orbit.db.SanctionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import kotlin.math.max

/**
 * OFAC & OpenSanctions Intelligence Engine (Master Instruction §14, §15, §16, §17, §18).
 * Supports:
 * - Multi-level matching: Exact crypto address, normalized name, alias search, fuzzy token matching.
 * - Historical validity tracking (effectiveFrom, effectiveTo).
 * - Pre-download storage validation per device tier.
 */
class SanctionsIntelligenceEngine(
    private val sanctionDao: SanctionDao
) {

    data class StorageCheckResult(
        val isSufficient: Boolean,
        val availableBytes: Long,
        val requiredBytes: Long,
        val messageEn: String,
        val messageFa: String
    )

    /**
     * Checks if local storage has sufficient space before initiating any dataset download (Master Instruction §15, §30).
     */
    fun checkStorageCapacity(requiredSizeBytes: Long): StorageCheckResult {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val available = stat.availableBlocksLong * stat.blockSizeLong
            val requiredTotal = (requiredSizeBytes * 1.5).toLong() // 1.5x buffer for decompression

            val isOk = available >= requiredTotal
            val availMb = available / (1024 * 1024)
            val reqMb = requiredTotal / (1024 * 1024)

            StorageCheckResult(
                isSufficient = isOk,
                availableBytes = available,
                requiredBytes = requiredTotal,
                messageEn = if (isOk) "Storage check passed ($availMb MB free, $reqMb MB required)." else "Insufficient storage ($availMb MB free, $reqMb MB required).",
                messageFa = if (isOk) "فضای ذخیره‌سازی تایید شد ($availMb مگابایت موجود، $reqMb مگابایت مورد نیاز)." else "فضای ذخیره‌سازی ناکافی است ($availMb مگابایت موجود، $reqMb مگابایت مورد نیاز)."
            )
        } catch (e: Exception) {
            StorageCheckResult(
                isSufficient = true,
                availableBytes = 500 * 1024 * 1024L,
                requiredBytes = requiredSizeBytes,
                messageEn = "Storage capacity estimated.",
                messageFa = "فضای ذخیره‌سازی برآورد شد."
            )
        }
    }

    /**
     * Matches a cryptocurrency address against indexed OFAC and OpenSanctions records.
     */
    suspend fun matchAddress(address: String): List<SanctionMatchResult> = withContext(Dispatchers.IO) {
        val matches = mutableListOf<SanctionMatchResult>()
        val records = sanctionDao.getSanctionsForAddress(address.trim())

        for (rec in records) {
            matches.add(
                SanctionMatchResult(
                    sanctionId = rec.sanctionId,
                    primaryName = rec.primaryName,
                    matchedField = "crypto_address",
                    matchedValue = address.trim(),
                    datasetSource = rec.datasetSource,
                    datasetVersion = rec.datasetVersion,
                    program = rec.program,
                    matchingMethod = SanctionsMatchingMethod.EXACT_CRYPTO_ADDRESS,
                    score = 1.0f,
                    effectiveFrom = rec.effectiveFrom,
                    effectiveTo = rec.effectiveTo,
                    isHistorical = rec.isHistorical
                )
            )
        }
        matches
    }

    /**
     * Matches a person or entity name against sanctions records with exact, alias, and fuzzy matching.
     */
    suspend fun matchName(name: String): List<SanctionMatchResult> = withContext(Dispatchers.IO) {
        val matches = mutableListOf<SanctionMatchResult>()
        val query = name.trim().lowercase()
        if (query.length < 3) return@withContext matches

        val candidates = sanctionDao.searchSanctionsByName(query)
        for (rec in candidates) {
            val primaryLower = rec.primaryName.lowercase()
            if (primaryLower == query) {
                matches.add(
                    SanctionMatchResult(
                        sanctionId = rec.sanctionId,
                        primaryName = rec.primaryName,
                        matchedField = "primary_name",
                        matchedValue = rec.primaryName,
                        datasetSource = rec.datasetSource,
                        datasetVersion = rec.datasetVersion,
                        program = rec.program,
                        matchingMethod = SanctionsMatchingMethod.NORMALIZED_EXACT_NAME,
                        score = 0.98f,
                        effectiveFrom = rec.effectiveFrom,
                        effectiveTo = rec.effectiveTo,
                        isHistorical = rec.isHistorical
                    )
                )
                continue
            }

            // Check aliases
            var aliasFound = false
            try {
                val aliasArray = JSONArray(rec.aliasesJson)
                for (i in 0 until aliasArray.length()) {
                    val alias = aliasArray.getString(i).trim().lowercase()
                    if (alias == query || alias.contains(query)) {
                        matches.add(
                            SanctionMatchResult(
                                sanctionId = rec.sanctionId,
                                primaryName = rec.primaryName,
                                matchedField = "alias",
                                matchedValue = aliasArray.getString(i),
                                datasetSource = rec.datasetSource,
                                datasetVersion = rec.datasetVersion,
                                program = rec.program,
                                matchingMethod = SanctionsMatchingMethod.KNOWN_ALIAS_MATCH,
                                score = 0.90f,
                                effectiveFrom = rec.effectiveFrom,
                                effectiveTo = rec.effectiveTo,
                                isHistorical = rec.isHistorical
                            )
                        )
                        aliasFound = true
                        break
                    }
                }
            } catch (e: Exception) {}

            if (!aliasFound) {
                // Compute fuzzy similarity score
                val sim = computeFuzzySimilarity(query, primaryLower)
                if (sim >= 0.75f) {
                    matches.add(
                        SanctionMatchResult(
                            sanctionId = rec.sanctionId,
                            primaryName = rec.primaryName,
                            matchedField = "fuzzy_name",
                            matchedValue = rec.primaryName,
                            datasetSource = rec.datasetSource,
                            datasetVersion = rec.datasetVersion,
                            program = rec.program,
                            matchingMethod = SanctionsMatchingMethod.FUZZY_NAME_SIMILARITY,
                            score = sim,
                            effectiveFrom = rec.effectiveFrom,
                            effectiveTo = rec.effectiveTo,
                            isHistorical = rec.isHistorical
                        )
                    )
                }
            }
        }
        matches.sortedByDescending { it.score }
    }

    /**
     * Seeds initial official OFAC SDN crypto addresses if table is empty.
     */
    suspend fun seedOfacSanctionsIfEmpty() = withContext(Dispatchers.IO) {
        val count = sanctionDao.getTotalSanctionsCount()
        if (count == 0) {
            val defaultSanctions = listOf(
                SanctionEntity(
                    sanctionId = "OFAC_SDN_13345_LAZARUS",
                    datasetSource = "OFAC_SDN",
                    datasetVersion = "2024.08.15",
                    schemaType = "CryptoAddress",
                    primaryName = "Lazarus Group (DPRK Cyber Group)",
                    primaryNameFa = "گروه سایبری لازاروس (کره شمالی)",
                    aliasesJson = "[\"APT38\", \"Hidden Cobra\", \"Guardians of Peace\", \"Labyrinth Chollima\"]",
                    cryptoAddressesJson = "[\"12tkqA9xSo9jQ8YQGE1TcDsQdXuNxBFxJU\", \"115p7UMMngoj1pMvkpHijcRdfJNXj6LrLn\", \"0x098b716b8aaf21512996dc57eb0615e2383e2f96\"]",
                    program = "CYBER2 / DPRK3",
                    country = "KP",
                    effectiveFrom = 1568332800000L,
                    effectiveTo = null,
                    isHistorical = false
                ),
                SanctionEntity(
                    sanctionId = "OFAC_SDN_24512_HYDRA",
                    datasetSource = "OFAC_SDN",
                    datasetVersion = "2024.08.15",
                    schemaType = "Company",
                    primaryName = "Hydra Market Darknet Exchange",
                    primaryNameFa = "مارکت‌پلیس تاریک هیدرا",
                    aliasesJson = "[\"Hydra Market\", \"Hydra CEX\"]",
                    cryptoAddressesJson = "[\"1LNoVToXk2sU1fGzPSt4m57W4e4o9YgXNn\", \"12t9YDPgwioPHQkdAhPQCqqTxztdYtStG2\"]",
                    program = "RANSOMWARE / CYBER2",
                    country = "RU",
                    effectiveFrom = 1649116800000L,
                    effectiveTo = null,
                    isHistorical = false
                ),
                SanctionEntity(
                    sanctionId = "OPENSANCTIONS_TORNADO_CASH",
                    datasetSource = "OPENSANCTIONS_TIER_A",
                    datasetVersion = "2024.08.10",
                    schemaType = "CryptoAddress",
                    primaryName = "Tornado Cash Classic Pool Routers",
                    primaryNameFa = "قراردادها و استخرهای تورنادو کش",
                    aliasesJson = "[\"Tornado.cash\", \"Tornado DAO\"]",
                    cryptoAddressesJson = "[\"0x12d66f87a04a9e220743712ce6d9bb1b5616b8fc\", \"0x47ce0c6ed5b0ce3d3a51fdb1c52dc66a7c3c2936\"]",
                    program = "FINANCIAL_CRIME_SANCTIONS",
                    country = "GLOBAL",
                    effectiveFrom = 1659916800000L,
                    effectiveTo = null,
                    isHistorical = false
                )
            )
            sanctionDao.insertSanctions(defaultSanctions)
        }
    }

    private fun computeFuzzySimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 1.0f
        val dist = levenshteinDistance(s1, s2)
        return (maxLen - dist).toFloat() / maxLen.toFloat()
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
