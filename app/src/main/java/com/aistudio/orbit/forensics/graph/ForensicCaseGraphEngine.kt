package com.aistudio.orbit.forensics.graph

import com.aistudio.orbit.forensics.osint.InvestigationExecutionSession
import com.aistudio.orbit.forensics.osint.OsintAnalysisReport
import com.aistudio.orbit.forensics.osint.identity.Email2PhoneNumberPipeline.ReconstructionCandidate
import com.aistudio.orbit.model.*
import kotlin.math.*

/**
 * Filter mode for the interactive forensic graph visualizer.
 */
enum class EntityFilterMode(val displayNameEn: String, val displayNameFa: String) {
    ALL("All Entities", "همه موجودیت‌ها"),
    ON_CHAIN_ONLY("On-Chain Wallets", "کیف‌پول‌های آن‌چین"),
    OFF_CHAIN_OSINT_ONLY("Off-Chain OSINT", "ردپاهای آف‌چین (OSINT)"),
    HIGH_RISK_ONLY("High Risk & Sanctions", "ریسک بالا و تحریم")
}

/**
 * Unified Forensic Case Graph Engine combining GraphSense on-chain node-link concepts
 * with OpenSanctions/Yente sanctions matching, SpiderFoot OSINT correlation, and TagPacks entity resolution.
 */
object ForensicCaseGraphEngine {

    /**
     * Builds an interconnected interactive case graph containing on-chain transactions,
     * GraphSense TagPack clusters, OpenSanctions regulatory designations, SpiderFoot infrastructure,
     * and off-chain OSINT intelligence transforms.
     */
    fun buildCaseGraph(
        investigationCase: InvestigationCase,
        osintReport: OsintAnalysisReport? = null,
        deepIdentityCandidates: List<ReconstructionCandidate>? = null,
        osintSession: InvestigationExecutionSession? = null
    ): InteractiveCaseGraph {
        val nodesMap = mutableMapOf<String, InteractiveCaseNode>()
        val edgesList = mutableListOf<InteractiveCaseEdge>()

        val targetAddress = investigationCase.targetAddress
        val network = investigationCase.network

        // Check for sanctions match from OSINT Session or Risk Indicators
        val hasSanctionsMatch = osintSession?.extractedEntities?.any { it.isSanctioned } == true ||
                osintSession?.collectedEvidence?.any { it.sourceId.contains("OPENSANCTIONS") || it.sourceId.contains("OFAC") } == true ||
                investigationCase.riskIndicators.any { it.severity == RiskSeverity.CRITICAL }

        // Determine target overall risk
        val targetRisk = when {
            hasSanctionsMatch -> RiskSeverity.CRITICAL
            investigationCase.riskIndicators.any { it.severity == RiskSeverity.CRITICAL } -> RiskSeverity.CRITICAL
            investigationCase.riskIndicators.any { it.severity == RiskSeverity.HIGH } -> RiskSeverity.HIGH
            investigationCase.riskIndicators.any { it.severity == RiskSeverity.MEDIUM } -> RiskSeverity.MEDIUM
            else -> RiskSeverity.INFO
        }

        // 1. Add Target Node at Center
        val targetTags = mutableListOf("Primary Suspect Target", network.displayName)
        if (hasSanctionsMatch) {
            targetTags.add("OpenSanctions / OFAC Match")
        }

        val targetNode = InteractiveCaseNode(
            id = targetAddress,
            label = "Target: ${targetAddress.take(6)}...${targetAddress.takeLast(4)}",
            entityCategory = ForensicEntityCategory.CRYPTO_WALLET,
            network = network,
            riskSeverity = targetRisk,
            epistemicStatus = ForensicEpistemicStatus.UNDISPUTED_LEDGER_FACT,
            isSeed = true,
            isTarget = true,
            confidencePercent = 100,
            balanceDisplay = "${String.format("%.4f", investigationCase.balanceBtc)} ${network.symbol}",
            txCount = investigationCase.totalTransactionsFound,
            tags = targetTags,
            x = 0f,
            y = 0f,
            visualRadius = 34f
        )
        nodesMap[targetAddress] = targetNode

        // 2. Add On-Chain Counterparties (GraphSense style)
        val counterparties = investigationCase.counterparties.take(24)
        val angleStep = if (counterparties.isNotEmpty()) (2 * PI / counterparties.size) else 0.0

        counterparties.forEachIndexed { index, cp ->
            val cpAngle = index * angleStep
            val distance = 220f + (index % 3) * 40f
            val labelStr = cp.label ?: "CP: ${cp.address.take(6)}...${cp.address.takeLast(4)}"

            val category = when {
                cp.label?.contains("Binance", ignoreCase = true) == true ||
                cp.label?.contains("Exchange", ignoreCase = true) == true ||
                cp.label?.contains("Kraken", ignoreCase = true) == true ||
                cp.label?.contains("Kucoin", ignoreCase = true) == true -> ForensicEntityCategory.EXCHANGE_HOT_WALLET

                cp.label?.contains("Mixer", ignoreCase = true) == true ||
                cp.label?.contains("Tornado", ignoreCase = true) == true ||
                cp.label?.contains("Wasabi", ignoreCase = true) == true -> ForensicEntityCategory.MIXER_OR_TUMBLER

                cp.address.startsWith("0x") && cp.address.length == 42 && (index % 5 == 0) -> ForensicEntityCategory.SMART_CONTRACT
                else -> ForensicEntityCategory.CRYPTO_WALLET
            }

            val cpRisk = when (category) {
                ForensicEntityCategory.MIXER_OR_TUMBLER -> RiskSeverity.CRITICAL
                ForensicEntityCategory.EXCHANGE_HOT_WALLET -> RiskSeverity.MEDIUM
                else -> if (cp.totalReceivedBtc > 5.0) RiskSeverity.HIGH else RiskSeverity.INFO
            }

            val cpNode = InteractiveCaseNode(
                id = cp.address,
                label = labelStr,
                entityCategory = category,
                network = network,
                riskSeverity = cpRisk,
                epistemicStatus = ForensicEpistemicStatus.UNDISPUTED_LEDGER_FACT,
                isSeed = false,
                isTarget = false,
                confidencePercent = 100,
                balanceDisplay = "${String.format("%.3f", cp.totalReceivedBtc)} ${network.symbol}",
                txCount = cp.txCount,
                inDegree = if (cp.totalSentBtc > 0) 1 else 0,
                outDegree = if (cp.totalReceivedBtc > 0) 1 else 0,
                tags = listOfNotNull(cp.label, "${cp.txCount} txs"),
                x = (cos(cpAngle) * distance).toFloat(),
                y = (sin(cpAngle) * distance).toFloat(),
                visualRadius = (20f + (cp.txCount.coerceAtMost(10) * 1.5f))
            )
            nodesMap[cp.address] = cpNode

            // Add Edge from/to Target
            val isIncoming = cp.totalSentBtc > cp.totalReceivedBtc
            val edgeId = "edge_onchain_${index}_${targetAddress.take(4)}_${cp.address.take(4)}"
            val netVol = cp.netVolumeBtc.absoluteValue

            edgesList.add(
                InteractiveCaseEdge(
                    id = edgeId,
                    sourceId = if (isIncoming) cp.address else targetAddress,
                    targetId = if (isIncoming) targetAddress else cp.address,
                    category = ForensicEdgeCategory.ON_CHAIN_TRANSFER,
                    volumeDisplay = "${String.format("%.3f", netVol)} ${network.symbol}",
                    txCount = cp.txCount,
                    confidencePercent = 100,
                    transformLabel = if (isIncoming) "Incoming Transfer" else "Outgoing Transfer",
                    strokeWidth = (2.0f + (netVol.toFloat().coerceAtMost(6.0f) * 0.8f))
                )
            )
        }

        // 3. Add Off-Chain OSINT Entities & Transforms (Maltego / Epieos / SpiderFoot style)
        val osintBaseDist = 340f
        var osintIndex = 0

        // 3a. IP Address exposures from SpiderFoot / network metadata
        osintReport?.ipExposures?.take(4)?.forEach { ip ->
            val ipAngle = PI / 4 + (osintIndex * 0.45)
            val ipNodeId = "IP_${ip.ipAddress}"
            val ipNode = InteractiveCaseNode(
                id = ipNodeId,
                label = "IP: ${ip.ipAddress}",
                entityCategory = ForensicEntityCategory.IP_NETWORK_NODE,
                network = network,
                riskSeverity = if (ip.torOrVpnDetected) RiskSeverity.HIGH else RiskSeverity.INFO,
                epistemicStatus = ForensicEpistemicStatus.DERIVED_OSINT_INFERENCE,
                confidencePercent = 88,
                tags = listOfNotNull(
                    if (ip.torOrVpnDetected) "Tor/VPN Exit" else "Direct ISP",
                    ip.ispName,
                    ip.country
                ),
                metadata = mapOf(
                    "ASN" to ip.asn,
                    "Country" to ip.country,
                    "City" to ip.city,
                    "ISP" to ip.ispName
                ),
                x = (cos(ipAngle) * (osintBaseDist + 50f)).toFloat(),
                y = (sin(ipAngle) * (osintBaseDist + 50f)).toFloat(),
                visualRadius = 22f
            )
            nodesMap[ipNodeId] = ipNode

            edgesList.add(
                InteractiveCaseEdge(
                    id = "edge_transform_ip_${osintIndex}",
                    sourceId = targetAddress,
                    targetId = ipNodeId,
                    category = ForensicEdgeCategory.NETWORK_PROPAGATION,
                    transformLabel = if (ip.torOrVpnDetected) "Tor Relay Broadcast" else "P2P Node Broadcast",
                    confidencePercent = 88,
                    strokeWidth = 2.0f
                )
            )
            osintIndex++
        }

        // 3b. Leaked emails & accounts (Holehe / GHunt / Epieos)
        var primaryEmailNodeId: String? = null
        osintReport?.leakRecords?.take(3)?.forEach { leak ->
            val emailAngle = 3 * PI / 4 + (osintIndex * 0.45)
            val emailId = "EMAIL_${leak.emailAssociated}"
            primaryEmailNodeId = emailId

            val leakConf = (if (leak.confidenceScore <= 1.0f) leak.confidenceScore * 100 else leak.confidenceScore).toInt().coerceIn(1, 100)
            val emailNode = InteractiveCaseNode(
                id = emailId,
                label = "Email: ${leak.emailAssociated}",
                entityCategory = ForensicEntityCategory.EMAIL_ADDRESS,
                network = network,
                riskSeverity = RiskSeverity.MEDIUM,
                epistemicStatus = ForensicEpistemicStatus.DERIVED_OSINT_INFERENCE,
                confidencePercent = leakConf,
                tags = listOf("Breach: ${leak.source}", "Holehe Registered"),
                metadata = mapOf(
                    "Source" to leak.source,
                    "Details" to leak.dataDetailsEn
                ),
                x = (cos(emailAngle) * osintBaseDist).toFloat(),
                y = (sin(emailAngle) * osintBaseDist).toFloat(),
                visualRadius = 24f
            )
            nodesMap[emailId] = emailNode

            edgesList.add(
                InteractiveCaseEdge(
                    id = "edge_transform_email_${osintIndex}",
                    sourceId = targetAddress,
                    targetId = emailId,
                    category = ForensicEdgeCategory.TRANSFORM_ATTRIBUTION,
                    transformLabel = "Attributed via Holehe ($leakConf%)",
                    confidencePercent = leakConf,
                    strokeWidth = 2.2f
                )
            )
            osintIndex++
        }

        // 3c. Reconstructed Phone Numbers (email2phonenumber / Ignorant / PhoneInfoga)
        deepIdentityCandidates?.take(2)?.forEachIndexed { idx, candidate ->
            val phoneAngle = 5 * PI / 4 + (idx * 0.45)
            val phoneId = "PHONE_${candidate.candidateE164}"
            val candConf = (if (candidate.matchConfidence <= 1.0f) candidate.matchConfidence * 100 else candidate.matchConfidence).toInt().coerceIn(1, 100)

            val phoneNode = InteractiveCaseNode(
                id = phoneId,
                label = "Phone: ${candidate.candidateE164}",
                entityCategory = ForensicEntityCategory.PHONE_NUMBER,
                network = network,
                riskSeverity = RiskSeverity.HIGH,
                epistemicStatus = ForensicEpistemicStatus.DERIVED_OSINT_INFERENCE,
                confidencePercent = candConf,
                tags = listOf(
                    "Carrier: ${candidate.carrier}",
                    "Type: ${candidate.lineType}",
                    "email2phonenumber"
                ),
                metadata = mapOf(
                    "Carrier" to candidate.carrier,
                    "LineType" to candidate.lineType,
                    "Verification" to candidate.verificationDetails
                ),
                x = (cos(phoneAngle) * (osintBaseDist + 70f)).toFloat(),
                y = (sin(phoneAngle) * (osintBaseDist + 70f)).toFloat(),
                visualRadius = 26f
            )
            nodesMap[phoneId] = phoneNode

            // Transform link from Email to Phone if email exists, otherwise from target
            val sourceId = primaryEmailNodeId ?: targetAddress
            edgesList.add(
                InteractiveCaseEdge(
                    id = "edge_transform_phone_$idx",
                    sourceId = sourceId,
                    targetId = phoneId,
                    category = ForensicEdgeCategory.TRANSFORM_ATTRIBUTION,
                    transformLabel = "email2phonenumber ($candConf%)",
                    confidencePercent = candConf,
                    strokeWidth = 2.4f
                )
            )
        }

        // 3d. Correlate Entities from Active OSINT Pipeline Session (OpenSanctions, TagPacks, SpiderFoot)
        osintSession?.let { session ->
            // OpenSanctions & Regulatory Matches
            val sanctionedEntities = session.extractedEntities.filter { it.isSanctioned }
            sanctionedEntities.forEachIndexed { sIdx, sent ->
                val sAngle = -PI / 3 + (sIdx * 0.4)
                val sNodeId = "SANCTIONS_${sent.entityId}"
                val sNode = InteractiveCaseNode(
                    id = sNodeId,
                    label = "Sanctions: ${sent.name.take(24)}",
                    entityCategory = ForensicEntityCategory.CLUSTER,
                    network = network,
                    riskSeverity = RiskSeverity.CRITICAL,
                    epistemicStatus = ForensicEpistemicStatus.UNDISPUTED_LEDGER_FACT,
                    confidencePercent = sent.confidence,
                    tags = listOf("OpenSanctions Match", "OFAC/EU Watchlist", sent.entityClass.displayNameEn),
                    metadata = mapOf(
                        "Entity Name" to sent.name,
                        "Entity Name FA" to sent.nameFa,
                        "Evidence" to sent.primaryEvidenceId,
                        "Details" to sent.summaryDetailsEn
                    ),
                    x = (cos(sAngle) * (osintBaseDist + 110f)).toFloat(),
                    y = (sin(sAngle) * (osintBaseDist + 110f)).toFloat(),
                    visualRadius = 28f
                )
                nodesMap[sNodeId] = sNode

                edgesList.add(
                    InteractiveCaseEdge(
                        id = "edge_sanctions_match_$sIdx",
                        sourceId = targetAddress,
                        targetId = sNodeId,
                        category = ForensicEdgeCategory.THREAT_CORRELATION,
                        transformLabel = "OpenSanctions (${sent.confidence}%)",
                        confidencePercent = sent.confidence,
                        strokeWidth = 3.0f
                    )
                )
            }

            // SpiderFoot DNS / Host Infrastructure
            session.collectedEvidence.filter { it.providerModule == "sfp_dnsresolve" }.take(3).forEachIndexed { dnsIdx, dnsEv ->
                dnsEv.extractedEntities.forEachIndexed { ipIdx, resolvedIp ->
                    val dnsNodeId = "DNS_IP_$resolvedIp"
                    if (!nodesMap.containsKey(dnsNodeId)) {
                        val dnsAngle = PI / 6 + (dnsIdx * 0.3) + (ipIdx * 0.2)
                        val dnsNode = InteractiveCaseNode(
                            id = dnsNodeId,
                            label = "Host: $resolvedIp",
                            entityCategory = ForensicEntityCategory.IP_NETWORK_NODE,
                            network = network,
                            riskSeverity = RiskSeverity.INFO,
                            epistemicStatus = ForensicEpistemicStatus.UNDISPUTED_LEDGER_FACT,
                            confidencePercent = dnsEv.numericConfidence,
                            tags = listOf("SpiderFoot DNS", "Google DoH"),
                            metadata = mapOf(
                                "Target" to dnsEv.queryTarget,
                                "Provider" to dnsEv.providerName,
                                "Evidence ID" to dnsEv.evidenceId
                            ),
                            x = (cos(dnsAngle) * (osintBaseDist + 40f)).toFloat(),
                            y = (sin(dnsAngle) * (osintBaseDist + 40f)).toFloat(),
                            visualRadius = 20f
                        )
                        nodesMap[dnsNodeId] = dnsNode

                        edgesList.add(
                            InteractiveCaseEdge(
                                id = "edge_spiderfoot_dns_${dnsIdx}_$ipIdx",
                                sourceId = targetAddress,
                                targetId = dnsNodeId,
                                category = ForensicEdgeCategory.NETWORK_PROPAGATION,
                                transformLabel = "SpiderFoot DNS (${dnsEv.numericConfidence}%)",
                                confidencePercent = dnsEv.numericConfidence,
                                strokeWidth = 2.0f
                            )
                        )
                    }
                }
            }

            // GraphSense TagPack VASP Clusters
            val vaspEntities = session.extractedEntities.filter { it.entityClass == ForensicEntityClass.EXCHANGE || it.entityClass == ForensicEntityClass.VASP }
            vaspEntities.forEachIndexed { vIdx, vent ->
                val vAngle = -2 * PI / 3 + (vIdx * 0.5)
                val vNodeId = "VASP_CLUSTER_${vent.entityId}"
                if (!nodesMap.containsKey(vNodeId)) {
                    val vNode = InteractiveCaseNode(
                        id = vNodeId,
                        label = "VASP: ${vent.name}",
                        entityCategory = ForensicEntityCategory.EXCHANGE_HOT_WALLET,
                        network = network,
                        riskSeverity = RiskSeverity.MEDIUM,
                        epistemicStatus = ForensicEpistemicStatus.ALGORITHMIC_CALCULATION,
                        confidencePercent = vent.confidence,
                        tags = listOf("GraphSense TagPack", "VASP Attribution"),
                        metadata = mapOf(
                            "VASP Name" to vent.name,
                            "Control Relation" to vent.controlRelation.name,
                            "Evidence" to vent.primaryEvidenceId
                        ),
                        x = (cos(vAngle) * (osintBaseDist + 60f)).toFloat(),
                        y = (sin(vAngle) * (osintBaseDist + 60f)).toFloat(),
                        visualRadius = 26f
                    )
                    nodesMap[vNodeId] = vNode

                    edgesList.add(
                        InteractiveCaseEdge(
                            id = "edge_vasp_attribution_$vIdx",
                            sourceId = targetAddress,
                            targetId = vNodeId,
                            category = ForensicEdgeCategory.CO_SPEND_CLUSTER,
                            transformLabel = "GraphSense TagPack (${vent.confidence}%)",
                            confidencePercent = vent.confidence,
                            strokeWidth = 2.5f
                        )
                    )
                }
            }
        }

        // 4. Run Force-Directed Relaxation to normalize spacing
        relaxLayout(nodesMap.values.toList(), edgesList, iterations = 35)

        return InteractiveCaseGraph(
            caseId = investigationCase.id,
            targetAddress = targetAddress,
            nodes = nodesMap.values.toList(),
            edges = edgesList
        )
    }

    /**
     * Executes a spring-repulsion force-directed layout step to prevent overlapping nodes.
     */
    fun relaxLayout(nodes: List<InteractiveCaseNode>, edges: List<InteractiveCaseEdge>, iterations: Int = 20) {
        val k = 160f // Preferred distance
        val repulsion = 4800f
        val attraction = 0.045f

        for (iter in 0 until iterations) {
            // Node-Node Repulsion
            for (i in nodes.indices) {
                val n1 = nodes[i]
                if (n1.isTarget) continue // Anchor target at center

                for (j in (i + 1) until nodes.size) {
                    val n2 = nodes[j]
                    val dx = n1.x - n2.x
                    val dy = n1.y - n2.y
                    val dist = max(18f, sqrt(dx * dx + dy * dy))

                    val force = repulsion / (dist * dist)
                    val fx = (dx / dist) * force
                    val fy = (dy / dist) * force

                    n1.x += fx
                    n1.y += fy
                    if (!n2.isTarget) {
                        n2.x -= fx
                        n2.y -= fy
                    }
                }
            }

            // Edge Attraction
            for (edge in edges) {
                val src = nodes.find { it.id == edge.sourceId } ?: continue
                val tgt = nodes.find { it.id == edge.targetId } ?: continue

                val dx = tgt.x - src.x
                val dy = tgt.y - src.y
                val dist = max(18f, sqrt(dx * dx + dy * dy))
                val displacement = dist - k

                val fx = (dx / dist) * displacement * attraction
                val fy = (dy / dist) * displacement * attraction

                if (!src.isTarget) {
                    src.x += fx
                    src.y += fy
                }
                if (!tgt.isTarget) {
                    tgt.x -= fx
                    tgt.y -= fy
                }
            }

            // Gravity towards center
            for (node in nodes) {
                if (node.isTarget) continue
                node.x *= 0.985f
                node.y *= 0.985f
            }
        }
    }

    /**
     * Filters graph nodes and edges according to the investigator's analytical focus.
     */
    fun filterGraph(graph: InteractiveCaseGraph, filterMode: EntityFilterMode): InteractiveCaseGraph {
        val filteredNodes = when (filterMode) {
            EntityFilterMode.ALL -> graph.nodes
            EntityFilterMode.ON_CHAIN_ONLY -> graph.nodes.filter { it.isOnChain }
            EntityFilterMode.OFF_CHAIN_OSINT_ONLY -> graph.nodes.filter { it.isOffChain || it.isTarget }
            EntityFilterMode.HIGH_RISK_ONLY -> graph.nodes.filter {
                it.isTarget || it.riskSeverity in listOf(RiskSeverity.HIGH, RiskSeverity.CRITICAL)
            }
        }

        val nodeIds = filteredNodes.map { it.id }.toSet()
        val filteredEdges = graph.edges.filter { it.sourceId in nodeIds && it.targetId in nodeIds }

        return graph.copy(nodes = filteredNodes, edges = filteredEdges)
    }

    /**
     * Finds shortest path between two entities using Breadth-First Search.
     */
    fun findShortestPath(graph: InteractiveCaseGraph, startId: String, targetId: String): List<String> {
        if (startId == targetId) return listOf(startId)

        val adjacency = mutableMapOf<String, MutableList<String>>()
        graph.edges.forEach { edge ->
            adjacency.getOrPut(edge.sourceId) { mutableListOf() }.add(edge.targetId)
            adjacency.getOrPut(edge.targetId) { mutableListOf() }.add(edge.sourceId)
        }

        val queue = ArrayDeque<String>()
        val visited = mutableSetOf<String>()
        val parentMap = mutableMapOf<String, String>()

        queue.add(startId)
        visited.add(startId)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == targetId) {
                val path = mutableListOf<String>()
                var curr: String? = targetId
                while (curr != null) {
                    path.add(curr)
                    curr = parentMap[curr]
                }
                return path.reversed()
            }

            adjacency[current]?.forEach { neighbor ->
                if (neighbor !in visited) {
                    visited.add(neighbor)
                    parentMap[neighbor] = current
                    queue.add(neighbor)
                }
            }
        }

        return emptyList()
    }

    /**
     * Traces directed multi-hop fund flows starting from a specific node up to maxHops.
     */
    fun traceFundFlow(graph: InteractiveCaseGraph, startId: String, maxHops: Int = 3): List<List<String>> {
        val results = mutableListOf<List<String>>()
        val adjacency = mutableMapOf<String, MutableList<String>>()

        graph.edges.filter { it.category == ForensicEdgeCategory.ON_CHAIN_TRANSFER }.forEach { edge ->
            adjacency.getOrPut(edge.sourceId) { mutableListOf() }.add(edge.targetId)
        }

        fun dfs(current: String, path: List<String>, hopsLeft: Int) {
            if (hopsLeft == 0) {
                if (path.size > 1) results.add(path)
                return
            }
            val neighbors = adjacency[current] ?: emptyList()
            if (neighbors.isEmpty()) {
                if (path.size > 1) results.add(path)
                return
            }
            for (nbr in neighbors) {
                if (nbr !in path) {
                    dfs(nbr, path + nbr, hopsLeft - 1)
                }
            }
        }

        dfs(startId, listOf(startId), maxHops)
        return results.take(10)
    }

    /**
     * Detects peel-chain laundering structures in the graph.
     */
    fun detectPeelChains(graph: InteractiveCaseGraph): List<List<String>> {
        val outEdges = graph.edges
            .filter { it.category == ForensicEdgeCategory.ON_CHAIN_TRANSFER }
            .groupBy { it.sourceId }

        val chains = mutableListOf<List<String>>()
        for ((nodeId, edges) in outEdges) {
            // Peel chains typically branch into 2 outputs: one small peel, one large change address
            if (edges.size == 2) {
                val chain = mutableListOf(nodeId)
                edges.forEach { chain.add(it.targetId) }
                chains.add(chain)
            }
        }
        return chains.take(8)
    }

    /**
     * Detects circular fund loops / cycle transfers.
     */
    fun detectCycles(graph: InteractiveCaseGraph): List<List<String>> {
        val adjacency = mutableMapOf<String, MutableList<String>>()
        graph.edges.filter { it.category == ForensicEdgeCategory.ON_CHAIN_TRANSFER }.forEach { edge ->
            adjacency.getOrPut(edge.sourceId) { mutableListOf() }.add(edge.targetId)
        }

        val cycles = mutableListOf<List<String>>()
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()

        fun dfs(current: String, path: List<String>) {
            visited.add(current)
            recursionStack.add(current)

            for (neighbor in adjacency[current] ?: emptyList()) {
                if (neighbor in recursionStack) {
                    val cycleStartIdx = path.indexOf(neighbor)
                    if (cycleStartIdx != -1) {
                        val cycle = path.subList(cycleStartIdx, path.size) + neighbor
                        if (cycle.size > 2) cycles.add(cycle)
                    }
                } else if (neighbor !in visited) {
                    dfs(neighbor, path + neighbor)
                }
            }
            recursionStack.remove(current)
        }

        for (node in graph.nodes) {
            if (node.id !in visited) {
                dfs(node.id, listOf(node.id))
            }
        }

        return cycles.distinctBy { it.sorted() }.take(6)
    }

    /**
     * Builds standard VisualInvestigationNode and VisualInvestigationEdge datasets
     * for the Biyena multi-mode visualization engine.
     */
    fun buildVisualInvestigationDataset(
        investigationCase: InvestigationCase,
        osintReport: OsintAnalysisReport? = null
    ): Pair<List<VisualInvestigationNode>, List<VisualInvestigationEdge>> {
        val interactiveGraph = buildCaseGraph(investigationCase, osintReport)

        val visualNodes = interactiveGraph.nodes.map { node ->
            val vType = when (node.entityCategory) {
                ForensicEntityCategory.EXCHANGE_HOT_WALLET, ForensicEntityCategory.EXCHANGE -> VisualNodeType.EXCHANGE
                ForensicEntityCategory.MIXER_OR_TUMBLER -> VisualNodeType.MIXER
                ForensicEntityCategory.MINING_POOL -> VisualNodeType.MINING_POOL
                ForensicEntityCategory.SMART_CONTRACT, ForensicEntityCategory.CONTRACT -> VisualNodeType.TRANSACTION
                ForensicEntityCategory.PHONE_NUMBER, ForensicEntityCategory.PUBLIC_PHONE -> VisualNodeType.PERSON
                ForensicEntityCategory.EMAIL_ADDRESS, ForensicEntityCategory.PUBLIC_EMAIL -> VisualNodeType.EMAIL
                ForensicEntityCategory.IP_NETWORK_NODE, ForensicEntityCategory.PUBLIC_IP -> VisualNodeType.IP
                ForensicEntityCategory.SOCIAL_ACCOUNT, ForensicEntityCategory.PUBLIC_PROFILE -> VisualNodeType.ENTITY
                ForensicEntityCategory.ALIAS_USERNAME, ForensicEntityCategory.PUBLIC_USERNAME -> VisualNodeType.USERNAME
                ForensicEntityCategory.DOMAIN_NAME, ForensicEntityCategory.DOMAIN -> VisualNodeType.DOMAIN
                ForensicEntityCategory.CLUSTER -> VisualNodeType.CLUSTER
                ForensicEntityCategory.EVIDENCE -> VisualNodeType.EVIDENCE
                else -> VisualNodeType.ADDRESS
            }

            val vConf = when (node.epistemicStatus) {
                ForensicEpistemicStatus.UNDISPUTED_LEDGER_FACT -> VisualConfidence.CONFIRMED
                ForensicEpistemicStatus.ALGORITHMIC_CALCULATION -> VisualConfidence.HIGH
                ForensicEpistemicStatus.DERIVED_OSINT_INFERENCE -> VisualConfidence.MEDIUM
                ForensicEpistemicStatus.WORKING_HYPOTHESIS -> VisualConfidence.LOW
            }

            VisualInvestigationNode(
                id = node.id,
                label = node.label,
                nodeType = vType,
                primaryIdentifier = node.id,
                secondaryIdentifier = node.tags.firstOrNull() ?: "",
                network = node.network,
                riskSeverity = node.riskSeverity,
                confidence = vConf,
                isSeed = node.isSeed,
                isTarget = node.isTarget,
                sourceCount = if (node.isOnChain) 1 else 2,
                evidenceCount = if (node.isTarget) 3 else 1,
                evidenceIds = if (node.isTarget) listOf("EVD-SEED-01", "EVD-FACT-02") else listOf("EVD-NODE-${node.id.take(4)}"),
                balanceDisplay = node.balanceDisplay,
                volumeBtc = 0.0,
                txCount = node.txCount,
                inDegree = node.inDegree,
                outDegree = node.outDegree,
                sanctionsMatch = node.riskSeverity == RiskSeverity.CRITICAL,
                mixerExposure = node.entityCategory == ForensicEntityCategory.MIXER_OR_TUMBLER,
                tags = node.tags,
                metadata = node.metadata,
                x = node.x,
                y = node.y,
                vx = node.vx,
                vy = node.vy,
                radius = node.visualRadius
            )
        }

        val visualEdges = interactiveGraph.edges.map { edge ->
            val vRel = when (edge.category) {
                ForensicEdgeCategory.ON_CHAIN_TRANSFER -> VisualEdgeType.SENT
                ForensicEdgeCategory.CO_SPEND_CLUSTER -> VisualEdgeType.BELONGS_TO_CLUSTER
                ForensicEdgeCategory.TRANSFORM_ATTRIBUTION -> VisualEdgeType.ATTRIBUTED_TO
                ForensicEdgeCategory.THREAT_CORRELATION -> VisualEdgeType.ASSOCIATED_WITH
                ForensicEdgeCategory.NETWORK_PROPAGATION -> VisualEdgeType.HOSTED_BY
                else -> VisualEdgeType.SENT
            }

            val epistemic = when (edge.category) {
                ForensicEdgeCategory.ON_CHAIN_TRANSFER -> EdgeEpistemicStyle.FACT
                ForensicEdgeCategory.CO_SPEND_CLUSTER -> EdgeEpistemicStyle.DERIVED
                ForensicEdgeCategory.TRANSFORM_ATTRIBUTION -> EdgeEpistemicStyle.INFERENCE
                ForensicEdgeCategory.THREAT_CORRELATION -> EdgeEpistemicStyle.HYPOTHESIS
                else -> EdgeEpistemicStyle.FACT
            }

            val conf = when (edge.confidencePercent) {
                100 -> VisualConfidence.CONFIRMED
                in 80..99 -> VisualConfidence.HIGH
                in 50..79 -> VisualConfidence.MEDIUM
                else -> VisualConfidence.LOW
            }

            VisualInvestigationEdge(
                id = edge.id,
                sourceId = edge.sourceId,
                targetId = edge.targetId,
                relationshipType = vRel,
                epistemicStyle = epistemic,
                direction = edge.isDirected,
                amountDisplay = edge.volumeDisplay,
                amountSat = edge.volumeSat,
                confidence = conf,
                source = if (edge.category == ForensicEdgeCategory.ON_CHAIN_TRANSFER) "Bitcoin Core RPC" else "OSINT Engine",
                evidenceIds = listOf("EVD-EDGE-${edge.id.take(4)}"),
                strokeWidth = edge.strokeWidth,
                transformLabel = edge.transformLabel
            )
        }

        return Pair(visualNodes, visualEdges)
    }
}
