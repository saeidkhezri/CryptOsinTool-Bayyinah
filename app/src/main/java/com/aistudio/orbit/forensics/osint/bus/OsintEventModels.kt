package com.aistudio.orbit.forensics.osint.bus

import com.aistudio.orbit.db.EpistemicStatus
import kotlinx.serialization.Serializable
import java.security.MessageDigest

/**
 * Supported indicator types in Bayyinah Forensic OSINT Event Bus (Master Instruction §2).
 */
@Serializable
enum class IndicatorType(val displayNameEn: String, val displayNameFa: String) {
    ADDRESS("Cryptocurrency Address", "آدرس رمزارز"),
    TXID("Transaction Hash (TXID)", "هش تراکنش"),
    DOMAIN("Domain / FQDN", "دامنه اینترنتی"),
    URL("Web URL / Endpoint", "نشانی وب"),
    EMAIL("Email Address", "پست الکترونیک"),
    USERNAME("Online Handle / Username", "نام کاربری"),
    PHONE("Phone Number (E.164)", "شماره تلفن"),
    IP("IP Address (IPv4/IPv6)", "آدرس آی‌پی"),
    ASN("Autonomous System Number (ASN)", "شماره سامانه خودمختار (ASN)"),
    CERTIFICATE("TLS / SSL Certificate", "گواهی امنیتی TLS/SSL"),
    ENTITY("Forensic Entity", "موجودیت تحلیلی"),
    ORGANIZATION("Organization / Corporate", "سازمان یا شرکت"),
    SERVICE("Online / Web3 Service", "سرویس یا زیرساخت"),
    DOCUMENT("Document / File Artifact", "سند یا مدرک"),
    SOCIAL_PROFILE("Public Social Profile", "پروفایل شبکه اجتماعی")
}

/**
 * Result states for any OSINT observation (Master Instruction §28).
 */
@Serializable
enum class OsintResultState(val displayNameEn: String, val displayNameFa: String) {
    DISCOVERED("Discovered", "کشف‌شده"),
    UNVERIFIED("Unverified", "تاییدنشده"),
    CORROBORATED("Corroborated", "تایید هم‌راستا"),
    CONTRADICTED("Contradicted", "ردشده / متعارض"),
    INCONCLUSIVE("Inconclusive", "غیرقطعی"),
    EXPIRED("Expired", "منقضی‌شده"),
    UNAVAILABLE("Unavailable", "غیرقابل دسترس")
}

/**
 * Source independence tracking (Master Instruction §16, §27).
 */
@Serializable
enum class SourceLineageType {
    ORIGINAL_SOURCE,
    DERIVED_SOURCE,
    MIRROR_SOURCE,
    INDEPENDENT
}

/**
 * Normalized OSINT Event payload published on the forensic bus (Master Instruction §2).
 */
@Serializable
data class OsintEvent(
    val eventId: String = "EVT_${System.currentTimeMillis()}_${(1000..9999).random()}",
    val caseId: String,
    val investigationId: String = "",
    val source: String,
    val indicatorType: IndicatorType,
    val indicatorValue: String,
    val normalizedValue: String = normalizeIndicator(indicatorType, indicatorValue),
    val createdAt: Long = System.currentTimeMillis(),
    val observedAt: Long = System.currentTimeMillis(),
    val confidence: Float = 0.70f, // 0.0 to 1.0
    val provenance: String = "",
    val resultState: OsintResultState = OsintResultState.DISCOVERED,
    val epistemicStatus: EpistemicStatus = EpistemicStatus.INFERENCE,
    val sourceLineage: SourceLineageType = SourceLineageType.INDEPENDENT,
    val payloadJson: String = "{}",
    val tags: List<String> = emptyList()
) {
    companion object {
        fun normalizeIndicator(type: IndicatorType, value: String): String {
            val trimmed = value.trim()
            return when (type) {
                IndicatorType.ADDRESS -> trimmed // Preserve case for Bitcoin Base58, lower for ETH
                IndicatorType.TXID -> trimmed.lowercase()
                IndicatorType.DOMAIN -> trimmed.lowercase().removePrefix("http://").removePrefix("https://").substringBefore("/").substringBefore(":")
                IndicatorType.URL -> trimmed
                IndicatorType.EMAIL -> trimmed.lowercase()
                IndicatorType.USERNAME -> trimmed.lowercase().removePrefix("@")
                IndicatorType.PHONE -> trimmed.replace("[^0-9+]".toRegex(), "")
                IndicatorType.IP -> trimmed.substringBefore(":") // strip port if present
                IndicatorType.ASN -> if (trimmed.uppercase().startsWith("AS")) trimmed.uppercase() else "AS$trimmed"
                IndicatorType.CERTIFICATE -> trimmed.lowercase()
                IndicatorType.ENTITY, IndicatorType.ORGANIZATION, IndicatorType.SERVICE, IndicatorType.DOCUMENT, IndicatorType.SOCIAL_PROFILE -> trimmed
            }
        }
    }
}
