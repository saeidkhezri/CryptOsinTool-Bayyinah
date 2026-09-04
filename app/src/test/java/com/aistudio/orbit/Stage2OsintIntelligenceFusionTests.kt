package com.aistudio.orbit

import com.aistudio.orbit.db.DatasetMetadataDao
import com.aistudio.orbit.db.DatasetMetadataEntity
import com.aistudio.orbit.db.EpistemicStatus
import com.aistudio.orbit.db.TagPackEntity
import com.aistudio.orbit.forensics.attribution.EntityAttributionEngine
import com.aistudio.orbit.forensics.correlation.CorrelationRuleType
import com.aistudio.orbit.forensics.correlation.ForensicCorrelationEngine
import com.aistudio.orbit.forensics.infra.PublicSuffixList
import com.aistudio.orbit.forensics.offline.OfflineDatasetManager
import com.aistudio.orbit.forensics.osint.adapters.MaigretAdapter
import com.aistudio.orbit.forensics.osint.adapters.PhoneInfogaAdapter
import com.aistudio.orbit.forensics.osint.adapters.SherlockAdapter
import com.aistudio.orbit.forensics.osint.bus.*
import com.aistudio.orbit.forensics.osint.contract.ModuleExecutionStatus
import com.aistudio.orbit.forensics.osint.contract.OsintExecutionContext
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Stage2OsintIntelligenceFusionTests {

    @Test
    fun testOsintEventBusAndStorage() = runBlocking {
        val testCaseId = "CASE_TEST_BUS_${System.currentTimeMillis()}"
        OsintEventBus.clearCase(testCaseId)

        val event1 = OsintEvent(
            caseId = testCaseId,
            investigationId = "INV_01",
            indicatorType = IndicatorType.DOMAIN,
            indicatorValue = "evil-crypto-drainer.com",
            normalizedValue = "evil-crypto-drainer.com",
            source = "Certificate Transparency",
            sourceLineage = SourceLineageType.ORIGINAL_SOURCE,
            epistemicStatus = EpistemicStatus.FACT,
            confidence = 0.95f
        )

        OsintEventBus.publish(event1)

        val recorded = OsintEventBus.getEventsForCase(testCaseId)
        assertEquals("Event should be stored in bus", 1, recorded.size)
        assertEquals("evil-crypto-drainer.com", recorded.first().normalizedValue)
    }

    @Test
    fun testPublicSuffixListDomainNormalization() {
        val res1 = PublicSuffixList.parse("sub.exchange.co.uk")
        assertEquals("exchange.co.uk", res1.registrableDomain)
        assertEquals("co.uk", res1.publicSuffix)
        assertEquals("sub", res1.subdomain)

        val res2 = PublicSuffixList.parse("api.binance.com")
        assertEquals("binance.com", res2.registrableDomain)
        assertEquals("com", res2.publicSuffix)
        assertEquals("api", res2.subdomain)

        val res3 = PublicSuffixList.parse("invalid")
        assertEquals("invalid", res3.registrableDomain)
    }

    @Test
    fun testMaigretAndSherlockAdapters() = runBlocking {
        val maigret = MaigretAdapter()
        val sherlock = SherlockAdapter()

        assertTrue(maigret.name.contains("Maigret"))
        assertTrue(maigret.inputTypes.contains(IndicatorType.USERNAME))
        assertTrue(maigret.outputTypes.contains(IndicatorType.SOCIAL_PROFILE))
        assertEquals(ModuleExecutionStatus.NATIVE_READY, maigret.executionStatus)

        assertTrue(sherlock.name.contains("Sherlock"))
        assertTrue(sherlock.inputTypes.contains(IndicatorType.USERNAME))

        val inputEvent = OsintEvent(
            caseId = "CASE_01",
            investigationId = "INV_01",
            indicatorType = IndicatorType.USERNAME,
            indicatorValue = "satoshinakamoto",
            normalizedValue = "satoshinakamoto",
            source = "Test",
            sourceLineage = SourceLineageType.ORIGINAL_SOURCE,
            epistemicStatus = EpistemicStatus.FACT,
            confidence = 1.0f
        )
        // Offline execution test: must return emptyList immediately without network calls
        val offlineContext = OsintExecutionContext(caseId = "CASE_01", investigationId = "INV_01", allowActiveNetwork = false, isOfflineOnly = true)
        val offlineResults = maigret.execute(inputEvent, offlineContext)
        assertTrue("Offline mode must not make network requests", offlineResults.isEmpty())

        // Non-blocking resilience test: network execution must not crash if remote sites fail/timeout
        val onlineContext = OsintExecutionContext(caseId = "CASE_01", investigationId = "INV_01", allowActiveNetwork = true, isOfflineOnly = false)
        val onlineResults = maigret.execute(inputEvent, onlineContext)
        assertNotNull("Maigret must complete execution gracefully", onlineResults)
    }

    @Test
    fun testPhoneInfogaNormalizationNoFakeIdentity() = runBlocking {
        val phoneInfoga = PhoneInfogaAdapter()
        val inputEvent = OsintEvent(
            caseId = "CASE_01",
            investigationId = "INV_01",
            indicatorType = IndicatorType.PHONE,
            indicatorValue = "+989121234567",
            normalizedValue = "+989121234567",
            source = "Test",
            sourceLineage = SourceLineageType.ORIGINAL_SOURCE,
            epistemicStatus = EpistemicStatus.FACT,
            confidence = 1.0f
        )
        val context = OsintExecutionContext(caseId = "CASE_01", investigationId = "INV_01", allowActiveNetwork = true, isOfflineOnly = false)
        val results = phoneInfoga.execute(inputEvent, context)

        assertTrue("PhoneInfoga should parse international format", results.isNotEmpty())
        val telecomEvent = results.first()
        assertEquals(IndicatorType.ORGANIZATION, telecomEvent.indicatorType)
        assertTrue("Payload should contain Iran prefix routing", telecomEvent.payloadJson.contains("Iran"))
    }

    @Test
    fun testGraphSenseTagPackManagerAndAttributionConflict() {
        val tags = listOf(
            TagPackEntity(
                recordId = "TP_1",
                address = "1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s",
                currency = "BTC",
                label = "Binance Hot Wallet",
                entity = "Binance",
                category = "exchange",
                tagpackId = "tp_binance",
                tagpackTitle = "Binance Official Cluster",
                source = "GraphSense",
                confidence = 0.95f
            ),
            TagPackEntity(
                recordId = "TP_2",
                address = "1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s",
                currency = "BTC",
                label = "Huobi Custodial Deposit",
                entity = "Huobi Global",
                category = "exchange",
                tagpackId = "tp_huobi",
                tagpackTitle = "HTX Cluster Feed",
                source = "Academic Feed",
                confidence = 0.85f
            )
        )

        val resolution = EntityAttributionEngine.resolveAttribution("1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s", tags)
        assertEquals(EntityAttributionEngine.AttributionStatus.CONFLICTING_ATTRIBUTION, resolution.status)
        assertNotNull("Conflict explanation must be provided", resolution.conflictExplanationEn)
        assertEquals(2, resolution.allCandidates.size)
    }

    @Test
    fun testForensicCorrelationAndSourceIndependence() {
        val eventA = OsintEvent(
            caseId = "CASE_01",
            investigationId = "INV_01",
            indicatorType = IndicatorType.DOMAIN,
            indicatorValue = "dark-mixer.to",
            normalizedValue = "dark-mixer.to",
            source = "SecurityTrails",
            sourceLineage = SourceLineageType.ORIGINAL_SOURCE,
            epistemicStatus = EpistemicStatus.FACT,
            confidence = 0.90f
        )
        val eventB = OsintEvent(
            caseId = "CASE_01",
            investigationId = "INV_01",
            indicatorType = IndicatorType.DOMAIN,
            indicatorValue = "dark-mixer.to",
            normalizedValue = "dark-mixer.to",
            source = "ThreatFox",
            sourceLineage = SourceLineageType.INDEPENDENT,
            epistemicStatus = EpistemicStatus.FACT,
            confidence = 0.92f
        )

        val correlations = ForensicCorrelationEngine.correlateEvents(listOf(eventA, eventB))
        assertEquals(1, correlations.size)
        val corr = correlations.first()
        assertEquals(CorrelationRuleType.SAME_DOMAIN, corr.ruleType)
        assertEquals(2, corr.independentSourceCount)
        assertTrue("Corroborated findings should have boosted confidence", corr.compositeConfidence > 0.90f)
    }

    @Test
    fun testOfflineDatasetUserImportValidation() {
        val dummyDao = object : DatasetMetadataDao {
            override fun getAllDatasetsFlow() = flowOf(emptyList<DatasetMetadataEntity>())
            override suspend fun getAllDatasets() = emptyList<DatasetMetadataEntity>()
            override suspend fun getDatasetById(id: String) = null
            override suspend fun insertDataset(dataset: DatasetMetadataEntity) {}
            override suspend fun insertDatasets(datasets: List<DatasetMetadataEntity>) {}
            override suspend fun updateDatasetStatus(id: String, status: String, progress: Float, lastUpdated: Long) {}
            override suspend fun updateDatasetInstallation(id: String, status: String, progress: Float, recordCount: Int, lastUpdated: Long) {}
            override suspend fun setDatasetEnabled(id: String, enabled: Boolean) {}
            override suspend fun deleteDataset(id: String) {}
        }
        val manager = OfflineDatasetManager(dummyDao)

        // Valid CSV
        val csv = "address,label,category\n1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa,Satoshi Genesis,Genesis"
        val resCsv = manager.validateUserImport(csv, "dataset.csv")
        assertTrue("Valid CSV should pass", resCsv.isValid)
        assertEquals("CSV", resCsv.format)
        assertTrue(resCsv.parsedRecordCount >= 1)
        assertTrue(resCsv.sha256Checksum.isNotBlank())

        // Malicious Path Traversal Check
        val traversalRes = manager.validateUserImport(csv, "../../../etc/passwd")
        assertFalse("Path traversal must be rejected", traversalRes.isValid)
        assertNotNull(traversalRes.errorMessageEn)
    }
}
