package com.aistudio.orbit.forensics.sanctions

import kotlinx.serialization.Serializable

@Serializable
enum class SanctionsDatasetTier(val displayNameEn: String, val displayNameFa: String, val maxSizeBytes: Long) {
    TIER_A_ANDROID("Tier A (Targeted Crypto/Cyber)", "سطح الف (رمزارز و جرایم سایبری هدفمند)", 50 * 1024 * 1024L), // 50MB
    TIER_B_LARGE_ANDROID_OPTIONAL("Tier B (Consolidated Law Enforcement)", "سطح ب (فهرست تجمیعی اختیاری مراجع قانونی)", 250 * 1024 * 1024L), // 250MB
    TIER_C_DESKTOP_SERVER("Tier C (Full Global OpenSanctions)", "سطح ج (پایگاه جامع دسکتاپ و سرور)", 5L * 1024 * 1024 * 1024L) // 5GB+
}

@Serializable
enum class SanctionsMatchingMethod {
    EXACT_CRYPTO_ADDRESS,
    NORMALIZED_EXACT_NAME,
    KNOWN_ALIAS_MATCH,
    IDENTIFIER_MATCH,
    FUZZY_NAME_SIMILARITY
}

@Serializable
data class SanctionMatchResult(
    val sanctionId: String,
    val primaryName: String,
    val matchedField: String,
    val matchedValue: String,
    val datasetSource: String, // e.g. "OFAC_SDN", "OPENSANCTIONS_TIER_A", "UN_CONSOLIDATED"
    val datasetVersion: String,
    val program: String,
    val matchingMethod: SanctionsMatchingMethod,
    val score: Float, // 0.0 to 1.0 (1.0 for exact crypto address)
    val effectiveFrom: Long?,
    val effectiveTo: Long?,
    val isHistorical: Boolean,
    val limitationsEn: String = "Fuzzy name matches require independent corroboration. Crypto address matches are definitive ledger observations.",
    val limitationsFa: String = "تطابق نام‌های فازی نیازمند بررسی ادله تکمیلی است. تطابق آدرس رمزارزی مشاهده قطعی دفترکل است."
)
