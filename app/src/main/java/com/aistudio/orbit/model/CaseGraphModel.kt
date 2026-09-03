package com.aistudio.orbit.model

import kotlinx.serialization.Serializable

/**
 * Categorization of nodes in the forensic investigation graph
 * embracing both on-chain blockchain entities and off-chain OSINT indicators.
 */
@Serializable
enum class ForensicEntityCategory {
    // On-Chain Ledger Entities
    CRYPTO_WALLET,
    SMART_CONTRACT,
    EXCHANGE_HOT_WALLET,
    MIXER_OR_TUMBLER,
    MINING_POOL,

    // Off-Chain OSINT Entities (Maltego / Epieos / SpiderFoot style)
    PHONE_NUMBER,
    EMAIL_ADDRESS,
    IP_NETWORK_NODE,
    SOCIAL_ACCOUNT,
    ALIAS_USERNAME,
    DOMAIN_NAME,

    // Core Graph Model Additions for Stage C
    BLOCKCHAIN_ADDRESS,
    TRANSACTION,
    BLOCK,
    TOKEN,
    CONTRACT,
    ENS,
    DOMAIN,
    PUBLIC_EMAIL,
    PUBLIC_PHONE,
    PUBLIC_USERNAME,
    PUBLIC_IP,
    PUBLIC_PROFILE,
    PUBLIC_ORGANIZATION,
    PUBLIC_SERVICE,
    EXCHANGE,
    CLUSTER,
    EVIDENCE,
    INVESTIGATION_CASE,
    PATTERN
}

/**
 * Epistemic classification of graph components to distinguish undisputed blockchain ledger facts
 * from algorithmic calculations, derived OSINT inferences, and working hypotheses.
 */
@Serializable
enum class ForensicEpistemicStatus(val displayNameEn: String, val displayNameFa: String) {
    UNDISPUTED_LEDGER_FACT("Undisputed Ledger Fact", "حقیقت قطعی دفترکل"),
    ALGORITHMIC_CALCULATION("Algorithmic Calculation", "محاسبه الگوریتمی"),
    DERIVED_OSINT_INFERENCE("Derived OSINT Inference", "استنتاج هوشمندی منابع باز"),
    WORKING_HYPOTHESIS("Working Hypothesis", "فرضیه کاری تحقیق")
}

/**
 * Relationship edge classification connecting on-chain entities with each other
 * and bridging on-chain wallets to off-chain OSINT footprints.
 */
@Serializable
enum class ForensicEdgeCategory {
    ON_CHAIN_TRANSFER,          // Direct cryptocurrency transfer (solid directed line)
    TRANSFORM_ATTRIBUTION,      // Maltego/Epieos transform link (dashed line with confidence)
    CO_SPEND_CLUSTER,           // GraphSense multi-input clustering relationship
    THREAT_CORRELATION,         // SpiderFoot or sanctions alert correlation
    NETWORK_PROPAGATION,         // P2P node / IP relay relationship

    // Core Graph Model Edges for Stage C
    SENT_TO,
    RECEIVED_FROM,
    TOKEN_TRANSFER,
    CONTRACT_INTERACTION,
    BELONGS_TO_CLUSTER,
    PUBLICLY_ASSOCIATED_WITH,
    ATTRIBUTED_BY_SOURCE,
    TEMPORALLY_CORRELATED,
    BEHAVIORALLY_CORRELATED,
    SUPPORTED_BY_EVIDENCE,
    INVESTIGATOR_LINKED
}

/**
 * Rich node data model for the interactive Jetpack Compose Canvas graph.
 */
@Serializable
data class InteractiveCaseNode(
    val id: String,                             // Address, E.164 phone, email, IP, or username
    val label: String,                          // Human-readable title
    val entityCategory: ForensicEntityCategory, // Wallet, phone, email, etc.
    val network: BlockchainNetwork = BlockchainNetwork.BITCOIN,
    val riskSeverity: RiskSeverity = RiskSeverity.INFO,
    val epistemicStatus: ForensicEpistemicStatus = ForensicEpistemicStatus.UNDISPUTED_LEDGER_FACT,
    val isSeed: Boolean = false,
    val isTarget: Boolean = false,
    val confidencePercent: Int = 100,           // Attribution confidence (1-100%)
    val balanceDisplay: String = "",            // Formatted balance or volume
    val txCount: Int = 0,                       // Number of interactions or hops
    val inDegree: Int = 0,
    val outDegree: Int = 0,
    val tags: List<String> = emptyList(),       // GraphSense TagPack / OSINT tags
    val metadata: Map<String, String> = emptyMap(), // Additional forensic metadata
    var x: Float = 0f,                          // Layout coordinate X
    var y: Float = 0f,                          // Layout coordinate Y
    var vx: Float = 0f,                         // Force-directed velocity X
    var vy: Float = 0f,                         // Force-directed velocity Y
    val visualRadius: Float = 24f               // Hit-testing and drawing radius
) {
    val isOnChain: Boolean
        get() = entityCategory in listOf(
            ForensicEntityCategory.CRYPTO_WALLET,
            ForensicEntityCategory.SMART_CONTRACT,
            ForensicEntityCategory.EXCHANGE_HOT_WALLET,
            ForensicEntityCategory.MIXER_OR_TUMBLER,
            ForensicEntityCategory.MINING_POOL,
            ForensicEntityCategory.BLOCKCHAIN_ADDRESS,
            ForensicEntityCategory.TRANSACTION,
            ForensicEntityCategory.BLOCK,
            ForensicEntityCategory.TOKEN,
            ForensicEntityCategory.CONTRACT,
            ForensicEntityCategory.EXCHANGE,
            ForensicEntityCategory.CLUSTER
        )

    val isOffChain: Boolean
        get() = !isOnChain
}

/**
 * Relationship edge connecting two entities in the forensic graph.
 */
@Serializable
data class InteractiveCaseEdge(
    val id: String,
    val sourceId: String,
    val targetId: String,
    val category: ForensicEdgeCategory = ForensicEdgeCategory.ON_CHAIN_TRANSFER,
    val volumeDisplay: String = "",             // e.g., "1.45 BTC" or "10,000 USDT"
    val volumeSat: Long = 0L,
    val txCount: Int = 1,
    val confidencePercent: Int = 100,           // e.g. 92% confidence for Holehe email->phone link
    val transformLabel: String = "",            // e.g. "Linked via Holehe", "Direct P2P", "Tor Relay"
    val isDirected: Boolean = true,
    val strokeWidth: Float = 2.5f
)

/**
 * Container for the comprehensive case investigation graph.
 */
@Serializable
data class InteractiveCaseGraph(
    val caseId: String,
    val targetAddress: String,
    val nodes: List<InteractiveCaseNode> = emptyList(),
    val edges: List<InteractiveCaseEdge> = emptyList(),
    val generatedTimestamp: Long = System.currentTimeMillis()
)
