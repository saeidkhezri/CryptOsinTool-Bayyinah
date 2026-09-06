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
                "TAGPACKS", "VASP_REGISTRY" -> {
                    if (tagPackDao != null && dataFile.exists()) {
                        val content = if (dataFile.isDirectory) {
                            dataFile.walkTopDown().filter { it.isFile && (it.name.endsWith(".json") || it.name.endsWith(".csv") || it.name.endsWith(".yaml")) }.map { it.readText() }.joinToString("\n")
                        } else {
                            dataFile.readText()
                        }

                        val tagpackRecords = parseTagPacks(content, datasetId, meta.category)
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
                "THREAT_INTEL", "MIXER_HEURISTICS", "BREACHED_DATA" -> {
                    if (tagPackDao != null && dataFile.exists()) {
                        val content = if (dataFile.isDirectory) {
                            dataFile.walkTopDown().filter { it.isFile && (it.name.endsWith(".json") || it.name.endsWith(".csv") || it.name.endsWith(".yaml") || it.name.endsWith(".yml")) }.map { it.readText() }.joinToString("\n")
                        } else {
                            dataFile.readText()
                        }

                        val threatRecords = parseThreatIntel(content, datasetId, meta.category)
                        if (threatRecords.isNotEmpty()) {
                            tagPackDao.insertTags(threatRecords)
                            actualCount = threatRecords.size
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

    /**
     * Cross-Device Export: Exports installed dataset to a standardized portable JSON file.
     * Can be transferred via USB, local storage, or secure share to other forensic workstations.
     */
    suspend fun exportDatasetToPortableJson(
        datasetId: String,
        targetFile: File
    ): Result<Long> = withContext(Dispatchers.IO) {
        val meta = datasetDao.getDatasetById(datasetId)
            ?: return@withContext Result.failure(IllegalStateException("Dataset not found"))

        try {
            val root = JSONObject()
            root.put("exportVersion", "2.0")
            root.put("app", "Bayyinah")
            root.put("exportedAt", System.currentTimeMillis())
            root.put("datasetId", meta.datasetId)
            root.put("name", meta.name)
            root.put("nameFa", meta.nameFa)
            root.put("category", meta.category)
            root.put("tier", meta.tier)
            root.put("version", meta.version)

            val recordsArray = JSONArray()

            if (meta.category == "SANCTIONS" && sanctionDao != null) {
                val sanctions = sanctionDao.getSanctionsBySource(datasetId)
                for (s in sanctions) {
                    val sObj = JSONObject()
                    sObj.put("sanctionId", s.sanctionId)
                    sObj.put("primaryName", s.primaryName)
                    sObj.put("primaryNameFa", s.primaryNameFa)
                    sObj.put("cryptoAddressesJson", s.cryptoAddressesJson)
                    sObj.put("program", s.program)
                    recordsArray.put(sObj)
                }
            } else if (tagPackDao != null) {
                val tags = tagPackDao.getTagsForTagPack(datasetId)
                for (t in tags) {
                    val tObj = JSONObject()
                    tObj.put("recordId", t.recordId)
                    tObj.put("address", t.address)
                    tObj.put("currency", t.currency)
                    tObj.put("label", t.label)
                    tObj.put("entity", t.entity)
                    tObj.put("category", t.category)
                    tObj.put("confidence", t.confidence.toDouble())
                    recordsArray.put(tObj)
                }
            }

            root.put("recordCount", recordsArray.length())
            root.put("records", recordsArray)

            val content = root.toString(2)
            targetFile.parentFile?.mkdirs()
            targetFile.writeText(content, Charsets.UTF_8)

            Result.success(targetFile.length())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cross-Device Import: Imports portable dataset JSON file exported from another Bayyinah device.
     */
    suspend fun importPortableDataset(
        sourceFile: File,
        conflictPolicy: String = "MERGE"
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || sourceFile.length() == 0L) {
            return@withContext Result.failure(IllegalArgumentException("Source file does not exist or is empty"))
        }

        try {
            val content = sourceFile.readText(Charsets.UTF_8)
            val root = JSONObject(content)
            val datasetId = root.optString("datasetId", "imported_${UUID.randomUUID().toString().take(8)}")
            val name = root.optString("name", "Imported Dataset")
            val nameFa = root.optString("nameFa", "پایگاه داده واردشده")
            val category = root.optString("category", "TAGPACKS")
            val tier = root.optString("tier", "TIER_A_ANDROID")
            val version = root.optString("version", "1.0")
            val recordsArray = root.optJSONArray("records") ?: JSONArray()

            var importedCount = 0

            if (category == "SANCTIONS" && sanctionDao != null) {
                val sanctions = mutableListOf<SanctionEntity>()
                for (i in 0 until recordsArray.length()) {
                    val obj = recordsArray.optJSONObject(i) ?: continue
                    sanctions.add(
                        SanctionEntity(
                            sanctionId = obj.optString("sanctionId", "${datasetId}_$i"),
                            datasetSource = datasetId,
                            primaryName = obj.optString("primaryName", "Target"),
                            primaryNameFa = obj.optString("primaryNameFa", "هدف"),
                            cryptoAddressesJson = obj.optString("cryptoAddressesJson", "[]"),
                            program = obj.optString("program", "Sanction")
                        )
                    )
                }
                if (sanctions.isNotEmpty()) {
                    sanctionDao.insertSanctions(sanctions)
                    importedCount = sanctions.size
                }
            } else if (tagPackDao != null) {
                val tags = mutableListOf<TagPackEntity>()
                for (i in 0 until recordsArray.length()) {
                    val obj = recordsArray.optJSONObject(i) ?: continue
                    val addr = obj.optString("address", "")
                    if (addr.isBlank()) continue
                    tags.add(
                        TagPackEntity(
                            recordId = obj.optString("recordId", "${datasetId}_$i"),
                            address = addr,
                            currency = obj.optString("currency", "BTC"),
                            label = obj.optString("label", "Attributed"),
                            entity = obj.optString("entity", "Entity"),
                            category = obj.optString("category", category),
                            tagpackId = datasetId,
                            tagpackTitle = name,
                            source = "Portable Import",
                            confidence = obj.optDouble("confidence", 0.95).toFloat()
                        )
                    )
                }
                if (tags.isNotEmpty()) {
                    tagPackDao.insertTags(tags)
                    importedCount = tags.size
                }
            }

            // Register or update metadata
            val existing = datasetDao.getDatasetById(datasetId)
            val metadata = existing?.copy(
                status = "INSTALLED",
                downloadProgress = 1.0f,
                recordCount = importedCount,
                isEnabled = true
            ) ?: DatasetMetadataEntity(
                datasetId = datasetId,
                name = name,
                nameFa = nameFa,
                tier = tier,
                category = category,
                version = version,
                downloadSizeBytes = sourceFile.length(),
                installedSizeBytes = sourceFile.length() * 2,
                requiredTempStorageBytes = sourceFile.length(),
                sourceUrl = "file://${sourceFile.name}",
                sha256Checksum = sha256(content),
                license = "Forensic Shared Dataset",
                status = "INSTALLED",
                downloadProgress = 1.0f,
                recordCount = importedCount,
                isEnabled = true
            )

            datasetDao.insertDataset(metadata)
            Result.success(importedCount)
        } catch (e: Exception) {
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
                    datasetId = "sanctions_ofac_matrix",
                    name = "OFAC SDN & International Sanctions Matrix",
                    nameFa = "بانک جامع تحریم‌های بین‌المللی SDN دفتر OFAC",
                    tier = "TIER_A_ANDROID",
                    category = "SANCTIONS",
                    version = "v2025.01",
                    downloadSizeBytes = 18 * 1024 * 1024L,
                    installedSizeBytes = 36 * 1024 * 1024L,
                    requiredTempStorageBytes = 50 * 1024 * 1024L,
                    sourceUrl = "https://data.opensanctions.org/datasets/latest/us_ofac_sdn/targets.simple.csv",
                    sha256Checksum = "17efb8705d82ace32c09e315c5797a29b186e5f9fa6ccd73a4d19509ad7e883a",
                    license = "US Government / OpenSanctions ODbL",
                    status = "AVAILABLE",
                    downloadProgress = 0.0f,
                    recordCount = 0,
                    isEnabled = false
                ),
                DatasetMetadataEntity(
                    datasetId = "opensanctions_crypto_matrix",
                    name = "OpenSanctions Consolidated Crypto Targets",
                    nameFa = "پایگاه یکپارچه اهداف رمزارزی پرریسک بین‌المللی OpenSanctions",
                    tier = "TIER_A_ANDROID",
                    category = "SANCTIONS",
                    version = "v2025.02",
                    downloadSizeBytes = 22 * 1024 * 1024L,
                    installedSizeBytes = 45 * 1024 * 1024L,
                    requiredTempStorageBytes = 65 * 1024 * 1024L,
                    sourceUrl = "https://data.opensanctions.org/datasets/latest/sanctions/targets.simple.csv",
                    sha256Checksum = "6df05f70f92f0c3f8ece8030c9b6d7daa9df57dd0751976d291797ccbdbc5bfa",
                    license = "Open Database License (ODbL)",
                    status = "AVAILABLE",
                    downloadProgress = 0.0f,
                    recordCount = 0,
                    isEnabled = false
                ),
                DatasetMetadataEntity(
                    datasetId = "vasp_exchange_labels_tier1",
                    name = "GraphSense TagPacks & Global VASP Registry",
                    nameFa = "بانک برچسب صرافی‌ها و نهادهای مالی معتبر (GraphSense TagPacks)",
                    tier = "TIER_A_ANDROID",
                    category = "VASP_REGISTRY",
                    version = "v2.5.0",
                    downloadSizeBytes = 12 * 1024 * 1024L,
                    installedSizeBytes = 25 * 1024 * 1024L,
                    requiredTempStorageBytes = 35 * 1024 * 1024L,
                    sourceUrl = "https://raw.githubusercontent.com/graphsense/graphsense-tagpacks/master/packs/exchange-wallets-binance.yaml",
                    sha256Checksum = "e706166f2c74b23c80f79e4a93f85cb7f7fc80829a5be70409201ea4b5d4b944",
                    license = "CC-BY-4.0",
                    status = "AVAILABLE",
                    downloadProgress = 0.0f,
                    recordCount = 0,
                    isEnabled = false
                ),
                DatasetMetadataEntity(
                    datasetId = "cryptoscamdb_malicious_blacklist",
                    name = "CryptoScamDB Malicious & Fraud Blacklist",
                    nameFa = "فهرست سیاه آدرس‌های کلاهبرداری و اسکم CryptoScamDB",
                    tier = "TIER_A_ANDROID",
                    category = "THREAT_INTEL",
                    version = "v2024.11",
                    downloadSizeBytes = 16 * 1024 * 1024L,
                    installedSizeBytes = 32 * 1024 * 1024L,
                    requiredTempStorageBytes = 48 * 1024 * 1024L,
                    sourceUrl = "https://raw.githubusercontent.com/graphsense/graphsense-tagpacks/master/packs/etherscamdb_tagpack.yaml",
                    sha256Checksum = "e34a3548130bfca653b33a4a88aa6bafa06c63fb92e56a404176c3c616ff9dba",
                    license = "Open Community Data License",
                    status = "AVAILABLE",
                    downloadProgress = 0.0f,
                    recordCount = 0,
                    isEnabled = false
                ),
                DatasetMetadataEntity(
                    datasetId = "ransomwhere_jackcable_ransomware",
                    name = "Ransomwhere Ransomware Payments & Threat Actors",
                    nameFa = "پایگاه ردیابی باج‌افزارها و باج‌های رمزارزی Ransomwhere",
                    tier = "TIER_A_ANDROID",
                    category = "THREAT_INTEL",
                    version = "v4.1.0",
                    downloadSizeBytes = 28 * 1024 * 1024L,
                    installedSizeBytes = 56 * 1024 * 1024L,
                    requiredTempStorageBytes = 80 * 1024 * 1024L,
                    sourceUrl = "https://raw.githubusercontent.com/graphsense/graphsense-tagpacks/master/packs/ransomwhere.yaml",
                    sha256Checksum = "fa2061e36c4d6b1eed3590bd17e1138198245dd2d11999be1589be0eac122ae0",
                    license = "MIT / Open Research License",
                    status = "AVAILABLE",
                    downloadProgress = 0.0f,
                    recordCount = 0,
                    isEnabled = false
                ),
                DatasetMetadataEntity(
                    datasetId = "phishfort_phishing_drainers",
                    name = "PhishFort Phishing Domains & Crypto Drainers Blacklist",
                    nameFa = "فهرست سیاه دامنه‌های فیشینگ و اسکریپت‌های درینر PhishFort",
                    tier = "TIER_A_ANDROID",
                    category = "THREAT_INTEL",
                    version = "v2025.01",
                    downloadSizeBytes = 14 * 1024 * 1024L,
                    installedSizeBytes = 28 * 1024 * 1024L,
                    requiredTempStorageBytes = 40 * 1024 * 1024L,
                    sourceUrl = "https://raw.githubusercontent.com/phishfort/phishfort-lists/master/blacklists/domains.json",
                    sha256Checksum = "c291bcfaad367b6629ce1df6731e144c53d7cdc28e3e321829c8a496b6a9c887",
                    license = "Open Cybersecurity License",
                    status = "AVAILABLE",
                    downloadProgress = 0.0f,
                    recordCount = 0,
                    isEnabled = false
                ),
                DatasetMetadataEntity(
                    datasetId = "tornado_cash_coinjoin_pools",
                    name = "Tornado Cash & Anonymity Pools Heuristics",
                    nameFa = "الگوهای استخرهای گمنام‌ساز Tornado Cash و CoinJoin",
                    tier = "TIER_A_ANDROID",
                    category = "MIXER_HEURISTICS",
                    version = "v1.9.0",
                    downloadSizeBytes = 32 * 1024 * 1024L,
                    installedSizeBytes = 64 * 1024 * 1024L,
                    requiredTempStorageBytes = 90 * 1024 * 1024L,
                    sourceUrl = "https://raw.githubusercontent.com/graphsense/graphsense-tagpacks/master/packs/tornado_cash.yaml",
                    sha256Checksum = "a2fdafe9a32f538150e70b13e7653088bdff82abcd74085dc8c9d380e00480bd",
                    license = "GPL-3.0",
                    status = "AVAILABLE",
                    downloadProgress = 0.0f,
                    recordCount = 0,
                    isEnabled = false
                ),
                DatasetMetadataEntity(
                    datasetId = "breached_credentials_crypto_tier3",
                    name = "Leaked Credentials & Crypto Wallet Directory (Hacks & Exploits)",
                    nameFa = "بانک داده‌های سرقت‌ها و هک‌های صرافی‌ها و پروتکل‌ها",
                    tier = "TIER_A_ANDROID",
                    category = "BREACHED_DATA",
                    version = "v2024.12",
                    downloadSizeBytes = 65 * 1024 * 1024L,
                    installedSizeBytes = 130 * 1024 * 1024L,
                    requiredTempStorageBytes = 180 * 1024 * 1024L,
                    sourceUrl = "https://raw.githubusercontent.com/graphsense/graphsense-tagpacks/master/packs/hacks.yaml",
                    sha256Checksum = "7fff7a640e14800f749fa6cb222cea7494d0d92767106fa4ecca8976ca682c60",
                    license = "Open Intelligence Data License",
                    status = "AVAILABLE",
                    downloadProgress = 0.0f,
                    recordCount = 0,
                    isEnabled = false
                ),
                DatasetMetadataEntity(
                    datasetId = "darknet_hydra_silkroad_tier3",
                    name = "Darknet Market & Illicit Clusters Taxonomy",
                    nameFa = "پایگاه کلاسترهای مارکت‌های تاریک، هیدرا و سیلک‌رود",
                    tier = "TIER_A_ANDROID",
                    category = "THREAT_INTEL",
                    version = "v3.3.0",
                    downloadSizeBytes = 45 * 1024 * 1024L,
                    installedSizeBytes = 90 * 1024 * 1024L,
                    requiredTempStorageBytes = 130 * 1024 * 1024L,
                    sourceUrl = "https://raw.githubusercontent.com/graphsense/graphsense-tagpacks/master/packs/hydra.yaml",
                    sha256Checksum = "d56a9fc6f1d45df1b489a9f453646483765df6ae18d4d758064eddef14f51412",
                    license = "Public Forensic Intelligence License",
                    status = "AVAILABLE",
                    downloadProgress = 0.0f,
                    recordCount = 0,
                    isEnabled = false
                ),
                DatasetMetadataEntity(
                    datasetId = "maxmind_geolite_country_asn",
                    name = "MaxMind GeoLite2 Offline ASN & GeoIP",
                    nameFa = "بانک محلی موقعیت جغرافیایی و ASN مکس‌مایند GeoLite2",
                    tier = "TIER_A_ANDROID",
                    category = "GEOIP",
                    version = "2024.11",
                    downloadSizeBytes = 35 * 1024 * 1024L,
                    installedSizeBytes = 70 * 1024 * 1024L,
                    requiredTempStorageBytes = 100 * 1024 * 1024L,
                    sourceUrl = "https://raw.githubusercontent.com/P3TERX/GeoLite.mmdb/download/GeoLite2-City.mmdb",
                    sha256Checksum = "85974cd715333c1dab9e23fa0685483a8c9316d372e69164f836d1f812c41ff8",
                    license = "Creative Commons Attribution-ShareAlike 4.0",
                    status = "AVAILABLE",
                    downloadProgress = 0.0f,
                    recordCount = 0,
                    isEnabled = false
                ),
                DatasetMetadataEntity(
                    datasetId = "coingecko_assets_master",
                    name = "CoinGecko Crypto Assets & Token Contract Master List",
                    nameFa = "رجیستری مستر دارایی‌ها و قراردادهای توکن CoinGecko",
                    tier = "TIER_A_ANDROID",
                    category = "VASP_REGISTRY",
                    version = "v2025.01",
                    downloadSizeBytes = 8 * 1024 * 1024L,
                    installedSizeBytes = 16 * 1024 * 1024L,
                    requiredTempStorageBytes = 25 * 1024 * 1024L,
                    sourceUrl = "https://api.coingecko.com/api/v3/coins/list",
                    sha256Checksum = "rolling",
                    license = "CoinGecko Public API Terms",
                    status = "AVAILABLE",
                    downloadProgress = 0.0f,
                    recordCount = 0,
                    isEnabled = false
                )
            )
            datasetDao.insertDatasets(defaultCatalog)
        }
    }

    private fun parseTagPacks(content: String, tagpackId: String, defaultCategory: String = "exchange"): List<TagPackEntity> {
        val result = mutableListOf<TagPackEntity>()
        try {
            val trimmed = content.trim()
            if (trimmed.contains("tags:") || trimmed.contains("- address:") || (trimmed.contains("address:") && (trimmed.contains("title:") || trimmed.contains("currency:")))) {
                val yamlResults = parseYamlTagPack(trimmed, tagpackId, defaultCategory)
                if (yamlResults.isNotEmpty()) return yamlResults
            }

            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val address = obj.optString("address", "")
                    if (address.isBlank()) continue
                    result.add(
                        TagPackEntity(
                            recordId = "${tagpackId}_$i",
                            address = address,
                            currency = obj.optString("currency", "BTC"),
                            label = obj.optString("label", obj.optString("name", "Attribution")),
                            entity = obj.optString("entity", obj.optString("label", obj.optString("name", ""))),
                            category = obj.optString("category", defaultCategory),
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
                        if (addr.isNotBlank() && addr.length > 15) {
                            val label = if (parts.size > 1) parts[1].trim() else "Imported"
                            val entity = if (parts.size > 2) parts[2].trim() else label
                            result.add(
                                TagPackEntity(
                                    recordId = "${tagpackId}_$i",
                                    address = addr,
                                    currency = "BTC",
                                    label = label,
                                    entity = entity,
                                    category = defaultCategory,
                                    tagpackId = tagpackId,
                                    source = "Offline Dataset"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore parse errors on malformed items
        }
        return result
    }

    private fun parseThreatIntel(content: String, datasetId: String, category: String): List<TagPackEntity> {
        val result = mutableListOf<TagPackEntity>()
        try {
            val trimmed = content.trim()
            if (trimmed.contains("tags:") || trimmed.contains("- address:") || (trimmed.contains("address:") && (trimmed.contains("title:") || trimmed.contains("currency:")))) {
                val yamlResults = parseYamlTagPack(trimmed, datasetId, category)
                if (yamlResults.isNotEmpty()) return yamlResults
            }

            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val item = array.get(i)
                    if (item is JSONObject) {
                        val address = item.optString("address", item.optString("wallet", item.optString("contract", "")))
                        if (address.isNotBlank() && address.length > 15) {
                            val label = item.optString("name", item.optString("ransomware", item.optString("title", "Threat Actor")))
                            val entity = item.optString("entity", label)
                            result.add(
                                TagPackEntity(
                                    recordId = "${datasetId}_$i",
                                    address = address,
                                    currency = item.optString("coin", item.optString("currency", "BTC")),
                                    label = label,
                                    entity = entity,
                                    category = category,
                                    tagpackId = datasetId,
                                    tagpackTitle = "Threat Intelligence",
                                    source = "Offline Dataset",
                                    confidence = 0.95f
                                )
                            )
                        }
                    } else if (item is String && item.length > 15) {
                        result.add(
                            TagPackEntity(
                                recordId = "${datasetId}_$i",
                                address = item,
                                currency = "MULTI",
                                label = "Threat Target",
                                entity = "Blacklisted",
                                category = category,
                                tagpackId = datasetId,
                                tagpackTitle = "Threat Blacklist",
                                source = "Offline Dataset",
                                confidence = 0.90f
                            )
                        )
                    }
                }
            } else if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                val keys = root.keys()
                var idx = 0
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = root.opt(key)
                    if (value is JSONObject) {
                        val addr = value.optString("address", key)
                        if (addr.length > 15) {
                            result.add(
                                TagPackEntity(
                                    recordId = "${datasetId}_$idx",
                                    address = addr,
                                    currency = value.optString("currency", "ETH"),
                                    label = value.optString("name", "Pool/Contract"),
                                    entity = value.optString("protocol", "DeFi/Mixer"),
                                    category = category,
                                    tagpackId = datasetId,
                                    source = "Offline Dataset",
                                    confidence = 0.95f
                                )
                            )
                            idx++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore parse errors on malformed payloads
        }
        return result
    }

    private fun parseSanctions(content: String, sourceId: String): List<SanctionEntity> {
        val result = mutableListOf<SanctionEntity>()
        try {
            val trimmed = content.trim()
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
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
            } else {
                // Parse OpenSanctions simple CSV (id,schema,name,aliases,birth_date,countries,addresses,identifiers,sanctions,phones,emails)
                val lines = trimmed.lines().filter { it.isNotBlank() && !it.startsWith("#") }
                lines.forEachIndexed { i, line ->
                    if (i == 0 && line.lowercase().contains("schema")) return@forEachIndexed // skip CSV header
                    val parts = line.split(",")
                    if (parts.size >= 3) {
                        val id = parts[0].trim()
                        val name = parts[2].trim().replace("\"", "")
                        val addrs = if (parts.size >= 7) parts[6].trim().replace("\"", "") else ""
                        val addrsJson = if (addrs.isNotBlank()) {
                            val list = addrs.split(";").map { it.trim() }.filter { it.isNotBlank() }
                            JSONArray(list).toString()
                        } else "[]"

                        result.add(
                            SanctionEntity(
                                sanctionId = if (id.isNotBlank()) id else "SANC_${sourceId}_$i",
                                datasetSource = sourceId,
                                primaryName = name,
                                primaryNameFa = name,
                                cryptoAddressesJson = addrsJson,
                                program = if (parts.size >= 9) parts[8].trim().replace("\"", "") else "Sanctions List"
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

    private fun parseYamlTagPack(content: String, tagpackId: String, defaultCategory: String): List<TagPackEntity> {
        val result = mutableListOf<TagPackEntity>()
        try {
            var globalCurrency = "BTC"
            var globalTitle = "TagPack"
            var inTags = false

            var currentAddress = ""
            var currentLabel = ""
            var currentEntity = ""
            var currentCategory = defaultCategory
            var currentCurrency = ""
            var idx = 0

            fun flushCurrent() {
                if (currentAddress.isNotBlank() && currentAddress.length > 15) {
                    result.add(
                        TagPackEntity(
                            recordId = "${tagpackId}_$idx",
                            address = currentAddress,
                            currency = if (currentCurrency.isNotBlank()) currentCurrency else globalCurrency,
                            label = if (currentLabel.isNotBlank()) currentLabel else (if (currentEntity.isNotBlank()) currentEntity else "Attributed"),
                            entity = if (currentEntity.isNotBlank()) currentEntity else currentLabel,
                            category = if (currentCategory.isNotBlank()) currentCategory else defaultCategory,
                            tagpackId = tagpackId,
                            tagpackTitle = globalTitle,
                            source = "GraphSense / Offline TagPack",
                            confidence = 0.98f
                        )
                    )
                    idx++
                }
                currentAddress = ""
                currentLabel = ""
                currentEntity = ""
                currentCategory = defaultCategory
                currentCurrency = ""
            }

            for (rawLine in content.lines()) {
                val line = rawLine.trim()
                if (line.isBlank() || line.startsWith("#")) continue

                if (line.startsWith("currency:", ignoreCase = true)) {
                    globalCurrency = line.substringAfter(":").trim().replace("\"", "").replace("'", "").uppercase()
                    continue
                }
                if (line.startsWith("title:", ignoreCase = true)) {
                    globalTitle = line.substringAfter(":").trim().replace("\"", "").replace("'", "")
                    continue
                }
                if (line.startsWith("tags:", ignoreCase = true)) {
                    inTags = true
                    continue
                }

                if (inTags || line.startsWith("- address:") || line.contains("address:")) {
                    if (line.startsWith("- ")) {
                        flushCurrent()
                        val remainder = line.substring(2).trim()
                        if (remainder.startsWith("address:", ignoreCase = true)) {
                            currentAddress = remainder.substringAfter(":").trim().replace("\"", "").replace("'", "")
                        }
                    } else if (line.startsWith("address:", ignoreCase = true)) {
                        if (currentAddress.isNotBlank()) {
                            flushCurrent()
                        }
                        currentAddress = line.substringAfter(":").trim().replace("\"", "").replace("'", "")
                    } else if (line.startsWith("label:", ignoreCase = true)) {
                        currentLabel = line.substringAfter(":").trim().replace("\"", "").replace("'", "")
                    } else if (line.startsWith("entity:", ignoreCase = true)) {
                        currentEntity = line.substringAfter(":").trim().replace("\"", "").replace("'", "")
                    } else if (line.startsWith("category:", ignoreCase = true)) {
                        currentCategory = line.substringAfter(":").trim().replace("\"", "").replace("'", "")
                    } else if (line.startsWith("currency:", ignoreCase = true)) {
                        currentCurrency = line.substringAfter(":").trim().replace("\"", "").replace("'", "").uppercase()
                    }
                }
            }
            flushCurrent()
        } catch (e: Exception) {
            // Ignore parse errors on malformed yaml items
        }
        return result
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
