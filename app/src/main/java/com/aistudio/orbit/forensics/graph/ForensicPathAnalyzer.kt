package com.aistudio.orbit.forensics.graph

import com.aistudio.orbit.model.RiskSeverity
import java.util.*

/**
 * Result of a Path Analysis operation.
 */
data class PathAnalysisResult(
    val titleEn: String,
    val titleFa: String,
    val pathNodeIds: List<String>,
    val pathEdgeIds: List<String>,
    val totalHopCount: Int,
    val totalVolumeBtc: Double,
    val minConfidence: VisualConfidence,
    val maxRiskSeverity: RiskSeverity,
    val involvedEvidenceIds: List<String>,
    val durationSeconds: Long = 0L,
    val summaryEn: String,
    val summaryFa: String
)

/**
 * NetworkX / igraph style Graph Path Analysis Engine for Forensic Investigations.
 */
object ForensicPathAnalyzer {

    /**
     * Find Shortest Path between startNodeId and targetNodeId using BFS.
     */
    fun findShortestPath(
        nodes: List<VisualInvestigationNode>,
        edges: List<VisualInvestigationEdge>,
        startId: String,
        targetId: String
    ): PathAnalysisResult? {
        if (startId == targetId) return null
        val nodeMap = nodes.associateBy { it.id }
        if (!nodeMap.containsKey(startId) || !nodeMap.containsKey(targetId)) return null

        val adj = mutableMapOf<String, MutableList<VisualInvestigationEdge>>()
        edges.forEach { edge ->
            adj.getOrPut(edge.sourceId) { mutableListOf() }.add(edge)
        }

        val queue: java.util.Queue<String> = java.util.LinkedList()
        val visited = mutableSetOf<String>()
        val parentEdge = mutableMapOf<String, VisualInvestigationEdge>()

        queue.add(startId)
        visited.add(startId)

        var found = false
        while (queue.isNotEmpty()) {
            val curr = queue.poll() ?: continue
            if (curr == targetId) {
                found = true
                break
            }

            for (edge in adj[curr] ?: emptyList()) {
                val next = edge.targetId
                if (!visited.contains(next)) {
                    visited.add(next)
                    parentEdge[next] = edge
                    queue.add(next)
                }
            }
        }

        if (!found) return null

        // Reconstruct path
        val pathEdges = mutableListOf<VisualInvestigationEdge>()
        var curr = targetId
        while (curr != startId) {
            val edge = parentEdge[curr] ?: break
            pathEdges.add(0, edge)
            curr = edge.sourceId
        }

        val pathNodeIds = mutableListOf(startId)
        pathEdges.forEach { pathNodeIds.add(it.targetId) }

        val totalVol = pathEdges.sumOf { it.amountSat.toDouble() / 100_000_000.0 }
        val minConf = pathEdges.minByOrNull { it.confidence.weight }?.confidence ?: VisualConfidence.CONFIRMED
        val maxRisk = pathNodeIds.mapNotNull { nodeMap[it]?.riskSeverity }.maxByOrNull { it.ordinal } ?: RiskSeverity.INFO
        val evidenceList = pathEdges.flatMap { it.evidenceIds }.distinct()

        return PathAnalysisResult(
            titleEn = "Shortest Forensic Path",
            titleFa = "کوتاه‌ترین مسیر پی‌جویی ادله",
            pathNodeIds = pathNodeIds,
            pathEdgeIds = pathEdges.map { it.id },
            totalHopCount = pathEdges.size,
            totalVolumeBtc = totalVol,
            minConfidence = minConf,
            maxRiskSeverity = maxRisk,
            involvedEvidenceIds = evidenceList,
            summaryEn = "Identified ${pathEdges.size}-hop path between $startId and $targetId carrying ${String.format("%.4f", totalVol)} BTC.",
            summaryFa = "مسیر با ${pathEdges.size} گام میان مبدا و مقصد با حجم کل انتقال ${String.format("%.4f", totalVol)} بیت‌کوین شناسایی گردید."
        )
    }

    /**
     * Find Highest-Value Path from a seed node.
     */
    fun findHighestValuePath(
        nodes: List<VisualInvestigationNode>,
        edges: List<VisualInvestigationEdge>,
        startId: String,
        maxHops: Int = 4
    ): PathAnalysisResult? {
        val nodeMap = nodes.associateBy { it.id }
        if (!nodeMap.containsKey(startId)) return null

        val adj = mutableMapOf<String, MutableList<VisualInvestigationEdge>>()
        edges.forEach { edge ->
            adj.getOrPut(edge.sourceId) { mutableListOf() }.add(edge)
        }

        var bestPath = listOf<VisualInvestigationEdge>()
        var bestVolume = 0.0

        fun dfs(curr: String, currentPath: List<VisualInvestigationEdge>, visited: Set<String>, currentVol: Double) {
            if (currentPath.size >= maxHops) {
                if (currentVol > bestVolume) {
                    bestVolume = currentVol
                    bestPath = currentPath
                }
                return
            }

            val nextEdges = adj[curr] ?: emptyList()
            if (nextEdges.isEmpty() && currentPath.isNotEmpty()) {
                if (currentVol > bestVolume) {
                    bestVolume = currentVol
                    bestPath = currentPath
                }
                return
            }

            for (edge in nextEdges) {
                if (!visited.contains(edge.targetId)) {
                    val vol = edge.amountSat.toDouble() / 100_000_000.0
                    dfs(edge.targetId, currentPath + edge, visited + edge.targetId, currentVol + vol)
                }
            }
        }

        dfs(startId, emptyList(), setOf(startId), 0.0)

        if (bestPath.isEmpty()) return null

        val pathNodeIds = mutableListOf(startId)
        bestPath.forEach { pathNodeIds.add(it.targetId) }
        val minConf = bestPath.minByOrNull { it.confidence.weight }?.confidence ?: VisualConfidence.CONFIRMED
        val maxRisk = pathNodeIds.mapNotNull { nodeMap[it]?.riskSeverity }.maxByOrNull { it.ordinal } ?: RiskSeverity.INFO

        return PathAnalysisResult(
            titleEn = "Highest-Value Fund Traversal",
            titleFa = "مسیر انتقال بالاترین ارزش مالی",
            pathNodeIds = pathNodeIds,
            pathEdgeIds = bestPath.map { it.id },
            totalHopCount = bestPath.size,
            totalVolumeBtc = bestVolume,
            minConfidence = minConf,
            maxRiskSeverity = maxRisk,
            involvedEvidenceIds = bestPath.flatMap { it.evidenceIds }.distinct(),
            summaryEn = "Dominant value flow carrying ${String.format("%.4f", bestVolume)} BTC across ${bestPath.size} hops.",
            summaryFa = "جریان ارزش غالب با انتقال ${String.format("%.4f", bestVolume)} بیت‌کوین در طول ${bestPath.size} گام ردیابی شد."
        )
    }

    /**
     * Find paths to known Exchanges or VASPs (Exit points).
     */
    fun findPathsToExchanges(
        nodes: List<VisualInvestigationNode>,
        edges: List<VisualInvestigationEdge>,
        startId: String
    ): List<PathAnalysisResult> {
        val exchangeNodes = nodes.filter {
            it.nodeType == VisualNodeType.EXCHANGE || it.nodeType == VisualNodeType.VASP ||
            it.tags.any { tag -> tag.contains("Exchange", ignoreCase = true) || tag.contains("Binance", ignoreCase = true) || tag.contains("Kraken", ignoreCase = true) }
        }

        val results = mutableListOf<PathAnalysisResult>()
        for (exchange in exchangeNodes) {
            val path = findShortestPath(nodes, edges, startId, exchange.id)
            if (path != null) {
                results.add(path)
            }
        }
        return results
    }

    /**
     * Compute Degree Centrality for all nodes.
     */
    fun computeDegreeCentrality(
        nodes: List<VisualInvestigationNode>,
        edges: List<VisualInvestigationEdge>
    ): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        nodes.forEach { counts[it.id] = 0 }
        edges.forEach { edge ->
            counts[edge.sourceId] = (counts[edge.sourceId] ?: 0) + 1
            counts[edge.targetId] = (counts[edge.targetId] ?: 0) + 1
        }
        return counts
    }

    /**
     * Detect Graph Anomalies (Fan-In, Fan-Out, Peel Chain, Mixer exposure).
     */
    fun detectGraphAnomalies(
        nodes: List<VisualInvestigationNode>,
        edges: List<VisualInvestigationEdge>
    ): List<GraphAnomalyIndicator> {
        val anomalies = mutableListOf<GraphAnomalyIndicator>()
        val inDegrees = mutableMapOf<String, Int>()
        val outDegrees = mutableMapOf<String, Int>()

        edges.forEach { edge ->
            outDegrees[edge.sourceId] = (outDegrees[edge.sourceId] ?: 0) + 1
            inDegrees[edge.targetId] = (inDegrees[edge.targetId] ?: 0) + 1
        }

        nodes.forEach { node ->
            val inD = inDegrees[node.id] ?: 0
            val outD = outDegrees[node.id] ?: 0

            // Fan-In / Aggregation Hub
            if (inD >= 4 && outD <= 2) {
                anomalies.add(
                    GraphAnomalyIndicator(
                        nodeId = node.id,
                        titleEn = "Fund Consolidation / Fan-In Hub",
                        titleFa = "مرکز تجمیع سرمایه (Fan-In)",
                        descriptionEn = "Node receives inflows from $inD distinct sources before consolidating funds.",
                        descriptionFa = "این گره ورودی‌های متعددی از $inD منبع مستقل دریافت کرده و تجمیع نموده است.",
                        anomalyType = AnomalyType.FAN_IN,
                        riskSeverity = RiskSeverity.HIGH
                    )
                )
            }

            // Fan-Out / Layering Dispenser
            if (outD >= 4 && inD <= 2) {
                anomalies.add(
                    GraphAnomalyIndicator(
                        nodeId = node.id,
                        titleEn = "Dispersion / Fan-Out Layering",
                        titleFa = "توزیع لایه‌بندی‌شده سرمایه (Fan-Out)",
                        descriptionEn = "Node rapidly splits inputs across $outD outbound counterparties.",
                        descriptionFa = "سرمایه دریافتی بلافاصله به $outD طرف مقابل مختلف خرد و پراکنده شده است.",
                        anomalyType = AnomalyType.FAN_OUT,
                        riskSeverity = RiskSeverity.HIGH
                    )
                )
            }

            // Mixer Exposure
            if (node.nodeType == VisualNodeType.MIXER || node.mixerExposure) {
                anomalies.add(
                    GraphAnomalyIndicator(
                        nodeId = node.id,
                        titleEn = "Mixer / Privacy Protocol Interaction",
                        titleFa = "تعامل مستقیم با میکسر یا کوین‌جوین",
                        descriptionEn = "Direct topological proximity to anonymizing tumbler infrastructure.",
                        descriptionFa = "مجاورت گرافیکی و تراکنشی مستقیم با پروتکل‌های اختلاط و گمنام‌سازی.",
                        anomalyType = AnomalyType.MIXER_EXPOSURE,
                        riskSeverity = RiskSeverity.CRITICAL
                    )
                )
            }

            // Sanctions Exposure
            if (node.sanctionsMatch) {
                anomalies.add(
                    GraphAnomalyIndicator(
                        nodeId = node.id,
                        titleEn = "Sanctions List Association",
                        titleFa = "تطابق با فهرست تحریم‌های بین‌المللی",
                        descriptionEn = "Identified on active OFAC / UN / EU sanctions registries.",
                        descriptionFa = "شناسه در فهرست‌های رسمی تحریم‌های بین‌المللی احراز شده است.",
                        anomalyType = AnomalyType.SANCTIONS_EXPOSURE,
                        riskSeverity = RiskSeverity.CRITICAL
                    )
                )
            }
        }

        return anomalies
    }
}
