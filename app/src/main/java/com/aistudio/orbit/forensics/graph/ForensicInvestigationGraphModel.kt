package com.aistudio.orbit.forensics.graph

import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.RiskSeverity
import kotlinx.serialization.Serializable

/**
 * Node Types supported by the Biyena Forensic Graph Engine.
 */
@Serializable
enum class VisualNodeType(val displayNameEn: String, val displayNameFa: String) {
    ADDRESS("Blockchain Address", "آدرس بلاکچین"),
    TRANSACTION("Transaction", "تراکنش"),
    BLOCK("Block", "بلاک"),
    CLUSTER("Address Cluster", "خوشه آدرس‌ها"),
    ENTITY("Entity", "موجودیت / هویت"),
    PERSON("Person", "شخص حقیقی"),
    ORGANIZATION("Organization", "سازمان"),
    EXCHANGE("Exchange / VASP", "صرافی / VASP"),
    VASP("Virtual Asset Service Provider", "ارائه‌دهنده خدمات دارایی مجازی"),
    MERCHANT("Merchant", "پذیرنده تجاری"),
    SERVICE("Online Service", "سرویس برخط"),
    MIXER("Mixer / Tumbler", "میکسر / کوین‌جوین"),
    MINING_POOL("Mining Pool", "استخر استخراج"),
    DOMAIN("Domain Name", "دامنه اینترنتی"),
    URL("Web URL", "آدرس وب (URL)"),
    EMAIL("Email Address", "آدرس ایمیل"),
    USERNAME("Username / Handle", "نام کاربری / شناسه"),
    IP("IP Address", "آدرس آی‌پی"),
    ASN("Autonomous System (ASN)", "شماره سیستم خودگردان (ASN)"),
    COUNTRY("Country / Jurisdiction", "کشور / حوزه قضایی"),
    CITY("City", "شهر"),
    EVIDENCE("Evidence Item", "آیتم ادله دیجیتال"),
    SOURCE("Source Record", "منبع استنادی"),
    HYPOTHESIS("Investigative Hypothesis", "فرضیه تحقیقاتی"),
    FINDING("Forensic Finding", "یافته مستند کارشناسی"),
    ALERT("Sanctions / Threat Alert", "هشدار تحریم / تهدید")
}

/**
 * Edge Relationship Types supported by the Forensic Graph Engine.
 */
@Serializable
enum class VisualEdgeType(val displayNameEn: String, val displayNameFa: String) {
    SENT("Sent Value To", "ارسال ارزش به"),
    RECEIVED("Received From", "دریافت از"),
    SPENT("Spent UTXO In", "مصرف UTXO در"),
    CREATED("Created / Deployed", "ایجاد / استقرار"),
    FUNDED("Funded Account", "تامین مالی حساب"),
    CONSOLIDATED("Consolidated Funds", "تجمیع سرمایه"),
    SPLIT("Split / Dispersed", "توزیع / خرد کردن تراکنش"),
    BELONGS_TO_CLUSTER("Belongs To Cluster", "عضویت در خوشه"),
    ATTRIBUTED_TO("Attributed To", "منتسب به"),
    ASSOCIATED_WITH("Associated With", "مرتبط با"),
    MENTIONS("Mentions Identifier", "اشاره به شناسه"),
    SELF_PUBLISHED("Self-Published By", "منتشرشده توسط"),
    RESOLVES_TO("DNS Resolves To", "ترجمه نام DNS به"),
    HOSTED_BY("Hosted By ASN/Server", "میزبانی‌شده توسط"),
    REFERENCED_BY("Referenced In Record", "ارجاع‌شده در گزارش"),
    REPORTED_BY("Reported By Source", "گزارش‌شده توسط منبع"),
    SUPPORTS("Supports Hypothesis", "تاییدکننده فرضیه"),
    CONTRADICTS("Contradicts Claim", "ناقض ادعا"),
    DERIVED_FROM("Derived From Source", "استخراج‌شده از منبع")
}

/**
 * Visual Confidence Level (Orthogonal to Risk Severity).
 */
@Serializable
enum class VisualConfidence(val displayNameEn: String, val displayNameFa: String, val weight: Float) {
    CONFIRMED("Confirmed (Direct Ledger/Legal Record)", "قطعی (دفترکل مستقیم / سند رسمی)", 1.0f),
    HIGH("High Confidence (Multi-Source)", "بالا (تایید چندمنبعی مستقل)", 0.85f),
    MEDIUM("Medium Confidence (Single Corroborated)", "متوسط (منبع معتبر واحد)", 0.60f),
    LOW("Low Confidence (Heuristic / Unverified)", "پایین (اکتشافی / تاییدنشده)", 0.35f),
    UNKNOWN("Unknown / Pending Verification", "نامشخص / نیازمند راستی‌آزمایی", 0.15f),
    CONTESTED("Contested / Disputed Attribution", "مورد مناقشه / دارای تعارض", 0.0f)
}

/**
 * Epistemic Edge Styling Category.
 */
@Serializable
enum class EdgeEpistemicStyle {
    FACT,         // Solid edge - Cryptographic blockchain reality or official document
    DERIVED,      // Semi-solid edge - Algorithmic calculation (e.g. UTXO flow, balance)
    INFERENCE,    // Dotted edge - OSINT correlation or tagpack attribution
    HYPOTHESIS,   // Dashed edge - Investigator working model
    CONTESTED,    // Double-dashed warning edge - Contradictory reports
    REJECTED      // Faded/Grayed edge - Refuted claim
}

/**
 * Heuristic Method used for Clustering.
 */
@Serializable
enum class ClusteringMethod(val displayNameEn: String, val displayNameFa: String) {
    COMMON_INPUT("Common-Input Ownership Heuristic", "قاعده تجمیع ورودی‌های مشترک"),
    CHANGE_PATTERN("Change-Address Pattern Heuristic", "الگوی تشخیص آدرس باقیمانده"),
    KNOWN_SERVICE("Known-Service TagPack Attribution", "انتساب برچسب سرویس شناخته‌شده"),
    BEHAVIORAL_SIMILARITY("Behavioral Temporal Similarity", "شباهت زمانی-رفتاری"),
    EXTERNAL_OSINT("External OSINT Corroboration", "تایید متقاطع منابع باز"),
    ANALYST_DEFINED("Analyst-Defined Forensic Cluster", "خوشه تعریف‌شده توسط کارشناس")
}

/**
 * Comprehensive Node model for the Biyena Visual Investigation Engine.
 */
@Serializable
data class VisualInvestigationNode(
    val id: String,
    val label: String,
    val nodeType: VisualNodeType,
    val primaryIdentifier: String,
    val secondaryIdentifier: String = "",
    val entityId: String? = null,
    val clusterId: String? = null,
    val network: BlockchainNetwork = BlockchainNetwork.BITCOIN,
    val riskSeverity: RiskSeverity = RiskSeverity.INFO,
    val confidence: VisualConfidence = VisualConfidence.CONFIRMED,
    val isSeed: Boolean = false,
    val isTarget: Boolean = false,
    val isCollapsed: Boolean = false,
    val memberCount: Int = 1, // For clusters
    val sourceCount: Int = 1,
    val evidenceCount: Int = 0,
    val evidenceIds: List<String> = emptyList(),
    val balanceDisplay: String = "",
    val volumeBtc: Double = 0.0,
    val txCount: Int = 0,
    val inDegree: Int = 0,
    val outDegree: Int = 0,
    val firstSeenTimestamp: Long = 0L,
    val lastSeenTimestamp: Long = 0L,
    val sanctionsMatch: Boolean = false,
    val mixerExposure: Boolean = false,
    val darkWebExposure: Boolean = false,
    val tags: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val radius: Float = 28f
) {
    val isAddressOrTx: Boolean
        get() = nodeType in listOf(VisualNodeType.ADDRESS, VisualNodeType.TRANSACTION, VisualNodeType.BLOCK)

    val isOffChain: Boolean
        get() = nodeType in listOf(
            VisualNodeType.DOMAIN, VisualNodeType.URL, VisualNodeType.EMAIL,
            VisualNodeType.USERNAME, VisualNodeType.IP, VisualNodeType.ASN,
            VisualNodeType.PERSON, VisualNodeType.ORGANIZATION
        )
}

/**
 * Comprehensive Edge model for the Biyena Visual Investigation Engine.
 */
@Serializable
data class VisualInvestigationEdge(
    val id: String,
    val sourceId: String,
    val targetId: String,
    val relationshipType: VisualEdgeType,
    val epistemicStyle: EdgeEpistemicStyle = EdgeEpistemicStyle.FACT,
    val direction: Boolean = true, // Directed
    val amountDisplay: String = "",
    val amountSat: Long = 0L,
    val asset: String = "BTC",
    val timestamp: Long = 0L,
    val firstSeen: Long = 0L,
    val lastSeen: Long = 0L,
    val transactionId: String = "",
    val confidence: VisualConfidence = VisualConfidence.CONFIRMED,
    val source: String = "Blockchain Ledger",
    val evidenceIds: List<String> = emptyList(),
    val status: String = "ACTIVE",
    val strokeWidth: Float = 2.5f,
    val transformLabel: String = ""
)

/**
 * Filter Configuration State for the Forensic Visualizer.
 */
@Serializable
data class VisualFilterState(
    val query: String = "",
    val selectedNodeTypes: Set<VisualNodeType> = VisualNodeType.values().toSet(),
    val selectedEdgeTypes: Set<VisualEdgeType> = VisualEdgeType.values().toSet(),
    val minAmountBtc: Double = 0.0,
    val minConfidence: VisualConfidence = VisualConfidence.LOW,
    val minRiskSeverity: RiskSeverity = RiskSeverity.INFO,
    val showOnChainOnly: Boolean = false,
    val showOffChainOnly: Boolean = false,
    val showHighRiskOnly: Boolean = false,
    val showSanctionedOnly: Boolean = false,
    val maxHopDepth: Int = 3,
    val hideLowDegreeNodes: Boolean = false,
    val startTime: Long = 0L,
    val endTime: Long = Long.MAX_VALUE
)

/**
 * Graph Anomaly Flags.
 */
@Serializable
data class GraphAnomalyIndicator(
    val nodeId: String,
    val titleEn: String,
    val titleFa: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val anomalyType: AnomalyType,
    val riskSeverity: RiskSeverity,
    val evidenceId: String? = null
)

@Serializable
enum class AnomalyType {
    FAN_IN,
    FAN_OUT,
    PEEL_CHAIN,
    CONSOLIDATION,
    REPEATED_SPLITTING,
    DORMANT_TO_ACTIVE,
    RAPID_MOVEMENT,
    MULTIPLE_HOPS,
    MIXER_EXPOSURE,
    EXCHANGE_EXPOSURE,
    SANCTIONS_EXPOSURE
}
