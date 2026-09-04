package com.aistudio.orbit.model

import kotlinx.serialization.Serializable

@Serializable
enum class GraphNodeType {
    TARGET_SEED,
    PRIMARY_COUNTERPARTY,
    SECONDARY_HOP,
    EXCHANGE_HOT_WALLET,
    MIXER_OR_HIGH_RISK,
    SMART_CONTRACT,
    UNKNOWN_WALLET,
    CLUSTER,
    ENTITY,
    TRANSACTION,
    EVIDENCE
}

@Serializable
enum class GraphViewType {
    ADDRESS_GRAPH,
    TRANSACTION_GRAPH,
    CLUSTER_GRAPH,
    ENTITY_GRAPH,
    EVIDENCE_GRAPH,
    MONEY_FLOW
}

@Serializable
data class ForensicGraphNode(
    val id: String,                    // Blockchain address or cluster/entity/tx ID
    val label: String,                 // Display label or truncated address
    val network: BlockchainNetwork = BlockchainNetwork.BITCOIN,
    val nodeType: GraphNodeType = GraphNodeType.PRIMARY_COUNTERPARTY,
    val isSeed: Boolean = false,
    val balanceSat: Long = 0,
    val totalReceivedSat: Long = 0,
    val totalSentSat: Long = 0,
    val txCount: Int = 0,
    val degree: Int = 1,
    val riskSeverity: RiskSeverity = RiskSeverity.INFO,
    val centralityScore: Double = 0.0,
    val communityId: String? = null,
    val evidenceIds: List<String> = emptyList(),
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val size: Int = 12
)

@Serializable
data class ForensicGraphEdge(
    val id: String,
    val source: String,                // Source ID
    val target: String,                // Target ID
    val weight: Int = 1,               // Number of interactions or hop distance
    val totalVolumeSat: Long = 0,      // Transferred satoshis
    val firstTxTime: Long = 0,
    val lastTxTime: Long = 0,
    val direction: TxDirection = TxDirection.INCOMING,
    val evidenceIds: List<String> = emptyList(),
    val isShortestPath: Boolean = false,
    val size: Int = 1
)

@Serializable
data class MoneyFlowHop(
    val hopIndex: Int,
    val transactionId: String,
    val amountSat: Long,
    val timestamp: Long,
    val address: String,
    val clusterId: String? = null,
    val entityId: String? = null,
    val riskSeverity: RiskSeverity = RiskSeverity.INFO,
    val evidenceIds: List<String> = emptyList(),
    val confidence: ConfidenceLevel = ConfidenceLevel.LOW_CONFIDENCE
)

@Serializable
data class MoneyFlow(
    val id: String,
    val sourceAddress: String,
    val destinationAddress: String,
    val hops: List<MoneyFlowHop>,
    val overallConfidence: ConfidenceLevel = ConfidenceLevel.LOW_CONFIDENCE,
    val totalVolumeSat: Long
)

@Serializable
data class ForensicGraph(
    val nodes: List<ForensicGraphNode> = emptyList(),
    val edges: List<ForensicGraphEdge> = emptyList(),
    val moneyFlows: List<MoneyFlow> = emptyList(),
    val viewType: GraphViewType = GraphViewType.ADDRESS_GRAPH,
    val generatedAt: Long = System.currentTimeMillis()
)
