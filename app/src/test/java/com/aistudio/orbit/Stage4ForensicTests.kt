package com.aistudio.orbit

import com.aistudio.orbit.forensics.classification.EntityClassifier
import com.aistudio.orbit.forensics.clustering.ClusteringEngine
import com.aistudio.orbit.forensics.clustering.UnionFind
import com.aistudio.orbit.forensics.graph.ForensicEdge
import com.aistudio.orbit.forensics.graph.ForensicNode
import com.aistudio.orbit.forensics.graph.GraphEngine
import com.aistudio.orbit.forensics.relationship.RelationshipAnalyzer
import com.aistudio.orbit.forensics.temporal.GeographicTimeEngine
import com.aistudio.orbit.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage4ForensicTests {

    private val targetAddress = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
    private val addrA = "3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy"
    private val addrB = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"
    private val addrC = "1BoatSLRHtKNngkdXEeobR76b53LETtpyT"
    private val addrD = "34xp4vRoCGJym3xR7yCVPFHoCNxv4Twseo"

    // ----------------------------------------------------
    // 1. CLUSTERING ENGINE (CIOH & COINJOIN) TESTS
    // ----------------------------------------------------

    @Test
    fun testUnionFindDataStructure() {
        val uf = UnionFind()
        uf.union("addr1", "addr2")
        uf.union("addr2", "addr3")
        uf.union("addr4", "addr5")

        assertEquals("addr1, addr2, addr3 should share the same root", uf.find("addr1"), uf.find("addr3"))
        assertEquals("addr4, addr5 should share the same root", uf.find("addr4"), uf.find("addr5"))
        assertTrue("addr1 and addr4 must belong to different disjoint sets", uf.find("addr1") != uf.find("addr4"))
    }

    @Test
    fun testCIOHClustering() {
        // Create transactions where addrA and addrB co-spend inputs
        val tx1 = ForensicTransaction(
            txId = "tx_cioh_1",
            timestamp = 1700000000L,
            inputs = listOf(
                TxInput(prevOutAddress = addrA, prevOutValueSat = 50_000_000L),
                TxInput(prevOutAddress = addrB, prevOutValueSat = 50_000_000L)
            ),
            outputs = listOf(
                TxOutput(address = targetAddress, valueSat = 99_000_000L)
            )
        )

        val tx2 = ForensicTransaction(
            txId = "tx_cioh_2",
            timestamp = 1700086400L,
            inputs = listOf(
                TxInput(prevOutAddress = addrB, prevOutValueSat = 30_000_000L),
                TxInput(prevOutAddress = addrC, prevOutValueSat = 20_000_000L)
            ),
            outputs = listOf(
                TxOutput(address = targetAddress, valueSat = 49_000_000L)
            )
        )

        val clusters = ClusteringEngine.clusterCIOH(listOf(tx1, tx2))
        assertEquals("Should form 1 unified cluster from co-spending inputs", 1, clusters.size)
        val cluster = clusters[0]
        assertEquals(3, cluster.memberAddresses.size)
        assertTrue(cluster.memberAddresses.contains(addrA))
        assertTrue(cluster.memberAddresses.contains(addrB))
        assertTrue(cluster.memberAddresses.contains(addrC))
        assertTrue("Confidence score must be > 0", cluster.confidenceScore > 0f)
    }

    @Test
    fun testCoinJoinDetection() {
        // Equal output values signature
        val coinJoinTx = ForensicTransaction(
            txId = "tx_coinjoin",
            timestamp = 1700000000L,
            inputs = listOf(
                TxInput(prevOutAddress = addrA, prevOutValueSat = 10_050_000L),
                TxInput(prevOutAddress = addrB, prevOutValueSat = 10_060_000L),
                TxInput(prevOutAddress = addrC, prevOutValueSat = 10_070_000L)
            ),
            outputs = listOf(
                TxOutput(address = "mix_out_1", valueSat = 10_000_000L),
                TxOutput(address = "mix_out_2", valueSat = 10_000_000L),
                TxOutput(address = "mix_out_3", valueSat = 10_000_000L),
                TxOutput(address = "change_1", valueSat = 45_000L),
                TxOutput(address = "change_2", valueSat = 55_000L)
            )
        )

        val isCJ = ClusteringEngine.isCoinJoinTransaction(coinJoinTx)
        assertTrue("Transaction matching 3+ equal outputs and 3+ inputs must be recognized as CoinJoin", isCJ)

        val standardTx = ForensicTransaction(
            txId = "tx_normal",
            timestamp = 1700000000L,
            inputs = listOf(TxInput(prevOutAddress = addrA, prevOutValueSat = 10_000_000L)),
            outputs = listOf(
                TxOutput(address = addrB, valueSat = 8_000_000L),
                TxOutput(address = addrA, valueSat = 1_900_000L)
            )
        )
        assertTrue("Standard transaction must NOT be flagged as CoinJoin", !ClusteringEngine.isCoinJoinTransaction(standardTx))
    }

    // ----------------------------------------------------
    // 2. GRAPH ENGINE TESTS
    // ----------------------------------------------------

    @Test
    fun testGraphEngineShortestPathAndTraversal() {
        val engine = GraphEngine()
        engine.addNode(ForensicNode(id = "N1", label = "Node 1"))
        engine.addNode(ForensicNode(id = "N2", label = "Node 2"))
        engine.addNode(ForensicNode(id = "N3", label = "Node 3"))
        engine.addNode(ForensicNode(id = "N4", label = "Node 4"))

        engine.addEdge(ForensicEdge(id = "E1", sourceId = "N1", targetId = "N2"))
        engine.addEdge(ForensicEdge(id = "E2", sourceId = "N2", targetId = "N3"))
        engine.addEdge(ForensicEdge(id = "E3", sourceId = "N3", targetId = "N4"))
        engine.addEdge(ForensicEdge(id = "E4", sourceId = "N1", targetId = "N4")) // Direct shortcut

        val path = engine.findShortestPath("N1", "N4")
        assertEquals(listOf("N1", "N4"), path)

        val neighborsOfN1 = engine.getNeighbors("N1")
        assertEquals(2, neighborsOfN1.size)
    }

    @Test
    fun testGraphFlowTracingAndCycles() {
        val engine = GraphEngine()
        engine.addNode(ForensicNode(id = "A", label = "A"))
        engine.addNode(ForensicNode(id = "B", label = "B"))
        engine.addNode(ForensicNode(id = "C", label = "C"))
        engine.addNode(ForensicNode(id = "D", label = "D"))

        engine.addEdge(ForensicEdge(id = "e1", sourceId = "A", targetId = "B", totalVolumeSat = 1000L))
        engine.addEdge(ForensicEdge(id = "e2", sourceId = "B", targetId = "C", totalVolumeSat = 1000L))
        engine.addEdge(ForensicEdge(id = "e3", sourceId = "C", targetId = "A", totalVolumeSat = 1000L)) // Cycle back to A

        val traces = engine.traceFundFlow("A", maxHops = 3)
        assertTrue("Should trace multi-hop fund flow paths", traces.isNotEmpty())

        val cycles = engine.detectCycles()
        assertTrue("Should detect circular fund cycle A -> B -> C -> A", cycles.isNotEmpty())

        val centralities = engine.calculateCentralityScores()
        assertEquals(2, centralities["A"]) // In from C, Out to B
        assertEquals(2, centralities["B"]) // In from A, Out to C
        assertEquals(2, centralities["C"]) // In from B, Out to A
    }

    @Test
    fun testPeelChainDetection() {
        val engine = GraphEngine()
        engine.addNode(ForensicNode(id = "PeelSeed", label = "Seed"))
        engine.addNode(ForensicNode(id = "SmallHop", label = "Small Hop"))
        engine.addNode(ForensicNode(id = "ChangeHop", label = "Change Hop"))

        // Asymmetric peeling: 10% peeled off, 90% change
        engine.addEdge(ForensicEdge(id = "peel_1", sourceId = "PeelSeed", targetId = "SmallHop", totalVolumeSat = 10_000_000L))
        engine.addEdge(ForensicEdge(id = "peel_2", sourceId = "PeelSeed", targetId = "ChangeHop", totalVolumeSat = 90_000_000L))

        val peelChains = engine.detectPeelChains()
        assertEquals("Should detect peel chain pattern", 1, peelChains.size)
        assertEquals("PeelSeed", peelChains[0].seedAddress)
    }

    // ----------------------------------------------------
    // 3. BEHAVIORAL CLASSIFICATION TESTS
    // ----------------------------------------------------

    @Test
    fun testBehavioralClassificationHeuristics() {
        // High volume Exchange Hot Wallet
        val exchangeClassification = EntityClassifier.classifyAddress(
            address = addrD,
            network = BlockchainNetwork.BITCOIN,
            txCount = 1200,
            counterpartyCount = 450,
            hasConsolidation = false,
            hasFanOut = false
        )
        assertEquals(EntityClassificationType.EXCHANGE_HOT_WALLET, exchangeClassification.classification)
        assertEquals(ConfidenceLevel.MEDIUM_CONFIDENCE, exchangeClassification.confidence)

        // Mining Pool Payout
        val miningClassification = EntityClassifier.classifyAddress(
            address = addrA,
            network = BlockchainNetwork.BITCOIN,
            txCount = 100,
            counterpartyCount = 45,
            hasConsolidation = false,
            hasFanOut = true
        )
        assertEquals(EntityClassificationType.MINING_POOL, miningClassification.classification)

        // Aggregation / Sweep address
        val sweepClassification = EntityClassifier.classifyAddress(
            address = addrB,
            network = BlockchainNetwork.BITCOIN,
            txCount = 30,
            counterpartyCount = 15,
            hasConsolidation = true,
            hasFanOut = false
        )
        assertEquals(EntityClassificationType.MERCHANT_PROCESSOR, sweepClassification.classification)

        // Standard Unhosted Wallet
        val unhostedClassification = EntityClassifier.classifyAddress(
            address = addrC,
            network = BlockchainNetwork.BITCOIN,
            txCount = 5,
            counterpartyCount = 3,
            hasConsolidation = false,
            hasFanOut = false
        )
        assertEquals(EntityClassificationType.INDIVIDUAL_WALLET, unhostedClassification.classification)
    }

    // ----------------------------------------------------
    // 4. TEMPORAL & GEOGRAPHIC-TIME TESTS
    // ----------------------------------------------------

    @Test
    fun testTemporalAndDiurnalRhythmAnalysis() {
        // 5 transactions spread over daytime UTC
        val txs = listOf(
            ForensicTransaction(txId = "t1", timestamp = 1700035200L), // 08:00 UTC
            ForensicTransaction(txId = "t2", timestamp = 1700038800L), // 09:00 UTC
            ForensicTransaction(txId = "t3", timestamp = 1700042400L), // 10:00 UTC
            ForensicTransaction(txId = "t4", timestamp = 1700046000L), // 11:00 UTC
            ForensicTransaction(txId = "t5", timestamp = 1700049600L)  // 12:00 UTC
        )

        val report = GeographicTimeEngine.analyzeTemporalProfile(targetAddress, txs)
        assertNotNull(report)
        assertEquals(5, report.hourlyDistribution.totalTransactionsAnalyzed)
        assertTrue(report.candidateRegions.isNotEmpty())
        assertNotNull("Report must contain forensic disclaimer", report.disclaimerEn)
        assertNotNull("Report must contain Persian forensic disclaimer", report.disclaimerFa)
    }
}
