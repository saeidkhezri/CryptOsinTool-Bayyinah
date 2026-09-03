package com.aistudio.orbit

import com.aistudio.orbit.forensics.ForensicStatisticsEngine
import com.aistudio.orbit.forensics.TransactionAnalyzer
import com.aistudio.orbit.forensics.filter.FilterDirection
import com.aistudio.orbit.forensics.filter.ForensicFilterEngine
import com.aistudio.orbit.forensics.filter.InvestigationFilterState
import com.aistudio.orbit.forensics.relationship.RelationshipAnalyzer
import com.aistudio.orbit.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage3ForensicTests {

    private val targetAddress = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
    private val counterparty1 = "3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy"
    private val counterparty2 = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"

    private fun createSampleTransactions(): List<ForensicTransaction> {
        val now = 1700000000L // epoch seconds

        return listOf(
            ForensicTransaction(
                txId = "tx_001",
                timestamp = now,
                blockHeight = 800000,
                direction = TxDirection.INCOMING,
                relevantAmountSat = 100_000_000L, // 1 BTC
                feeSat = 10_000L,
                inputs = listOf(TxInput(prevOutAddress = counterparty1, prevOutValueSat = 100_000_000L)),
                outputs = listOf(TxOutput(address = targetAddress, valueSat = 100_000_000L))
            ),
            ForensicTransaction(
                txId = "tx_002",
                timestamp = now + 86400L, // +1 day
                blockHeight = 800144,
                direction = TxDirection.INCOMING,
                relevantAmountSat = 100_000_000L, // 1 BTC (repeated amount)
                feeSat = 10_000L,
                inputs = listOf(TxInput(prevOutAddress = counterparty1, prevOutValueSat = 100_000_000L)),
                outputs = listOf(TxOutput(address = targetAddress, valueSat = 100_000_000L))
            ),
            ForensicTransaction(
                txId = "tx_003",
                timestamp = now + 172800L, // +2 days
                blockHeight = 800288,
                direction = TxDirection.OUTGOING,
                relevantAmountSat = 50_000_000L, // 0.5 BTC
                feeSat = 15_000L,
                inputs = listOf(TxInput(prevOutAddress = targetAddress, prevOutValueSat = 50_000_000L)),
                outputs = listOf(TxOutput(address = counterparty2, valueSat = 50_000_000L))
            ),
            ForensicTransaction(
                txId = "tx_004",
                timestamp = now + 259200L, // +3 days
                blockHeight = 800432,
                direction = TxDirection.OUTGOING,
                relevantAmountSat = 20_000_000L, // 0.2 BTC
                feeSat = 12_000L,
                inputs = listOf(TxInput(prevOutAddress = targetAddress, prevOutValueSat = 20_000_000L)),
                outputs = listOf(TxOutput(address = counterparty1, valueSat = 20_000_000L))
            )
        )
    }

    @Test
    fun testCounterpartyExtraction() {
        val txs = createSampleTransactions()
        val counterparties = TransactionAnalyzer.extractCounterparties(targetAddress, txs)

        assertEquals("Should extract 2 unique counterparties", 2, counterparties.size)

        val cp1 = counterparties.find { it.address == counterparty1 }
        assertNotNull("Counterparty 1 must be found", cp1)
        assertEquals(3, cp1!!.txCount) // 2 incoming + 1 outgoing
        assertEquals(200_000_000L, cp1.totalReceivedSatFromCounterparty)
        assertEquals(20_000_000L, cp1.totalSentSatToCounterparty)
        assertEquals(180_000_000L, cp1.netVolumeSat)

        val cp2 = counterparties.find { it.address == counterparty2 }
        assertNotNull("Counterparty 2 must be found", cp2)
        assertEquals(1, cp2!!.txCount)
        assertEquals(50_000_000L, cp2.totalSentSatToCounterparty)
    }

    @Test
    fun testPairwiseRelationshipDeepDive() {
        val txs = createSampleTransactions()
        val pairwise = RelationshipAnalyzer.analyzePairwiseRelationship(targetAddress, counterparty1, txs)

        assertNotNull(pairwise)
        assertEquals(targetAddress, pairwise.addressA)
        assertEquals(counterparty1, pairwise.addressB)
        assertEquals(3, pairwise.totalDirectInteractions)
        assertEquals(0.2, pairwise.totalVolumeAToB, 0.001) // Sent to B: 0.2 BTC
        assertEquals(2.0, pairwise.totalVolumeBToA, 0.001) // Received from B: 2.0 BTC
        assertEquals(3, pairwise.directTxHashes.size)
        assertTrue("Pairwise flow should reflect net flow calculation", pairwise.netFlowAToB != 0.0)
    }

    @Test
    fun testDescriptiveStatisticsComputation() {
        val txs = createSampleTransactions()
        val counterparties = TransactionAnalyzer.extractCounterparties(targetAddress, txs)
        val stats = ForensicStatisticsEngine.computeDescriptiveStatistics(targetAddress, BlockchainNetwork.BITCOIN, txs, counterparties)

        assertEquals(4, stats.totalTxCount)
        assertEquals(2, stats.inboundTxCount)
        assertEquals(2, stats.outboundTxCount)
        assertEquals(200_000_000L, stats.totalInboundSat)
        assertEquals(70_000_000L, stats.totalOutboundSat)
        assertEquals(100_000_000L, stats.avgInboundSat)
        assertEquals(100_000_000L, stats.medianInboundSat)
        assertEquals(35_000_000L, stats.avgOutboundSat)
        assertEquals(20_000_000L, stats.minAmountSat)
        assertEquals(100_000_000L, stats.maxAmountSat)
        assertEquals(2, stats.uniqueCounterpartyCount)
        assertEquals(1, stats.repeatedCounterpartyCount) // counterparty1 has 3 interactions
        assertEquals(4, stats.activeDaysCount)
        assertTrue("Average interval should be approx 24h", stats.avgIntervalHours in 23.0..25.0)

        // Test repeated amount clusters (100_000_000 sat occurred twice)
        assertEquals(1, stats.repeatedAmountClusters.size)
        assertEquals(100_000_000L, stats.repeatedAmountClusters[0].amountSat)
        assertEquals(2, stats.repeatedAmountClusters[0].occurrenceCount)
    }

    @Test
    fun testForensicCounterpartyFiltering() {
        val txs = createSampleTransactions()
        val counterparties = TransactionAnalyzer.extractCounterparties(targetAddress, txs)

        // Filter: minimum 1.0 BTC
        val filterMinAmount = InvestigationFilterState(minAmountBtc = 1.0)
        val filtered = ForensicFilterEngine.filterCounterparties(counterparties, filterMinAmount)
        assertEquals(1, filtered.size)
        assertEquals(counterparty1, filtered[0].address)

        // Filter: incoming only
        val filterIncoming = InvestigationFilterState(direction = FilterDirection.INCOMING_ONLY)
        val filteredInc = ForensicFilterEngine.filterCounterparties(counterparties, filterIncoming)
        assertEquals(1, filteredInc.size)
        assertEquals(counterparty1, filteredInc[0].address)

        // Filter: search by query
        val filterSearch = InvestigationFilterState(searchQuery = "3J98t1")
        val filteredSearch = ForensicFilterEngine.filterCounterparties(counterparties, filterSearch)
        assertEquals(1, filteredSearch.size)
        assertEquals(counterparty1, filteredSearch[0].address)
    }
}
