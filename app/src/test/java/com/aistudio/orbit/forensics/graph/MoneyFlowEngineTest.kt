package com.aistudio.orbit.forensics.graph

import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.ForensicTransaction
import com.aistudio.orbit.model.TxInput
import com.aistudio.orbit.model.TxOutput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyFlowEngineTest {
    @Test fun tracesObservedMultiHopPathWithoutInventingHops() {
        val tx1 = ForensicTransaction("tx1", timestamp=1L, inputs=listOf(TxInput(prevOutAddress="A")), outputs=listOf(TxOutput(address="B", valueSat=1000)))
        val tx2 = ForensicTransaction("tx2", timestamp=2L, inputs=listOf(TxInput(prevOutAddress="B")), outputs=listOf(TxOutput(address="C", valueSat=900)))
        val flows = MoneyFlowEngine.traceMoneyFlow("A", "C", listOf(tx1,tx2), BlockchainNetwork.BITCOIN, MoneyFlowTraversalConfig(maxHops=3))
        assertEquals(1, flows.size)
        assertEquals(2, flows.first().hops.size)
        assertEquals(900, flows.first().totalVolumeSat)
        assertTrue(flows.first().overallConfidence.name != "DEFINITIVE_FACT")
    }
}
