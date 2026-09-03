package com.aistudio.orbit.model

import kotlinx.serialization.Serializable

/**
 * Standard Forensic Evidence Classification
 * Strictly separates directly observed facts from external data, algorithms, and analytical inferences.
 */
@Serializable
enum class EvidenceCategory(val displayNameEn: String, val displayNameFa: String) {
    OBSERVED_ON_CHAIN(
        "Directly Observed On-Chain Fact",
        "حقیقت قطعی مشاهده‌شده در داده بلاکچین (درون بلاکچین)"
    ),
    EXTERNAL_SOURCE(
        "External OSINT / Explorer Data",
        "داده‌های دریافتی از منابع خارجی و کاوشگرها"
    ),
    ALGORITHMIC_RESULT(
        "Algorithmic Calculation",
        "محاسبه‌شده با قواعد الگوریتمی"
    ),
    ATTRIBUTION(
        "Entity Attribution & Tag",
        "انتساب و برچسب‌گذاری موجودیت"
    ),
    OSINT_INTELLIGENCE(
        "Open-Source Intelligence (OSINT)",
        "داده‌های اطلاعاتی و OSINT"
    ),
    SANCTIONS_MATCH(
        "Sanctions Screening Match",
        "تطبیق با فهرست‌های تحریمی و شاخص‌های ریسک"
    ),
    BEHAVIORAL_PATTERN(
        "Suspicious Behavioral Pattern",
        "تطبیق الگو و رفتار مشکوک مالی"
    ),
    TEMPORAL_ANALYSIS(
        "Temporal / Diurnal Analysis",
        "تحلیل زمانی و سازگاری زمانی-جغرافیایی"
    ),
    AI_INFERENCE(
        "AI-Assisted Interpretation (Probabilistic)",
        "استنباط دستیار هوش مصنوعی (احتمالاتی)"
    ),
    INVESTIGATOR_CONCLUSION(
        "Investigator Finding / Assessment",
        "ارزیابی و جمع‌بندی کارشناس پرونده"
    )
}

@Serializable
enum class ConfidenceLevel(val displayNameEn: String, val displayNameFa: String, val weight: Float) {
    DEFINITIVE_FACT("100% Deterministic Fact", "قطعیت ۱۰۰٪ (حقیقت اثبات‌شده)", 1.0f),
    HIGH_CONFIDENCE("High Confidence (>= 85%)", "احتمال بالا (تایید چندمنبعی ≥ ۸۵٪)", 0.85f),
    MEDIUM_CONFIDENCE("Medium Confidence (50-84%)", "احتمال متوسط (قانون تحلیلی ۵۰-۸۴٪)", 0.65f),
    LOW_CONFIDENCE("Low Confidence (< 50%)", "احتمال پایین (سرنخ اولیه < ۵۰٪)", 0.35f),
    HEURISTIC_HYPOTHESIS("Probabilistic Hypothesis", "فرضیه تحلیلی مشروط", 0.50f),
    UNCERTAIN("Unverified / Conflicting", "تاییدنشده یا دارای تعارض", 0.10f)
}

@Serializable
enum class DataSourceType(val displayNameEn: String, val displayNameFa: String) {
    ON_CHAIN_RPC("Direct Blockchain Full Node / RPC", "نود کامل بلاکچین / فراخوانی مستقیم RPC"),
    PUBLIC_EXPLORER_API("Public Blockchain Explorer API", "رابط کاربری کاوشگرهای عمومی"),
    OSINT_DATABASE("OSINT Intelligence & Attribution Registry", "پایگاه داده OSINT و برچسب‌های عمومی"),
    CLUSTERING_ALGORITHM("CIOH Multi-Input Clustering Engine", "موتور خوشه آدرس‌ها با ورودی مشترک (CIOH)"),
    HEURISTIC_ENGINE("Forensic Heuristic Engine", "موتور تحلیلی و توپولوژی زنجیره"),
    MACHINE_LEARNING("Machine Learning / Statistical Model", "مدل‌های آماری و یادگیری ماشین"),
    INVESTIGATOR_MANUAL("Manual Investigator Annotation", "ثبت دستی کارشناس پرونده")
}

@Serializable
enum class VerificationStatus(val displayNameEn: String, val displayNameFa: String) {
    VERIFIED_OFFICIAL("Cryptographically / Officially Verified", "تاییدشده رسمی / داده بلاکچین"),
    CROWDSOURCED_CONFIRMED("Crowdsourced Multi-Party Confirmed", "تاییدشده همگانی و چندمنبعی"),
    HEURISTIC_CLUSTER("Heuristic Cluster Estimation", "تخمین خوشه آدرس‌ها بر اساس قانون تحلیلی"),
    INVESTIGATOR_ANNOTATED("Investigator Annotated", "ثبت‌شده توسط کارشناس پرونده"),
    UNVERIFIED("Unverified Preliminary Lead", "بررسی‌نشده / سرنخ اولیه")
}

/**
 * Provenance Record
 * Strictly captures the lineage, data source, transformation pipeline, and verification trail of any finding.
 */
@Serializable
data class ProvenanceRecord(
    val sourceName: String = "Blockchain Explorer",
    val sourceType: DataSourceType = DataSourceType.PUBLIC_EXPLORER_API,
    val retrievalTimestamp: Long = System.currentTimeMillis(),
    val endpointUrl: String? = null,
    val dataHashOrFingerprint: String? = null,
    val transformationPipeline: List<String> = emptyList(),
    val rawPayloadExcerpt: String? = null,
    val analystUsername: String? = "System Auditor",
    val providerId: String = "mempool_space_btc",
    val providerName: String = sourceName,
    val endpointQuery: String = endpointUrl ?: "",
    val network: String = "BITCOIN",
    val rawTxHash: String = "",
    val isCache: Boolean = false
)

/**
 * Enhanced Forensic Evidence Data Class
 * Every finding tracks provenance, category, confidence, bilingual titles/descriptions, and direct fact status.
 */
@Serializable
data class EvidenceItem(
    val id: String,
    val timestamp: Long,
    val category: EvidenceCategory,
    val title: String,
    val description: String,
    val rawDataSource: String,
    val providerName: String = "Blockchain Ledger",
    val confidence: ConfidenceLevel = ConfidenceLevel.DEFINITIVE_FACT,
    val txHash: String? = null,
    val relatedAddress: String? = null,
    val blockHeight: Long? = null,
    val isDirectFact: Boolean = true,
    val technicalMetadata: Map<String, String> = emptyMap(),
    val titleEn: String = title,
    val titleFa: String = title,
    val descriptionEn: String = description,
    val descriptionFa: String = description,
    val provenance: ProvenanceRecord = ProvenanceRecord(
        sourceName = providerName,
        sourceType = if (isDirectFact) DataSourceType.ON_CHAIN_RPC else DataSourceType.HEURISTIC_ENGINE,
        retrievalTimestamp = timestamp
    ),
    val verificationStatus: VerificationStatus = if (isDirectFact) VerificationStatus.VERIFIED_OFFICIAL else VerificationStatus.HEURISTIC_CLUSTER,
    val contentHash: String = "",
    val previousHash: String = "",
    val version: Int = 1
) {
    fun localizedTitle(isPersian: Boolean): String = if (isPersian) titleFa else titleEn
    fun localizedDescription(isPersian: Boolean): String = if (isPersian) descriptionFa else descriptionEn
}
