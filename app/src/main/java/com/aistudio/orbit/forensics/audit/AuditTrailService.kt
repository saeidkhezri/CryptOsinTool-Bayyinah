package com.aistudio.orbit.forensics.audit

import com.aistudio.orbit.security.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

@Serializable
enum class AuditOperationType(val titleFa: String, val titleEn: String) {
    ADDRESS_LOOKUP("استعلام و واکشی آدرس", "Address Ledger Discovery"),
    FILTER_APPLIED("اعمال فیلتر جرم‌یابی", "Forensic Filter Applied"),
    REPORT_EXPORT("صدور گزارش رسمی", "Forensic Report Export"),
    CLASSIFICATION("طبقه‌بندی و برچسب‌گذاری", "Entity Classification"),
    CRIME_PATTERN_MATCH("تطبیق الگوی مشکوک", "Crime Pattern Detection"),
    GRAPH_MODIFIED("تغییرات گراف ارتباطی", "Investigation Graph Modified"),
    PROVIDER_CONFIG("پیکربندی ارائه‌دهنده", "API Provider Configuration"),
    PERMISSION_CHANGE("تغییر سطح دسترسی", "Permission State Modified"),
    INVESTIGATION_CREATED("ایجاد پرونده جدید", "New Investigation Case")
}

@Serializable
enum class AuditResultState(val titleFa: String, val titleEn: String) {
    SUCCESS("موفق", "Success"),
    FAILED("ناموفق / خطا", "Failed"),
    CACHED("واکشی از حافظه محلی", "Cached / Offline"),
    CANCELED("لغو توسط کارشناس", "Canceled by Investigator")
}

@Serializable
data class ForensicAuditRecord(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val user: String,
    val operation: AuditOperationType,
    val target: String,
    val resultState: AuditResultState,
    val details: String,
    val metricsCount: Int = 0,
    val reproducibilityHash: String = ""
) {
    val formattedDateUtc: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(Date(timestamp))
        }
}

object AuditTrailService {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val _records = MutableStateFlow<List<ForensicAuditRecord>>(
        listOf(
            ForensicAuditRecord(
                id = "audit_init",
                timestamp = System.currentTimeMillis(),
                user = "Administrator",
                operation = AuditOperationType.INVESTIGATION_CREATED,
                target = "ORBIT_CHAIN_FORENSICS",
                resultState = AuditResultState.SUCCESS,
                details = "Forensic audit engine initialized with cryptographic reproducibility hash tracking.",
                reproducibilityHash = calculateHash("ORBIT_CHAIN_FORENSICS_INIT")
            )
        )
    )
    val records: StateFlow<List<ForensicAuditRecord>> = _records.asStateFlow()

    fun recordAddressLookup(
        user: String,
        address: String,
        network: String,
        state: AuditResultState,
        txCount: Int,
        notes: String = ""
    ) {
        val details = "Blockchain: $network, Transactions discovered: $txCount. Notes: $notes"
        val record = createRecord(
            user = user,
            operation = AuditOperationType.ADDRESS_LOOKUP,
            target = "$network:$address",
            resultState = state,
            details = details,
            metricsCount = txCount
        )
        appendRecord(record)
        AuthManager.logAudit("ADDRESS_LOOKUP", address, details)
    }

    fun recordFilterApplied(
        user: String,
        caseId: String,
        filterSummary: String,
        matchCount: Int
    ) {
        val details = "Filters applied: [$filterSummary]. Retained items: $matchCount"
        val record = createRecord(
            user = user,
            operation = AuditOperationType.FILTER_APPLIED,
            target = caseId,
            resultState = AuditResultState.SUCCESS,
            details = details,
            metricsCount = matchCount
        )
        appendRecord(record)
        AuthManager.logAudit("FILTER_APPLIED", caseId, details)
    }

    fun recordReportExport(
        user: String,
        caseId: String,
        format: String,
        destination: String
    ) {
        val details = "Report exported in format: $format to $destination"
        val record = createRecord(
            user = user,
            operation = AuditOperationType.REPORT_EXPORT,
            target = caseId,
            resultState = AuditResultState.SUCCESS,
            details = details
        )
        appendRecord(record)
        AuthManager.logAudit("REPORT_EXPORT", caseId, details)
    }

    fun recordClassification(
        user: String,
        address: String,
        classification: String,
        confidencePct: Int
    ) {
        val details = "Classified as: $classification (Confidence: $confidencePct%)"
        val record = createRecord(
            user = user,
            operation = AuditOperationType.CLASSIFICATION,
            target = address,
            resultState = AuditResultState.SUCCESS,
            details = details,
            metricsCount = confidencePct
        )
        appendRecord(record)
        AuthManager.logAudit("CLASSIFICATION", address, details)
    }

    fun recordPatternMatched(
        user: String,
        address: String,
        patternId: String,
        confidencePct: Int
    ) {
        val details = "Matched pattern: $patternId with confidence: $confidencePct%"
        val record = createRecord(
            user = user,
            operation = AuditOperationType.CRIME_PATTERN_MATCH,
            target = address,
            resultState = AuditResultState.SUCCESS,
            details = details,
            metricsCount = confidencePct
        )
        appendRecord(record)
        AuthManager.logAudit("CRIME_PATTERN_MATCH", address, details)
    }

    fun recordProviderConfig(
        user: String,
        providerId: String,
        status: String
    ) {
        val details = "API provider $providerId configuration updated. State: $status"
        val record = createRecord(
            user = user,
            operation = AuditOperationType.PROVIDER_CONFIG,
            target = providerId,
            resultState = AuditResultState.SUCCESS,
            details = details
        )
        appendRecord(record)
        AuthManager.logAudit("PROVIDER_CONFIG", providerId, details)
    }

    fun recordGraphModified(
        user: String,
        caseId: String,
        details: String
    ) {
        val record = createRecord(
            user = user,
            operation = AuditOperationType.GRAPH_MODIFIED,
            target = caseId,
            resultState = AuditResultState.SUCCESS,
            details = details
        )
        appendRecord(record)
        AuthManager.logAudit("GRAPH_MODIFIED", caseId, details)
    }

    private fun createRecord(
        user: String,
        operation: AuditOperationType,
        target: String,
        resultState: AuditResultState,
        details: String,
        metricsCount: Int = 0
    ): ForensicAuditRecord {
        val id = "audit_${System.currentTimeMillis()}_${(100..999).random()}"
        val hash = calculateHash("$id:$user:${operation.name}:$target:${resultState.name}:$details")
        return ForensicAuditRecord(
            id = id,
            timestamp = System.currentTimeMillis(),
            user = user,
            operation = operation,
            target = target,
            resultState = resultState,
            details = details,
            metricsCount = metricsCount,
            reproducibilityHash = hash
        )
    }

    private fun appendRecord(record: ForensicAuditRecord) {
        _records.value = listOf(record) + _records.value.take(500)
    }

    private fun calculateHash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }

    fun exportAsJson(): String {
        return json.encodeToString(_records.value)
    }

    fun exportAsCsv(): String {
        val sb = StringBuilder()
        sb.append("ID,Timestamp_UTC,Investigator,Operation,Target,State,Metrics,Details,Hash\n")
        _records.value.forEach { r ->
            sb.append("\"${r.id}\",")
            sb.append("\"${r.formattedDateUtc}\",")
            sb.append("\"${r.user}\",")
            sb.append("\"${r.operation.titleEn}\",")
            sb.append("\"${r.target}\",")
            sb.append("\"${r.resultState.name}\",")
            sb.append("${r.metricsCount},")
            sb.append("\"${r.details.replace("\"", "'")}\",")
            sb.append("\"${r.reproducibilityHash}\"\n")
        }
        return sb.toString()
    }
}
