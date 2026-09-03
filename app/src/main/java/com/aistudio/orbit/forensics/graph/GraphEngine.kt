package com.aistudio.orbit.forensics.graph

import com.aistudio.orbit.Edge
import com.aistudio.orbit.Graph
import com.aistudio.orbit.Node
import com.aistudio.orbit.model.*
import kotlinx.serialization.Serializable
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Interface representing a node in the blockchain investigation graph.
 */
interface IGraphNode {
    val id: String                      // Unique address
    val label: String                   // Truncated display label
    val network: BlockchainNetwork
    val nodeType: GraphNodeType
    val isSeed: Boolean
    val balanceSat: Long
    val totalReceivedSat: Long
    val totalSentSat: Long
    val txCount: Int
    val degree: Int
    val inDegree: Int
    val outDegree: Int
    val riskSeverity: RiskSeverity
    var x: Float
    var y: Float
    var vx: Float
    var vy: Float
    val size: Int
    val tags: List<String>
}

/**
 * Interface representing a directed transaction relationship edge in the graph.
 */
interface IGraphEdge {
    val id: String
    val sourceId: String                // Sending address
    val targetId: String                // Receiving address
    val txCount: Int                    // Interaction frequency
    val totalVolumeSat: Long            // Total transferred volume
    val firstTxTime: Long
    val lastTxTime: Long
    val direction: TxDirection
    val size: Int
    val confidence: ConfidenceLevel
}

@Serializable
data class ForensicNode(
    override val id: String,
    override val label: String,
    override val network: BlockchainNetwork = BlockchainNetwork.BITCOIN,
    override val nodeType: GraphNodeType = GraphNodeType.PRIMARY_COUNTERPARTY,
    override val isSeed: Boolean = false,
    override val balanceSat: Long = 0L,
    override val totalReceivedSat: Long = 0L,
    override val totalSentSat: Long = 0L,
    override val txCount: Int = 0,
    override val degree: Int = 1,
    override val inDegree: Int = 0,
    override val outDegree: Int = 0,
    override val riskSeverity: RiskSeverity = RiskSeverity.INFO,
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var vx: Float = 0f,
    override var vy: Float = 0f,
    override val size: Int = 14,
    override val tags: List<String> = emptyList()
) : IGraphNode

@Serializable
data class ForensicEdge(
    override val id: String,
    override val sourceId: String,
    override val targetId: String,
    override val txCount: Int = 1,
    override val totalVolumeSat: Long = 0L,
    override val firstTxTime: Long = 0L,
    override val lastTxTime: Long = 0L,
    override val direction: TxDirection = TxDirection.INCOMING,
    override val size: Int = 2,
    override val confidence: ConfidenceLevel = ConfidenceLevel.DEFINITIVE_FACT
) : IGraphEdge

data class FlowTraceResult(
    val path: List<String>,
    val totalHops: Int,
    val totalVolumeSat: Long,
    val intermediateNodes: List<IGraphNode>
)

data class PeelChainSubgraph(
    val seedAddress: String,
    val hopAddresses: List<String>,
    val peeledAmountsSat: List<Long>,
    val changeAmountsSat: List<Long>,
    val txHashes: List<String>
)

/**
 * Core Graph Engine Interface for Blockchain Forensics
 */
interface IGraphEngine {
    val nodes: Map<String, IGraphNode>
    val edges: List<IGraphEdge>
    val adjacencyList: Map<String, List<String>>

    fun addNode(node: IGraphNode)
    fun addEdge(edge: IGraphEdge)
    fun getNode(id: String): IGraphNode?
    fun getNeighbors(address: String): List<IGraphNode>
    fun findShortestPath(sourceId: String, targetId: String): List<String>
    fun traceFundFlow(startAddress: String, maxHops: Int = 3): List<FlowTraceResult>
    fun detectPeelChains(): List<PeelChainSubgraph>
    fun detectCycles(): List<List<String>>
    fun calculateCentralityScores(): Map<String, Int>
    fun stepForceDirectedLayout(width: Float, height: Float, damping: Float = 0.88f, repulsion: Float = 5500f, springLength: Float = 140f)
    fun toForensicGraph(): ForensicGraph
}

/**
 * Standard implementation of GraphEngine
 */
class GraphEngine : IGraphEngine {

    private val _nodes = mutableMapOf<String, IGraphNode>()
    private val _edges = mutableListOf<IGraphEdge>()
    private val _adjacency = mutableMapOf<String, MutableList<String>>()

    override val nodes: Map<String, IGraphNode> get() = _nodes
    override val edges: List<IGraphEdge> get() = _edges
    override val adjacencyList: Map<String, List<String>> get() = _adjacency

    override fun addNode(node: IGraphNode) {
        _nodes[node.id] = node
        if (!_adjacency.containsKey(node.id)) {
            _adjacency[node.id] = mutableListOf()
        }
    }

    override fun addEdge(edge: IGraphEdge) {
        _edges.add(edge)
        _adjacency.getOrPut(edge.sourceId) { mutableListOf() }.add(edge.targetId)
    }

    override fun getNode(id: String): IGraphNode? = _nodes[id]

    override fun getNeighbors(address: String): List<IGraphNode> {
        val neighborIds = _adjacency[address] ?: emptyList()
        return neighborIds.mapNotNull { _nodes[it] }
    }

    override fun findShortestPath(sourceId: String, targetId: String): List<String> {
        if (!_nodes.containsKey(sourceId) || !_nodes.containsKey(targetId)) return emptyList()
        if (sourceId.equals(targetId, ignoreCase = true)) return listOf(sourceId)

        val queue = ArrayDeque<String>()
        val visited = mutableSetOf<String>()
        val parentMap = mutableMapOf<String, String>()

        queue.add(sourceId)
        visited.add(sourceId)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current.equals(targetId, ignoreCase = true)) {
                val path = mutableListOf<String>()
                var curr: String? = targetId
                while (curr != null) {
                    path.add(curr)
                    curr = parentMap[curr]
                }
                return path.reversed()
            }

            val neighbors = _adjacency[current] ?: emptyList()
            for (neighbor in neighbors) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor)
                    parentMap[neighbor] = current
                    queue.add(neighbor)
                }
            }
        }
        return emptyList()
    }

    override fun traceFundFlow(startAddress: String, maxHops: Int): List<FlowTraceResult> {
        val results = mutableListOf<FlowTraceResult>()
        if (!_nodes.containsKey(startAddress)) return results

        fun dfs(current: String, currentPath: List<String>, currentVol: Long, hopsLeft: Int) {
            if (hopsLeft == 0) {
                if (currentPath.size > 1) {
                    val intermediates = currentPath.mapNotNull { _nodes[it] }
                    results.add(
                        FlowTraceResult(
                            path = currentPath,
                            totalHops = currentPath.size - 1,
                            totalVolumeSat = currentVol,
                            intermediateNodes = intermediates
                        )
                    )
                }
                return
            }

            val outgoingEdges = _edges.filter { it.sourceId.equals(current, ignoreCase = true) }
            if (outgoingEdges.isEmpty()) {
                if (currentPath.size > 1) {
                    val intermediates = currentPath.mapNotNull { _nodes[it] }
                    results.add(
                        FlowTraceResult(
                            path = currentPath,
                            totalHops = currentPath.size - 1,
                            totalVolumeSat = currentVol,
                            intermediateNodes = intermediates
                        )
                    )
                }
                return
            }

            var branched = false
            for (edge in outgoingEdges) {
                if (!currentPath.contains(edge.targetId)) {
                    branched = true
                    dfs(
                        edge.targetId,
                        currentPath + edge.targetId,
                        currentVol + edge.totalVolumeSat,
                        hopsLeft - 1
                    )
                }
            }

            if (!branched && currentPath.size > 1) {
                val intermediates = currentPath.mapNotNull { _nodes[it] }
                results.add(
                    FlowTraceResult(
                        path = currentPath,
                        totalHops = currentPath.size - 1,
                        totalVolumeSat = currentVol,
                        intermediateNodes = intermediates
                    )
                )
            }
        }

        dfs(startAddress, listOf(startAddress), 0L, maxHops)
        return results
    }

    override fun detectPeelChains(): List<PeelChainSubgraph> {
        val peelChains = mutableListOf<PeelChainSubgraph>()
        for ((address, node) in _nodes) {
            val outgoing = _edges.filter { it.sourceId.equals(address, ignoreCase = true) }
            if (outgoing.size == 2) {
                val v1 = outgoing[0].totalVolumeSat
                val v2 = outgoing[1].totalVolumeSat
                val minV = minOf(v1, v2)
                val maxV = maxOf(v1, v2)
                if (maxV > 0 && (minV.toDouble() / maxV.toDouble()) <= 0.35) {
                    peelChains.add(
                        PeelChainSubgraph(
                            seedAddress = address,
                            hopAddresses = listOf(outgoing[0].targetId, outgoing[1].targetId),
                            peeledAmountsSat = listOf(minV),
                            changeAmountsSat = listOf(maxV),
                            txHashes = listOf(outgoing[0].id, outgoing[1].id)
                        )
                    )
                }
            }
        }
        return peelChains
    }

    override fun detectCycles(): List<List<String>> {
        val cycles = mutableListOf<List<String>>()
        val visited = mutableSetOf<String>()
        val recStack = mutableSetOf<String>()
        val currentPath = mutableListOf<String>()

        fun dfsCycle(u: String) {
            visited.add(u)
            recStack.add(u)
            currentPath.add(u)

            val neighbors = _adjacency[u] ?: emptyList()
            for (v in neighbors) {
                if (!visited.contains(v)) {
                    dfsCycle(v)
                } else if (recStack.contains(v)) {
                    val cycleStartIndex = currentPath.indexOf(v)
                    if (cycleStartIndex != -1) {
                        cycles.add(currentPath.subList(cycleStartIndex, currentPath.size) + v)
                    }
                }
            }

            currentPath.removeAt(currentPath.size - 1)
            recStack.remove(u)
        }

        for (nodeId in _nodes.keys) {
            if (!visited.contains(nodeId)) {
                dfsCycle(nodeId)
            }
        }
        return cycles
    }

    override fun calculateCentralityScores(): Map<String, Int> {
        val centrality = mutableMapOf<String, Int>()
        for (nodeId in _nodes.keys) {
            val inCount = _edges.count { it.targetId.equals(nodeId, ignoreCase = true) }
            val outCount = _edges.count { it.sourceId.equals(nodeId, ignoreCase = true) }
            centrality[nodeId] = inCount + outCount
        }
        return centrality
    }

    override fun stepForceDirectedLayout(
        width: Float,
        height: Float,
        damping: Float,
        repulsion: Float,
        springLength: Float
    ) {
        val nodeList = _nodes.values.toList()
        val n = nodeList.size
        if (n <= 1) return

        for (i in 0 until n) {
            val nodeA = nodeList[i]
            for (j in (i + 1) until n) {
                val nodeB = nodeList[j]
                val dx = nodeB.x - nodeA.x
                val dy = nodeB.y - nodeA.y
                val distSq = dx * dx + dy * dy + 0.01f
                val dist = sqrt(distSq)
                if (dist > 0.01f) {
                    val force = repulsion / distSq
                    val fx = (dx / dist) * force
                    val fy = (dy / dist) * force
                    nodeA.vx -= fx
                    nodeA.vy -= fy
                    nodeB.vx += fx
                    nodeB.vy += fy
                }
            }
        }

        for (edge in _edges) {
            val sourceNode = _nodes[edge.sourceId] ?: continue
            val targetNode = _nodes[edge.targetId] ?: continue
            val dx = targetNode.x - sourceNode.x
            val dy = targetNode.y - sourceNode.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > 0.01f) {
                val springForce = (dist - springLength) * 0.04f
                val fx = (dx / dist) * springForce
                val fy = (dy / dist) * springForce
                sourceNode.vx += fx
                sourceNode.vy += fy
                targetNode.vx -= fx
                targetNode.vy -= fy
            }
        }

        val halfW = (width / 2f).coerceAtLeast(180f)
        val halfH = (height / 2f).coerceAtLeast(180f)

        for (node in nodeList) {
            if (node.isSeed) {
                node.x = 0f
                node.y = 0f
                node.vx = 0f
                node.vy = 0f
                continue
            }
            node.vx *= damping
            node.vy *= damping
            node.x = (node.x + node.vx).coerceIn(-halfW, halfW)
            node.y = (node.y + node.vy).coerceIn(-halfH, halfH)
        }
    }

    override fun toForensicGraph(): ForensicGraph {
        val forensicNodes = _nodes.values.map { node ->
            ForensicGraphNode(
                id = node.id,
                label = node.label,
                network = node.network,
                nodeType = node.nodeType,
                isSeed = node.isSeed,
                balanceSat = node.balanceSat,
                totalReceivedSat = node.totalReceivedSat,
                totalSentSat = node.totalSentSat,
                txCount = node.txCount,
                degree = node.degree,
                riskSeverity = node.riskSeverity,
                x = node.x,
                y = node.y,
                vx = node.vx,
                vy = node.vy,
                size = node.size
            )
        }

        val forensicEdges = _edges.map { edge ->
            ForensicGraphEdge(
                id = edge.id,
                source = edge.sourceId,
                target = edge.targetId,
                weight = edge.txCount,
                totalVolumeSat = edge.totalVolumeSat,
                firstTxTime = edge.firstTxTime,
                lastTxTime = edge.lastTxTime,
                direction = edge.direction,
                size = edge.size
            )
        }

        return ForensicGraph(
            nodes = forensicNodes,
            edges = forensicEdges,
            generatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        fun buildFromInvestigation(
            targetAddress: String,
            network: BlockchainNetwork,
            counterparties: List<CounterpartySummary>,
            transactions: List<ForensicTransaction> = emptyList()
        ): GraphEngine {
            val engine = GraphEngine()

            val totalRecv: Long = counterparties.sumOf { it.totalReceivedSatFromCounterparty }
            val totalSent: Long = counterparties.sumOf { it.totalSentSatToCounterparty }
            val totalTxs: Int = counterparties.sumOf { it.txCount }

            val seedNode = ForensicNode(
                id = targetAddress,
                label = targetAddress.take(8) + "...",
                network = network,
                nodeType = GraphNodeType.TARGET_SEED,
                isSeed = true,
                totalReceivedSat = totalRecv,
                totalSentSat = totalSent,
                txCount = totalTxs,
                degree = counterparties.size,
                x = 0f,
                y = 0f,
                size = 28,
                tags = listOf("SEED_TARGET", "INVESTIGATION_ORIGIN")
            )
            engine.addNode(seedNode)

            val topCounterparties = counterparties.take(24)
            val count = topCounterparties.size
            val radius = 220f

            for ((index, cp) in topCounterparties.withIndex()) {
                val angle = (2 * Math.PI * index / count.coerceAtLeast(1)).toFloat()
                val x = (radius * cos(angle)) + (Math.random().toFloat() * 16f - 8f)
                val y = (radius * sin(angle)) + (Math.random().toFloat() * 16f - 8f)
                val nodeSize = (14 + (cp.txCount * 2)).coerceIn(14, 26)

                val nodeType = when {
                    cp.txCount > 50 -> GraphNodeType.EXCHANGE_HOT_WALLET
                    cp.totalReceivedSatFromCounterparty > 100_000_000 -> GraphNodeType.PRIMARY_COUNTERPARTY
                    else -> GraphNodeType.UNKNOWN_WALLET
                }

                val cpNode = ForensicNode(
                    id = cp.address,
                    label = cp.address.take(8) + "...",
                    network = network,
                    nodeType = nodeType,
                    isSeed = false,
                    totalReceivedSat = cp.totalReceivedSatFromCounterparty,
                    totalSentSat = cp.totalSentSatToCounterparty,
                    txCount = cp.txCount,
                    degree = 1,
                    x = x,
                    y = y,
                    size = nodeSize,
                    tags = listOf("COUNTERPARTY")
                )
                engine.addNode(cpNode)

                val edgeDirection = if (cp.totalReceivedSatFromCounterparty >= cp.totalSentSatToCounterparty) TxDirection.INCOMING else TxDirection.OUTGOING
                val source = if (edgeDirection == TxDirection.OUTGOING) targetAddress else cp.address
                val target = if (edgeDirection == TxDirection.OUTGOING) cp.address else targetAddress

                val edge = ForensicEdge(
                    id = "edge_${source.take(4)}_${target.take(4)}_$index",
                    sourceId = source,
                    targetId = target,
                    txCount = cp.txCount,
                    totalVolumeSat = cp.totalReceivedSatFromCounterparty + cp.totalSentSatToCounterparty,
                    firstTxTime = cp.firstSeenTimestamp,
                    lastTxTime = cp.lastSeenTimestamp,
                    direction = edgeDirection,
                    size = cp.txCount.coerceIn(1, 8)
                )
                engine.addEdge(edge)
            }

            return engine
        }

        fun stepForceDirectedLayout(graph: Graph, iterations: Int = 30): Graph {
            val nodeList = graph.nodes
            val n = nodeList.size
            if (n <= 1) return graph

            val positions = nodeList.map { it.copy() }.toMutableList()
            val vx = FloatArray(n) { 0f }
            val vy = FloatArray(n) { 0f }

            val repulsion = 4500f
            val springLength = 130f
            val damping = 0.85f

            val nodeIndexMap = positions.mapIndexed { idx, node -> node.id to idx }.toMap()

            for (iter in 0 until iterations) {
                // Repulsion
                for (i in 0 until n) {
                    for (j in (i + 1) until n) {
                        val dx = positions[j].x - positions[i].x
                        val dy = positions[j].y - positions[i].y
                        val distSq = dx * dx + dy * dy + 0.01f
                        val dist = sqrt(distSq)
                        if (dist > 0.01f) {
                            val force = repulsion / distSq
                            val fx = (dx / dist) * force
                            val fy = (dy / dist) * force
                            vx[i] -= fx
                            vy[i] -= fy
                            vx[j] += fx
                            vy[j] += fy
                        }
                    }
                }

                // Spring Attraction along edges
                for (edge in graph.edges) {
                    val i = nodeIndexMap[edge.source] ?: continue
                    val j = nodeIndexMap[edge.target] ?: continue
                    val dx = positions[j].x - positions[i].x
                    val dy = positions[j].y - positions[i].y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist > 0.01f) {
                        val force = (dist - springLength) * 0.04f
                        val fx = (dx / dist) * force
                        val fy = (dy / dist) * force
                        vx[i] += fx
                        vy[i] += fy
                        vx[j] -= fx
                        vy[j] -= fy
                    }
                }

                // Update
                for (i in 0 until n) {
                    if (i == 0) { // seed node
                        positions[i] = positions[i].copy(x = 0f, y = 0f)
                        continue
                    }
                    vx[i] *= damping
                    vy[i] *= damping
                    val newX = (positions[i].x + vx[i]).coerceIn(-280f, 280f)
                    val newY = (positions[i].y + vy[i]).coerceIn(-280f, 280f)
                    positions[i] = positions[i].copy(x = newX, y = newY)
                }
            }

            return Graph(nodes = positions, edges = graph.edges)
        }
    }
}
