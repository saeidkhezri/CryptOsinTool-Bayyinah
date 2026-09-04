package com.aistudio.orbit.forensics.graph

import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.ConfidenceLevel
import com.aistudio.orbit.model.ForensicTransaction
import com.aistudio.orbit.model.MoneyFlow
import com.aistudio.orbit.model.MoneyFlowHop
import com.aistudio.orbit.model.RiskSeverity
import java.util.ArrayDeque
import java.util.UUID

data class MoneyFlowTraversalConfig(
    val maxHops: Int = 4,
    val maxFlows: Int = 100,
    val minAmountSat: Long = 0L,
    val maxNodes: Int = 2000
)

object MoneyFlowEngine {
    /**
     * Traverses only the transactions supplied by the caller. It never pretends that
     * missing hops were fetched. Provider-backed recursive discovery belongs above this
     * engine and must append its own provenance before calling this function.
     */
    fun traceMoneyFlow(
        startAddress: String,
        targetAddress: String,
        transactions: List<ForensicTransaction>,
        network: BlockchainNetwork,
        maxHops: Int = 4
    ): List<MoneyFlow> = traceMoneyFlow(startAddress, targetAddress, transactions, network, MoneyFlowTraversalConfig(maxHops=maxHops))

    fun traceMoneyFlow(
        startAddress: String,
        targetAddress: String,
        transactions: List<ForensicTransaction>,
        network: BlockchainNetwork,
        config: MoneyFlowTraversalConfig
    ): List<MoneyFlow> {
        if (startAddress.isBlank() || targetAddress.isBlank() || config.maxHops < 1) return emptyList()
        data class Edge(val from:String, val to:String, val tx:ForensicTransaction, val amount:Long)
        val edgesByFrom = mutableMapOf<String, MutableList<Edge>>()
        transactions.filter { it.network == network }.forEach { tx ->
            tx.inputs.mapNotNull { it.address?.trim()?.takeIf(String::isNotBlank) }.forEach { from ->
                tx.outputs.filter { it.valueSat >= config.minAmountSat }.forEach { out ->
                    val to = out.address?.trim()?.takeIf(String::isNotBlank) ?: return@forEach
                    edgesByFrom.getOrPut(from.lowercase()) { mutableListOf() }.add(Edge(from,to,tx,out.valueSat))
                }
            }
        }
        data class Path(val address:String, val hops:List<MoneyFlowHop>, val visited:Set<String>, val volume:Long)
        val queue = ArrayDeque<Path>()
        queue.add(Path(startAddress, emptyList(), setOf(startAddress.lowercase()), 0L))
        val flows = mutableListOf<MoneyFlow>()
        var nodesVisited = 0
        while (queue.isNotEmpty() && flows.size < config.maxFlows && nodesVisited < config.maxNodes) {
            val path = queue.removeFirst(); nodesVisited++
            if (path.hops.size >= config.maxHops) continue
            for (edge in edgesByFrom[path.address.lowercase()].orEmpty()) {
                val next = edge.to.lowercase()
                if (next in path.visited) continue
                val hopIndex = path.hops.size + 1
                val hop = MoneyFlowHop(
                    hopIndex = hopIndex,
                    transactionId = edge.tx.txId,
                    amountSat = edge.amount,
                    timestamp = edge.tx.timestamp,
                    address = edge.to,
                    riskSeverity = RiskSeverity.INFO,
                    confidence = ConfidenceLevel.HIGH_CONFIDENCE
                )
                val newHops = path.hops + hop
                val newVolume = if (path.volume == 0L) edge.amount else minOf(path.volume, edge.amount)
                if (next == targetAddress.lowercase()) {
                    flows.add(MoneyFlow("FLOW_${UUID.randomUUID()}", startAddress, targetAddress, newHops, ConfidenceLevel.HIGH_CONFIDENCE, newVolume))
                    if (flows.size >= config.maxFlows) break
                } else {
                    queue.addLast(Path(edge.to, newHops, path.visited + next, newVolume))
                }
            }
        }
        return flows
    }
}
