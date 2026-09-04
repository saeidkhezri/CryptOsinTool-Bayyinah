package com.aistudio.orbit.model

import kotlinx.serialization.Serializable

@Serializable
enum class CrimeCategory(val displayNameEn: String, val displayNameFa: String) {
    FINANCIAL_CRIME("Financial Crime", "جرایم مالی"),
    MONEY_LAUNDERING("Money Laundering & Layering", "پولشویی و لایه‌بندی مالی"),
    FRAUD("Fraud", "کلاهبرداری"),
    SMUGGLING_FINANCING("Smuggling-related Financial Activity", "فعالیت‌های مالی مرتبط با قاچاق"),
    CYBERCRIME_FINANCING("Cybercrime Financing", "تامین مالی جرایم سایبری"),
    RANSOMWARE("Ransomware", "باج‌افزار"),
    ESPIONAGE_FINANCING("Espionage-related Financial Activity", "فعالیت‌های مالی مرتبط با جاسوسی"),
    SABOTAGE_FINANCING("Sabotage-related Financial Activity", "فعالیت‌های مالی مرتبط با خرابکاری"),
    TERRORIST_FINANCING("Terrorist/Extremist Financing", "تامین مالی تروریسم/افراط‌گرایی"),
    SANCTIONS_EVASION("Sanctions Evasion", "دور زدن تحریم‌ها"),
    ASSET_CONCEALMENT("Asset Concealment", "پنهان‌سازی دارایی"),
    MIXER_OBFUSCATION("Mixer & Tumbler Obfuscation", "ناشناس‌سازی و استفاده از میکسر"),
    STRUCTURING_SMURFING("Structuring & Smurfing", "خردسازی تراکنش‌ها (Smurfing)"),
    HIGH_VELOCITY_TRANSIT("High Velocity Transit", "انتقال سریع با ماندگاری صفر"),
    BEHAVIORAL_ANOMALY("Behavioral Anomaly", "ناهنجاری رفتاری"),
    EXTERNAL_EXPOSURE("External Exposure", "مواجهه با منابع خارجی"),
    EXTORTION_COERCION("Extortion & Coercion", "اخاذی و اجبار")
}

@Serializable
data class CrimePattern(
    val id: String,
    val code: String,
    val nameEn: String,
    val nameFa: String,
    val category: CrimeCategory,
    val descriptionEn: String,
    val descriptionFa: String,
    val behavioralIndicatorsEn: List<String>,
    val behavioralIndicatorsFa: List<String>,
    val detectionRules: String,
    val scoringModel: String,
    val evidenceRequirements: List<String> = emptyList(),
    val supportingSignals: List<String> = emptyList(),
    val contradictorySignals: List<String> = emptyList(),
    val confidenceInterpretationEn: String,
    val confidenceInterpretationFa: String,
    val references: List<String>,
    val limitationsEn: String = "Analytical indicator only. Does not prove criminal intent.",
    val limitationsFa: String = "صرفاً شاخص تحلیلی است. نیت مجرمانه را ثابت نمی‌کند.",
    val version: String = "2.0.0"
)

@Serializable
data class PatternMatchResult(
    val patternId: String,
    val patternCode: String,
    val patternNameEn: String,
    val patternNameFa: String,
    val category: CrimeCategory,
    val similarityScore: Double,          // 0.0 to 100.0
    val confidence: ConfidenceLevel,
    val matchedIndicators: List<String> = emptyList(),
    val matchedIndicatorsEn: List<String> = matchedIndicators,
    val matchedIndicatorsFa: List<String> = matchedIndicators,
    val conflictingIndicators: List<String> = emptyList(),
    val conflictingIndicatorsEn: List<String> = conflictingIndicators,
    val conflictingIndicatorsFa: List<String> = conflictingIndicators,
    val missingEvidence: List<String> = emptyList(),
    val missingEvidenceEn: List<String> = missingEvidence,
    val missingEvidenceFa: List<String> = missingEvidence,
    val relatedTxHashes: List<String> = emptyList(),
    val relatedAddresses: List<String> = emptyList(),
    val evidenceIds: List<String> = emptyList(),
    val thresholdsApplied: String = "",
    val analyticalRecommendationEn: String,
    val analyticalRecommendationFa: String,
    val forensicDisclaimerEn: String = "Pattern similarity is an analytical lead and does NOT establish criminal conduct.",
    val forensicDisclaimerFa: String = "هشدار جرم‌یابی: تطابق الگویی صرفاً سرنخ تحلیلی بوده و به منزله اثبات ارتکاب جرم نیست.",
    val limitationsEn: String = "Analytical indicator only. Does not prove criminal intent.",
    val limitationsFa: String = "صرفاً شاخص تحلیلی است. نیت مجرمانه را ثابت نمی‌کند."
) {
    fun localizedPatternName(isPersian: Boolean): String = if (isPersian) patternNameFa else patternNameEn
    fun localizedMatchedIndicators(isPersian: Boolean): List<String> = if (isPersian) matchedIndicatorsFa else matchedIndicatorsEn
    fun localizedConflictingIndicators(isPersian: Boolean): List<String> = if (isPersian) conflictingIndicatorsFa else conflictingIndicatorsEn
    fun localizedMissingEvidence(isPersian: Boolean): List<String> = if (isPersian) missingEvidenceFa else missingEvidenceEn
    fun localizedRecommendation(isPersian: Boolean): String = if (isPersian) analyticalRecommendationFa else analyticalRecommendationEn
    fun localizedDisclaimer(isPersian: Boolean): String = if (isPersian) forensicDisclaimerFa else forensicDisclaimerEn
    fun localizedLimitations(isPersian: Boolean): String = if (isPersian) limitationsFa else limitationsEn
}

@Serializable
data class ReferenceCase(
    val caseId: String,
    val nameEn: String,
    val nameFa: String,
    val year: Int,
    val blockchain: BlockchainNetwork,
    val primaryAddresses: List<String>,
    val patternCode: String,
    val summaryEn: String,
    val summaryFa: String,
    val keyCharacteristicsEn: List<String>,
    val keyCharacteristicsFa: List<String>,
    val publicReferenceUrl: String
)
