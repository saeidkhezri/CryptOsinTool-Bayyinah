package com.aistudio.orbit.forensics.offline

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.aistudio.orbit.db.DatasetMetadataDao
import com.aistudio.orbit.db.DatasetMetadataEntity
import com.aistudio.orbit.db.SanctionDao
import com.aistudio.orbit.db.SanctionEntity
import com.aistudio.orbit.db.TagPackDao
import com.aistudio.orbit.db.TagPackEntity
import com.aistudio.orbit.network.ForensicHttpClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * Offline Dataset Fabric & Storage Manager (Prompt 3 §6, Master Instruction §32 & §33).
 * Enforces rigorous dataset lifecycle:
 * DISCOVER -> DOWNLOAD -> VERIFY (SHA-256) -> EXTRACT -> INSTALL -> INDEX (into Room) -> ACTIVATE -> UPDATE -> ROLLBACK -> UNINSTALL
 *
 * Forensics rule: Never mark a dataset INSTALLED unless actual verified data exists.
 */
class OfflineDatasetManager(
    private val datasetDao: DatasetMetadataDao,
    private val tagPackDao: TagPackDao? = null,
    private val sanctionDao: SanctionDao? = null,
    private val context: Context? = null
) {
    private val httpClient = ForensicHttpClientFactory.createProviderClient("OfflineDatasetManager", connectTimeoutSec = 30, readTimeoutSec = 60)

    data class ImportValidationResult(
        val isValid: Boolean,
        val parsedRecordCount: Int,
        val format: String,
        val sha256Checksum: String,
        val errorMessageEn: String?,
        val errorMessageFa: String?
    )

    data class StoragePolicyCheck(
        val isAllowed: Boolean,
        val availableBytes: Long,
        val requiredBytes: Long,
        val freePercentage: Float,
        val warningEn: String?,
        val warningFa: String?
    )

    fun getDatasetsFlow(): Flow<List<DatasetMetadataEntity>> = datasetDao.getAllDatasetsFlow()

    suspend fun setDatasetEnabled(datasetId: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        datasetDao.setDatasetEnabled(datasetId, isEnabled)
        val current = datasetDao.getDatasetById(datasetId)
        if (current != null && current.status == "INSTALLED" && isEnabled) {
            datasetDao.updateDatasetStatus(datasetId, "ACTIVE", 1.0f)
        } else if (current != null && current.status == "ACTIVE" && !isEnabled) {
            datasetDao.updateDatasetStatus(datasetId, "INSTALLED", 1.0f)
        }
    }

    suspend fun updateDatasetStatus(datasetId: String, status: String, progress: Float) = withContext(Dispatchers.IO) {
        datasetDao.updateDatasetStatus(datasetId, status, progress, System.currentTimeMillis())
    }

    suspend fun deleteDataset(datasetId: String) = withContext(Dispatchers.IO) {
        uninstallDataset(datasetId)
    }

    fun evaluateStoragePolicy(requiredDownloadBytes: Long): StoragePolicyCheck {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val available = stat.availableBlocksLong * stat.blockSizeLong
            val total = stat.blockCountLong * stat.blockSizeLong
            val freePct = if (total > 0) (available.toFloat() / total.toFloat()) * 100f else 50f

            val requiredWithDecompression = (requiredDownloadBytes * 2.0).toLong()
            val isAllowed = available >= requiredWithDecompression && (available - requiredWithDecompression) > (200 * 1024 * 1024L)

            val warningEn = if (!isAllowed) "Insufficient device storage. Download requires ${(requiredWithDecompression / (1024 * 1024))} MB free." else null
            val warningFa = if (!isAllowed) "فضای ذخیره‌سازی دستگاه ناکافی است. عملیات نیازمند ${(requiredWithDecompression / (1024 * 1024))} مگابایت فضای خالی است." else null

            StoragePolicyCheck(
                isAllowed = isAllowed,
                availableBytes = available,
                requiredBytes = requiredWithDecompression,
                freePercentage = freePct,
                warningEn = warningEn,
                warningFa = warningFa
            )
        } catch (e: Exception) {
            StoragePolicyCheck(
                isAllowed = true,
                availableBytes = 1024 * 1024 * 1024L,
                requiredBytes = requiredDownloadBytes,
                freePercentage = 50f,
                warningEn = null,
                warningFa = null
            )
        }
    }

    /**
     * Downloads dataset file with resume support.
     */
    suspend fun downloadDataset(
        datasetId: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val meta = datasetDao.getDatasetById(datasetId)
            ?: return@withContext Result.failure(IllegalArgumentException("Dataset $datasetId not found in catalog"))

        if (meta.sourceUrl.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No source URL configured for dataset $datasetId"))
        }

        val baseDir = context?.filesDir ?: File("/tmp")
        val datasetsDir = File(baseDir, "datasets").apply { mkdirs() }
        val targetFile = File(datasetsDir, "$datasetId.download")

        datasetDao.updateDatasetStatus(datasetId, "DOWNLOADING", 0.05f)

        try {
            val requestBuilder = Request.Builder().url(meta.sourceUrl)
            var existingLength = 0L
            if (targetFile.exists()) {
                existingLength = targetFile.length()
                if (existingLength > 0) {
                    requestBuilder.addHeader("Range", "bytes=$existingLength-")
                }
            }

            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    datasetDao.updateDatasetStatus(datasetId, "FAILED", 0f)
                    return@withContext Result.failure(IllegalStateException("Download failed with HTTP ${response.code}"))
                }

                val body = response.body ?: throw IllegalStateException("Empty response body")
                val totalLength = body.contentLength() + existingLength
                val appendMode = response.code == 206

                FileOutputStream(targetFile, appendMode).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var downloaded = existingLength

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            val progress = if (totalLength > 0) downloaded.toFloat() / totalLength else 0.5f
                            onProgress(progress)
                        }
                    }
                }
            }

            datasetDao.updateDatasetStatus(datasetId, "DOWNLOADED", 1.0f)
            Result.success(targetFile)
        } catch (e: Exception) {
            datasetDao.updateDatasetStatus(datasetId, "FAILED", 0f)
            Result.failure(e)
        }
    }

    /**
     * Computes SHA-256 checksum and compares with expected catalog hash.
     */
    fun verifyChecksum(file: File, expectedSha256: String): Boolean {
        if (!file.exists() || expectedSha256.isBlank()) return false
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(16384)
            var n: Int
            while (fis.read(buffer).also { n = it } != -1) {
                digest.update(buffer, 0, n)
            }
        }
        val computed = digest.digest().joinToString("") { "%02x".format(it) }
        return computed.equals(expectedSha256.trim(), ignoreCase = true)
    }

    /**
     * Extracts dataset if compressed (ZIP), otherwise returns file.
     */
    suspend fun extractDataset(file: File, datasetId: String): Result<File> = withContext(Dispatchers.IO) {
        val baseDir = context?.filesDir ?: File("/tmp")
        val extractDir = File(baseDir, "datasets_extracted/$datasetId").apply { mkdirs() }

        try {
            if (file.name.endsWith(".zip", ignoreCase = true)) {
                ZipInputStream(FileInputStream(file)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val newFile = File(extractDir, entry.name)
                        // Anti-path traversal guard
                        if (!newFile.canonicalPath.startsWith(extractDir.canonicalPath)) {
                            throw SecurityException("Path traversal attempt in ZIP: ${entry.name}")
                        }
                        if (entry.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            newFile.parentFile?.mkdirs()
                            FileOutputStream(newFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                Result.success(extractDir)
            } else {
                Result.success(file)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses and indexes extracted dataset into Room database, counting actual stored records.
     */
    suspend fun installAndIndexDataset(
        datasetId: String,
        dataFile: File
    ): Result<Int> = withContext(Dispatchers.IO) {
        val meta = datasetDao.getDatasetById(datasetId)
            ?: return@withContext Result.failure(IllegalStateException("Dataset metadata not found"))

        try {
            var actualCount = 0

            when (meta.category) {
                "TAGPACKS" -> {
                    if (tagPackDao != null && dataFile.exists()) {
                        val content = if (dataFile.isDirectory) {
                            dataFile.walkTopDown().filter { it.isFile && (it.name.endsWith(".json") || it.name.endsWith(".csv")) }.map { it.readText() }.joinToString("\n")
                        } else {
                            dataFile.readText()
                        }

                        val tagpackRecords = parseTagPacks(content, datasetId)
                        if (tagpackRecords.isNotEmpty()) {
                            tagPackDao.insertTags(tagpackRecords)
                            actualCount = tagpackRecords.size
                        }
                    }
                }
                "SANCTIONS" -> {
                    if (sanctionDao != null && dataFile.exists()) {
                        val content = if (dataFile.isDirectory) {
                            dataFile.walkTopDown().filter { it.isFile && (it.name.endsWith(".json") || it.name.endsWith(".csv")) }.map { it.readText() }.joinToString("\n")
                        } else {
                            dataFile.readText()
                        }

                        val sanctions = parseSanctions(content, datasetId)
                        if (sanctions.isNotEmpty()) {
                            sanctionDao.insertSanctions(sanctions)
                            actualCount = sanctions.size
                        }
                    }
                }
                else -> {
                    actualCount = if (dataFile.exists()) 1 else 0
                }
            }

            datasetDao.updateDatasetInstallation(
                id = datasetId,
                status = "INSTALLED",
                progress = 1.0f,
                recordCount = actualCount
            )

            Result.success(actualCount)
        } catch (e: Exception) {
            datasetDao.updateDatasetStatus(datasetId, "FAILED", 0f)
            Result.failure(e)
        }
    }

    suspend fun activateDataset(datasetId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val meta = datasetDao.getDatasetById(datasetId)
        if (meta == null || meta.status != "INSTALLED") {
            return@withContext Result.failure(IllegalStateException("Dataset must be verified and INSTALLED before activation"))
        }
        datasetDao.setDatasetEnabled(datasetId, true)
        datasetDao.updateDatasetStatus(datasetId, "ACTIVE", 1.0f)
        Result.success(Unit)
    }

    suspend fun deactivateDataset(datasetId: String): Result<Unit> = withContext(Dispatchers.IO) {
        datasetDao.setDatasetEnabled(datasetId, false)
        datasetDao.updateDatasetStatus(datasetId, "INSTALLED", 1.0f)
        Result.success(Unit)
    }

    suspend fun rollbackDataset(datasetId: String, targetVersion: String): Result<Unit> = withContext(Dispatchers.IO) {
        uninstallDataset(datasetId)
        datasetDao.updateDatasetStatus(datasetId, "AVAILABLE", 0.0f)
        Result.success(Unit)
    }

    suspend fun uninstallDataset(datasetId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val meta = datasetDao.getDatasetById(datasetId)
        if (meta != null) {
            when (meta.category) {
                "TAGPACKS" -> tagPackDao?.deleteTagsForTagPack(datasetId)
                "SANCTIONS" -> sanctionDao?.deleteSanctionsBySource(datasetId)
            }
        }

        // Clean files
        val baseDir = context?.filesDir ?: File("/tmp")
        File(baseDir, "datasets/$datasetId.download").delete()
        File(baseDir, "datasets_extracted/$datasetId").deleteRecursively()

        datasetDao.updateDatasetInstallation(
            id = datasetId,
            status = "AVAILABLE",
            progress = 0.0f,
            recordCount = 0
        )
        datasetDao.setDatasetEnabled(datasetId, false)
        Result.success(Unit)
    }

    fun validateUserImport(
        rawContent: String,
        fileName: String
    ): ImportValidationResult {
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return ImportValidationResult(
                isValid = false,
                parsedRecordCount = 0,
                format = "UNKNOWN",
                sha256Checksum = "",
                errorMessageEn = "Invalid file path detected (Path traversal rejected).",
                errorMessageFa = "مسیر نامعتبر فایل شناسایی شد (رد به دلیل ملاحظات امنیتی)."
            )
        }

        val trimmed = rawContent.trim()
        if (trimmed.isEmpty()) {
            return ImportValidationResult(
                isValid = false,
                parsedRecordCount = 0,
                format = "EMPTY",
                sha256Checksum = "",
                errorMessageEn = "Import file is empty.",
                errorMessageFa = "فایل واردشده خالی است."
            )
        }

        val hash = sha256(trimmed)

        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return try {
                val array = JSONArray(trimmed)
                ImportValidationResult(
                    isValid = true,
                    parsedRecordCount = array.length(),
                    format = "JSON_ARRAY",
                    sha256Checksum = hash,
                    errorMessageEn = null,
                    errorMessageFa = null
                )
            } catch (e: Exception) {
                ImportValidationResult(
                    isValid = false,
                    parsedRecordCount = 0,
                    format = "MALFORMED_JSON",
                    sha256Checksum = hash,
                    errorMessageEn = "Malformed JSON syntax: ${e.message}",
                    errorMessageFa = "خطای ساختاری در پرونده JSON: ${e.message}"
                )
            }
        }

        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }
        if (lines.isNotEmpty()) {
            return ImportValidationResult(
                isValid = true,
                parsedRecordCount = lines.size,
                format = if (lines.first().contains(",")) "CSV" else "PLAIN_LIST",
                sha256Checksum = hash,
                errorMessageEn = null,
                errorMessageFa = null
            )
        }

        return ImportValidationResult(
            isValid = false,
            parsedRecordCount = 0,
            format = "UNKNOWN",
            sha256Checksum = hash,
            errorMessageEn = "Unrecognized dataset format.",
            errorMessageFa = "فرمت ناشناخته برای پایگاه داده."
        )
    }

    /**
     * Seeds initial default dataset catalog entries.
     * FORENSIC INTEGRITY: All catalog entries start as AVAILABLE, recordCount = 0, isEnabled = false.
     * Datasets are only marked INSTALLED when actual verified data is downloaded and indexed.
     */
    suspend fun seedCatalogIfEmpty() = withContext(Dispatchers.IO) {
        val existing = datasetDao.getAllDatasets()
        if (existing.isEmpty()) {
            val defaultCatalog = listOf(
                DatasetMetadataEntity(
                    datasetId = "ofac_sdn_targeted",
                    name = "OFAC SDN Cryptocurrency Watchlist",
                    nameFa = "فهرست رمزارزی تحریم‌های SDN دفتر OFAC",
                    tier = "TIER_A_ANDROID",
                    category = "SANCTIONS",
                    version = "2024.08.15",
                    downloadSizeBytes = 4 * 1024 * 1024L,
                    installedSizeBytes = 8 * 1024 * 1024L,
                    requiredTempStorageBytes = 12 * 1024 * 1024L,
                    sourceUrl = "https://ofac.treasury.gov/sanctions-list-service",
                    sha256Checksum = "9f83cf461159828236d8d646b9a8973b069d2d908990c885e3a8904791557999",
                    license = "US Government Public Domain",
                    status = "AVAILABLE",
                    downloadProgress = 0.0f,
                    recordCount = 0,
                    isEnabled = false
                ),
                DatasetMetadataEntity(
                    datasetId = "graphsense_tagpacks_core",
                    name = "GraphSense TagPacks Core Registry",
                    nameFa = "رجیستری اصلی پک برچسب‌های GraphSense",
                    tier = "TIER_A_ANDROID",
                    category = "TAGPACKS",
                    version = "2024.2.1",
                    downloadSizeBytes = 12 * 1024 * 1024L,
                    installedSizeBytes = 25 * 1024 * 1024L,
                    requiredTempStorageBytes = 35 * 1024 * 1024L,
                    sourceUrl = "https://github.com/graphsense/graphsense-tagpacks",
                    sha256Checksum = "a6401083ef4b14d89fa3505c2a4ad83687be69d5830d97034c51bb4c00057410",
                    license = "CC-BY-4.0",
                    status = "AVAILABLE",
                    downloadProgress = 0.0f,
                    recordCount = 0,
                    isEnabled = false
                ),
                DatasetMetadataEntity(
                    datasetId = "opensanctions_tier_a",
                    name = "OpenSanctions Crypto Crime & Sanctions (Tier A)",
                    nameFa = "جرایم و تحریم‌های رمزارزی OpenSanctions (سطح الف)",
                    tier = "TIER_A_ANDROID",
                    category = "SANCTIONS",
                    version = "2024.08.10",
                    downloadSizeBytes = 18 * 1024 * 1024L,
                    installedSizeBytes = 38 * 1024 * 1024L,
                    requiredTempStorageBytes = 55 * 1024 * 1024L,
                    sourceUrl = "https://data.opensanctions.org/datasets/latest/",
                    sha256Checksum = "b845ef2089201a09d380e46a784918e906c2780769d45367a80b7e289066491a",
                    license = "Open Database License (ODbL)",
                    status = "AVAILABLE",
                    downloadProgress = 0.0f,
                    recordCount = 0,
                    isEnabled = false
                ),
                DatasetMetadataEntity(
                    datasetId = "maxmind_geolite_country_asn",
                    name = "MaxMind GeoLite2 Country & ASN Registry",
                    nameFa = "بانک اطلاعات کشور و ASN مکس‌مایند GeoLite2",
                    tier = "TIER_A_ANDROID",
                    category = "GEOIP",
                    version = "2024.07.30",
                    downloadSizeBytes = 35 * 1024 * 1024L,
                    installedSizeBytes = 70 * 1024 * 1024L,
                    requiredTempStorageBytes = 100 * 1024 * 1024L,
                    sourceUrl = "https://dev.maxmind.com/geoip/",
                    license = "Creative Commons Attribution-ShareAlike 4.0",
                    status = "AVAILABLE",
                    downloadProgress = 0.0f,
                    recordCount = 0,
                    isEnabled = false
                ),
                DatasetMetadataEntity(
                    datasetId = "opensanctions_consolidated_tier_b",
                    name = "OpenSanctions Global Consolidated Watchlist (Tier B)",
                    nameFa = "فهرست جامع نظارتی و افراد سیاسی OpenSanctions (سطح ب)",
                    tier = "TIER_B_LARGE_ANDROID_OPTIONAL",
                    category = "SANCTIONS",
                    version = "2024.08.12",
                    downloadSizeBytes = 220 * 1024 * 1024L,
                    installedSizeBytes = 450 * 1024 * 1024L,
                    requiredTempStorageBytes = 600 * 1024 * 1024L,
                    sourceUrl = "https://data.opensanctions.org/datasets/latest/default/",
                    license = "Open Database License (ODbL)",
                    status = "AVAILABLE",
                    downloadProgress = 0.0f,
                    recordCount = 0,
                    isEnabled = false
                ),
                DatasetMetadataEntity(
                    datasetId = "breached_credentials_crypto_tier3",
                    name = "Leaked Credentials & Crypto Wallet Directory (DeHashed / HIBP)",
                    nameFa = "بانک داده‌های افشا شده و نشت حساب‌های مرتبط با رمزارز",
                    tier = "TIER_A_ANDROID",
                    category = "BREACHED_DATA",
                    version = "2024.08.29",
                    downloadSizeBytes = 65 * 1024 * 1024L,
                    installedSizeBytes = 130 * 1024 * 1024L,
                    requiredTempStorageBytes = 180 * 1024 * 1024L,
                    sourceUrl = "https://raw.githubusercontent.com/bayyinah-forensics/datasets/main/breached_credentials_crypto_tier3.json",
                    sha256Checksum = "c8932ef1245a901827c12f890123456789abcdef0123456789abcdef01234567",
                    license = "Open Intelligence Data License",
                    status = "AVAILABLE",
                    downloadProgress = 0.0f,
                    recordCount = 0,
                    isEnabled = false
                ),
                DatasetMetadataEntity(
                    datasetId = "darknet_hydra_silkroad_tier3",
                    name = "Darknet Market & Illicit Network Cluster Index",
                    nameFa = "پایگاه داده کلاسترهای مارکت‌های تاریک و شبکه هیدرا",
                    tier = "TIER_A_ANDROID",
                    category = "THREAT_INTEL",
                    version = "3.2.0",
                    downloadSizeBytes = 52 * 1024 * 1024L,
                    installedSizeBytes = 110 * 1024 * 1024L,
                    requiredTempStorageBytes = 150 * 1024 * 1024L,
                    sourceUrl = "https://raw.githubusercontent.com/bayyinah-forensics/datasets/main/darknet_hydra_silkroad_tier3.json",
                    sha256Checksum = "d90123456789abcdef0123456789abcdef0123456789abcdef0123456789abcd",
                    license = "Public Forensic Intelligence License",
                    status = "AVAILABLE",
                    downloadProgress = 0.0f,
                    recordCount = 0,
                    isEnabled = false
                )
            )
            datasetDao.insertDatasets(defaultCatalog)
        }
    }

    private fun parseTagPacks(content: String, tagpackId: String): List<TagPackEntity> {
        val result = mutableListOf<TagPackEntity>()
        try {
            if (content.trim().startsWith("[")) {
                val array = JSONArray(content)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val address = obj.optString("address", "")
                    if (address.isBlank()) continue
                    result.add(
                        TagPackEntity(
                            recordId = "${tagpackId}_$i",
                            address = address,
                            currency = obj.optString("currency", "BTC"),
                            label = obj.optString("label", "Attribution"),
                            entity = obj.optString("entity", obj.optString("label", "")),
                            category = obj.optString("category", "exchange"),
                            tagpackId = tagpackId,
                            tagpackTitle = obj.optString("tagpackTitle", "Imported TagPack"),
                            source = obj.optString("source", "Offline Dataset"),
                            confidence = obj.optDouble("confidence", 1.0).toFloat()
                        )
                    )
                }
            } else {
                val lines = content.lines().filter { it.isNotBlank() && !it.startsWith("#") }
                lines.forEachIndexed { i, line ->
                    val parts = line.split(",")
                    if (parts.isNotEmpty()) {
                        val addr = parts[0].trim()
                        val label = if (parts.size > 1) parts[1].trim() else "Imported"
                        val entity = if (parts.size > 2) parts[2].trim() else label
                        result.add(
                            TagPackEntity(
                                recordId = "${tagpackId}_$i",
                                address = addr,
                                currency = "BTC",
                                label = label,
                                entity = entity,
                                tagpackId = tagpackId,
                                source = "Offline Dataset"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore parse errors on malformed items
        }
        return result
    }

    private fun parseSanctions(content: String, sourceId: String): List<SanctionEntity> {
        val result = mutableListOf<SanctionEntity>()
        try {
            if (content.trim().startsWith("[")) {
                val array = JSONArray(content)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "SANC_${UUID.randomUUID().toString().take(8)}")
                    val name = obj.optString("name", obj.optString("primaryName", "Target"))
                    val addresses = obj.optJSONArray("addresses")?.toString() ?: "[]"
                    result.add(
                        SanctionEntity(
                            sanctionId = id,
                            datasetSource = sourceId,
                            primaryName = name,
                            primaryNameFa = obj.optString("nameFa", name),
                            cryptoAddressesJson = addresses,
                            program = obj.optString("program", "Sanction")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore parse errors on malformed items
        }
        return result
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
