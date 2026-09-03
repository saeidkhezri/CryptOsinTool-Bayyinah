package com.aistudio.orbit

import com.aistudio.orbit.forensics.export.ForensicEvidenceSealer
import com.aistudio.orbit.forensics.graph.EntityFilterMode
import com.aistudio.orbit.forensics.graph.ForensicCaseGraphEngine
import com.aistudio.orbit.forensics.osint.*
import com.aistudio.orbit.forensics.osint.identity.*
import com.aistudio.orbit.forensics.osint.identity.Email2PhoneNumberPipeline.ReconstructionCandidate
import com.aistudio.orbit.model.*
import com.aistudio.orbit.repository.AppLanguage
import org.junit.Assert.*
import org.junit.Test

class ForensicOsintPrompt3Tests {

    private fun createSampleCase(): InvestigationCase {
        return InvestigationCase(
            caseId = "CASE-2026-TEST-001",
            referenceNumber = "FATA-CRIME-9921",
            caseName = "Test Ransomware Financial Forensics",
            targetAddress = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa",
            network = BlockchainNetwork.BITCOIN,
            createdTimestamp = System.currentTimeMillis(),
            updatedTimestamp = System.currentTimeMillis(),
            balanceSat = 5_012_500_000L,
            totalTransactionsFound = 45,
            counterparties = listOf(
                CounterpartySummary(
                    address = "1CounterpartyExchangeBinanceHotWallet",
                    network = BlockchainNetwork.BITCOIN,
                    txCount = 12,
                    totalReceivedSatFromCounterparty = 100_000_000L,
                    label = "Binance Hot Wallet 14"
                ),
                CounterpartySummary(
                    address = "1IntermediateMulePeelAddress123",
                    network = BlockchainNetwork.BITCOIN,
                    txCount = 4,
                    totalReceivedSatFromCounterparty = 45_000_000L,
                    label = "Intermediary Mule Wallet"
                )
            ),
            riskIndicators = listOf(
                RiskIndicator(
                    id = "RISK_01",
                    code = "HIGH_VOLUME_MIXER",
                    title = "High Volume Mixer Interaction",
                    severity = RiskSeverity.HIGH,
                    category = "ON_CHAIN_ANOMALY",
                    description = "Funds routed through Wasabi CoinJoin mixer",
                    matchingScore = 92.0,
                    confidence = ConfidenceLevel.HIGH_CONFIDENCE
                )
            )
        )
    }

    @Test
    fun testForensicCaseGraphEngineBuildAndEntities() {
        val testCase = createSampleCase()
        val osint = OsintForensicsEngine.performOsintInvestigation(testCase.targetAddress, testCase.network)
        val candidates = listOf(
            ReconstructionCandidate(
                candidateE164 = "+989129998877",
                carrier = "MCI Hamrah-e Aval",
                carrierFa = "همراه اول",
                lineType = "Mobile / Permanent",
                lineTypeFa = "تلفن همراه دائمی",
                isIgnorantVerified = true,
                matchConfidence = 91f,
                verificationDetails = "Matched via Holehe masked hint and Iranian Telecom HLR"
            )
        )

        val graph = ForensicCaseGraphEngine.buildCaseGraph(testCase, osint, candidates)

        assertNotNull(graph)
        assertEquals("CASE-2026-TEST-001", graph.caseId)
        assertEquals("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa", graph.targetAddress)

        // Target node
        val targetNode = graph.nodes.find { it.isTarget }
        assertNotNull(targetNode)
        assertEquals(ForensicEntityCategory.CRYPTO_WALLET, targetNode?.entityCategory)
        assertEquals(ForensicEpistemicStatus.UNDISPUTED_LEDGER_FACT, targetNode?.epistemicStatus)

        // On-chain Counterparty nodes
        val exchangeNode = graph.nodes.find { it.entityCategory == ForensicEntityCategory.EXCHANGE_HOT_WALLET }
        assertNotNull(exchangeNode)
        assertTrue(exchangeNode!!.label.contains("Binance", ignoreCase = true))

        // Off-chain OSINT nodes
        val ipNode = graph.nodes.find { it.entityCategory == ForensicEntityCategory.IP_NETWORK_NODE }
        if (osint.ipExposures.isNotEmpty()) {
            assertNotNull(ipNode)
        }

        val emailNode = graph.nodes.find { it.entityCategory == ForensicEntityCategory.EMAIL_ADDRESS }
        if (osint.leakRecords.isNotEmpty()) {
            assertNotNull(emailNode)
            assertEquals(ForensicEpistemicStatus.DERIVED_OSINT_INFERENCE, emailNode?.epistemicStatus)
        }

        val phoneNode = graph.nodes.find { it.entityCategory == ForensicEntityCategory.PHONE_NUMBER }
        assertNotNull(phoneNode)
        assertTrue(phoneNode!!.label.contains("+989129998877"))

        // Edges
        assertTrue("Graph should contain interconnected nodes", graph.nodes.isNotEmpty())

        // Test filtering
        val onChainOnly = ForensicCaseGraphEngine.filterGraph(graph, EntityFilterMode.ON_CHAIN_ONLY)
        assertTrue(onChainOnly.nodes.all { it.isOnChain })

        val osintOnly = ForensicCaseGraphEngine.filterGraph(graph, EntityFilterMode.OFF_CHAIN_OSINT_ONLY)
        assertTrue(osintOnly.nodes.all { it.isOffChain || it.isTarget })

        // Test shortest path BFS
        val path = ForensicCaseGraphEngine.findShortestPath(graph, targetNode!!.id, phoneNode.id)
        assertTrue("Path should connect target wallet to phone node", path.isNotEmpty())
        assertEquals(targetNode.id, path.first())
        assertEquals(phoneNode.id, path.last())
    }

    @Test
    fun testForensicEvidenceSealerJsonAndSha256() {
        val testCase = createSampleCase()
        val osint = OsintForensicsEngine.performOsintInvestigation(testCase.targetAddress, testCase.network)
        val candidates = listOf(
            ReconstructionCandidate(
                candidateE164 = "+989129998877",
                carrier = "MCI",
                carrierFa = "همراه اول",
                lineType = "Mobile",
                lineTypeFa = "موبایل",
                isIgnorantVerified = true,
                matchConfidence = 89f,
                verificationDetails = "Verified"
            )
        )

        val graph = ForensicCaseGraphEngine.buildCaseGraph(testCase, osint, candidates)
        val jsonDossier = ForensicEvidenceSealer.generateCourtArchiveJson(
            investigationCase = testCase,
            graph = graph,
            osintReport = osint,
            language = AppLanguage.ENGLISH
        )

        assertNotNull(jsonDossier)
        assertTrue("Dossier must contain SHA-256 seal", jsonDossier.contains("sha256Seal"))
        assertTrue("Dossier must contain case ref", jsonDossier.contains("FATA-CRIME-9921"))
        assertTrue("Dossier must contain target address", jsonDossier.contains("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"))
        assertTrue("Dossier must contain ISO standard reference", jsonDossier.contains("ISO/IEC 27037:2012"))
        assertTrue("Dossier must contain graph topology", jsonDossier.contains("graphTopology"))
        assertTrue("Dossier must contain subpoena leads", jsonDossier.contains("judicialSubpoenaLeads"))
    }

    @Test
    fun testForensicEvidenceSealerSubpoenaRequisitionText() {
        val testCase = createSampleCase()
        val osint = OsintForensicsEngine.performOsintInvestigation(testCase.targetAddress, testCase.network)

        val subpoenaFa = ForensicEvidenceSealer.generateSubpoenaRequisitionText(
            investigationCase = testCase,
            osintReport = osint,
            language = AppLanguage.PERSIAN
        )

        assertTrue(subpoenaFa.contains("فرم رسمی پیش‌نویس استعلام و دستور قضایی"))
        assertTrue(subpoenaFa.contains("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"))
        assertTrue(subpoenaFa.contains("FATA-CRIME-9921"))
        assertTrue(subpoenaFa.contains("SHA-256"))

        val subpoenaEn = ForensicEvidenceSealer.generateSubpoenaRequisitionText(
            investigationCase = testCase,
            osintReport = osint,
            language = AppLanguage.ENGLISH
        )

        assertTrue(subpoenaEn.contains("FORMAL FORENSIC SUBPOENA & JUDICIAL REQUISITION"))
        assertTrue(subpoenaEn.contains("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"))
        assertTrue(subpoenaEn.contains("FATA-CRIME-9921"))
        assertTrue(subpoenaEn.contains("Cryptographic Integrity Seal"))
    }

    @Test
    fun testForensicToolControllerManagement() {
        val tools = ForensicToolController.tools.value
        assertEquals(8, tools.size)

        // Check key tools are configured
        assertTrue(tools.any { it.toolId == "HOLEHE" })
        assertTrue(tools.any { it.toolId == "EMAIL2PHONE" })
        assertTrue(tools.any { it.toolId == "GHUNT" })
        assertTrue(tools.any { it.toolId == "PHONEINFOGA" })
        assertTrue(tools.any { it.toolId == "IGNORANT" })
        assertTrue(tools.any { it.toolId == "EPIEOS" })
        assertTrue(tools.any { it.toolId == "GRAPHSENSE" })
        assertTrue(tools.any { it.toolId == "SPIDERFOOT" })

        // Toggle tool
        ForensicToolController.toggleTool("HOLEHE", false)
        assertFalse(ForensicToolController.tools.value.first { it.toolId == "HOLEHE" }.isEnabled)
        ForensicToolController.toggleTool("HOLEHE", true)
        assertTrue(ForensicToolController.tools.value.first { it.toolId == "HOLEHE" }.isEnabled)

        // Logging
        val initialCount = ForensicToolController.executionLogs.value.size
        ForensicToolController.addLog(
            ForensicExecutionLogItem(
                toolId = "TEST_TOOL",
                toolName = "Test Recon Tool",
                target = "test@example.com",
                status = ToolExecutionStatus.SUCCESS,
                messageEn = "Discovered test artifact",
                messageFa = "سرنخ تستی کشف شد"
            )
        )
        assertEquals(initialCount + 1, ForensicToolController.executionLogs.value.size)

        ForensicToolController.clearLogs()
        assertEquals(0, ForensicToolController.executionLogs.value.size)
    }

    @Test
    fun testOnChainToOffChainHandoffEngine() {
        val target = "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045" // vitalik.eth
        val txs = listOf(
            ForensicTransaction(
                txId = "0x123",
                network = BlockchainNetwork.ETHEREUM,
                timestamp = System.currentTimeMillis() / 1000,
                direction = TxDirection.INCOMING,
                relevantAmountSat = 150_000_000L,
                notes = "Ref: Payment Memo Code"
            )
        )

        val (entity, indicators) = OnChainToOffChainHandoffEngine.extractIndicatorsFromOnChain(
            targetAddress = target,
            network = BlockchainNetwork.ETHEREUM,
            transactions = txs
        )

        assertNotNull(entity)
        assertEquals(target, entity.primaryAddress)

        assertNotNull(indicators)
        assertTrue(indicators.any { it.type == IndicatorType.CRYPTO_ADDRESS && it.normalizedValue == target.lowercase() })
        assertTrue(indicators.any { it.type == IndicatorType.MEMO_TEXT })
    }

    @Test
    fun testOsintEventBusPipeline() = kotlinx.coroutines.runBlocking {
        val bus = OsintEventBus()
        val received = mutableListOf<ForensicEvent>()

        // Test subscribe with filter
        val job = bus.subscribe(this, filter = { it.type == ForensicEventType.ONCHAIN_ADDRESS_DISCOVERED }) {
            received.add(it)
        }

        val event1 = ForensicEvent(
            eventId = "EVT-1",
            type = ForensicEventType.ONCHAIN_ADDRESS_DISCOVERED,
            indicatorType = IndicatorType.CRYPTO_ADDRESS,
            data = "0xabc",
            confidenceScore = 0.9f,
            emittingModule = "UnitTester",
            targetAddress = "0xabc",
            descriptionEn = "Discovered vitalik.eth address on-chain",
            descriptionFa = "آدرس کشف شده"
        )

        val event2 = ForensicEvent(
            eventId = "EVT-2",
            type = ForensicEventType.PUBLIC_EMAIL_DISCOVERED,
            indicatorType = IndicatorType.EMAIL_ADDRESS,
            data = "test@example.com",
            confidenceScore = 0.8f,
            emittingModule = "UnitTester",
            targetAddress = "0xabc",
            descriptionEn = "Discovered email",
            descriptionFa = "ایمیل کشف شده"
        )

        bus.publish(event1)
        bus.publish(event2) // Should be filtered out

        kotlinx.coroutines.delay(100)
        assertEquals(1, received.size)
        assertEquals("EVT-1", received[0].eventId)

        // Test Deduplication
        bus.clear()
        val dupEvent = ForensicEvent(
            eventId = "EVT-D1",
            type = ForensicEventType.INDICATOR_DISCOVERED,
            indicatorType = IndicatorType.EMAIL_ADDRESS,
            data = "trader@gmail.com",
            confidenceScore = 0.8f,
            emittingModule = "UnitTester",
            targetAddress = "0xabc",
            descriptionEn = "Discovered email duplicate",
            descriptionFa = "ایمیل تکراری"
        )
        bus.publish(dupEvent)
        bus.publish(dupEvent) // Duplicate, should be skipped
        
        assertEquals(1, bus.eventLog.value.size)

        // Test Retry Pipeline
        val success = bus.publishWithRetry(event1, maxRetries = 2)
        assertTrue(success)

        job.cancel()
    }

    @Test
    fun testIdentityCorrelationModels() {
        val relationship = IdentityRelationship(
            relationshipId = "REL-1",
            sourceEntityId = "EMAIL-1",
            targetEntityId = "PROFILE-1",
            type = IdentityRelationshipType.EMAIL_USED_BY_PUBLIC_PROFILE,
            sourceProvenance = "Holehe Scanner Integration Probe",
            confidence = 0.85f
        )

        assertNotNull(relationship)
        assertEquals("REL-1", relationship.relationshipId)
        assertEquals(IdentityRelationshipType.EMAIL_USED_BY_PUBLIC_PROFILE, relationship.type)
        assertEquals(0.85f, relationship.confidence)
    }
}
