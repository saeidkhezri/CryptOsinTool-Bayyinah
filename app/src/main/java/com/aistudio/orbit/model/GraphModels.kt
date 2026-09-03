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
    UNKNOWN_WALLET
}

@Serializable
data class ForensicGraphNode(
    val id: String,                    // Blockchain address
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
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val size: Int = 12
)

@Serializable
data class ForensicGraphEdge(
    val id: String,
    val source: String,                // Source address
    val target: String,                // Target address
    val weight: Int = 1,               // Number of interactions
    val totalVolumeSat: Long = 0,      // Transferred satoshis
    val firstTxTime: Long = 0,
    val lastTxTime: Long = 0,
    val direction: TxDirection = TxDirection.INCOMING,
    val size: Int = 1
)

@Serializable
data class ForensicGraph(
    val nodes: List<ForensicGraphNode> = emptyList(),
    val edges: List<ForensicGraphEdge> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)
