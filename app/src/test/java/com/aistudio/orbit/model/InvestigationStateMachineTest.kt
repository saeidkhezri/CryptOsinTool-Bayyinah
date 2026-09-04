package com.aistudio.orbit.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class InvestigationStateMachineTest {
    private fun case(transactions: List<ForensicTransaction> = emptyList(), total: Int = transactions.size) = InvestigationCase(
        caseId = "case-test", referenceNumber = "T-001", caseName = "Test", targetAddress = "bc1qtest", totalTransactionsFound = total, transactions = transactions
    )

    @Test fun zeroBalanceDoesNotBecomeBlocked() {
        val info = InvestigationStateMachine.determineState(case(), false, false, false, false)
        assertFalse(info.state is InvestigationState.Blocked)
        assertEquals("DISCOVER_TRANSACTIONS", info.nextBestActionDetailed?.actionCode)
    }

    @Test fun transactionsWithoutCounterpartiesRecommendExtraction() {
        val tx = ForensicTransaction("tx1", timestamp = 1L, inputs = listOf(TxInput(prevOutAddress="bc1qtest")), outputs = listOf(TxOutput(address="bc1qother", valueSat=1000)))
        val info = InvestigationStateMachine.determineState(case(listOf(tx)), false, false, false, false)
        assertEquals("EXTRACT_COUNTERPARTIES", info.nextBestActionDetailed?.actionCode)
    }
}
