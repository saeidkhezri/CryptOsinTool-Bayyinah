package com.aistudio.orbit

import com.aistudio.orbit.forensics.EvidenceEngine
import com.aistudio.orbit.forensics.TemporalUtils
import com.aistudio.orbit.forensics.export.ForensicReportExporter
import com.aistudio.orbit.forensics.patterns.CrimePatternEngine
import com.aistudio.orbit.forensics.patterns.CrimePatternLibrary
import com.aistudio.orbit.forensics.temporal.GeographicTimeEngine
import com.aistudio.orbit.model.*
import com.aistudio.orbit.repository.AppLanguage
import com.aistudio.orbit.security.auth.AuthManager
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class Stage567ForensicTests {

    private val targetAddress = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"

    private fun createSampleTransactions(): List<ForensicTransaction> {
        val baseTime = 1700000000L 
        return listOf(
            ForensicTransaction(
                txId = "tx_001",
                timestamp = baseTime, 
                blockHeight = 800000,
                direction = TxDirection.INCOMING,
                relevantAmountSat = 150_000_000L,
                feeSat = 5000L,
                inputs = listOf(TxInput(prevOutAddress = "src1", prevOutValueSat = 150_000_000L)),
                outputs = listOf(TxOutput(address = targetAddress, valueSat = 150_000_000L))
            ),
            ForensicTransaction(
                txId = "tx_002",
                timestamp = baseTime + 1800, 
                blockHeight = 800003,
                direction = TxDirection.OUTGOING,
                relevantAmountSat = 140_000_000L,
                feeSat = 10000L,
                inputs = listOf(TxInput(prevOutAddress = targetAddress, prevOutValueSat = 150_000_000L)),
                outputs = listOf(TxOutput(address = "dst1", valueSat = 140_000_000L))
            ),
            ForensicTransaction(
                txId = "tx_003",
                timestamp = baseTime + 86400 * 2, 
                blockHeight = 800288,
                direction = TxDirection.INCOMING,
                relevantAmountSat = 50_000_000L,
                feeSat = 8000L,
                inputs = listOf(TxInput(prevOutAddress = "src2", prevOutValueSat = 50_000_000L)),
                outputs = listOf(TxOutput(address = targetAddress, valueSat = 50_000_000L))
            )
        )
    }

    @Test
    fun testStage5TemporalAnalysisAndRegionalCompatibility() {
        val txs = createSampleTransactions()
        val report = GeographicTimeEngine.analyzeTemporalProfile(targetAddress, txs)

        assertNotNull(report)
        assertEquals(targetAddress, report.targetAddress)
        assertEquals(3, report.hourlyDistribution.totalTransactionsAnalyzed)

        assertTrue(report.hourlyDistribution.peakHourUtc in 0..23)
        assertTrue(report.hourlyDistribution.peakHourTehran in 0..23)

        assertNotNull(report.candidateRegions)
        assertTrue(report.candidateRegions.isNotEmpty())
        
        val tehranRegion = report.candidateRegions.find { it.regionCode == "REG_TEHRAN_ME" }
        assertNotNull(tehranRegion)
        assertTrue(tehranRegion!!.compatibilityScore >= 0.0)
    }

    @Test
    fun testStage6CrimePatternLibraryMatching() {
        val txs = createSampleTransactions()
        
        val fanInInputs = (1..6).map { TxInput(prevOutAddress = "in_$it", prevOutValueSat = 10_000_000L) }
        val fanInTx = ForensicTransaction(
            txId = "fan_in_tx",
            timestamp = 1700100000L,
            blockHeight = 800100,
            direction = TxDirection.INCOMING,
            relevantAmountSat = 60_000_000L,
            feeSat = 5000L,
            inputs = fanInInputs,
            outputs = listOf(TxOutput(address = targetAddress, valueSat = 60_000_000L))
        )

        val allTxs = txs + fanInTx
        val counterparties = listOf(
            CounterpartySummary(
                address = "cp1",
                txCount = 2,
                totalReceivedSatFromCounterparty = 100_000_000L,
                totalSentSatToCounterparty = 50_000_000L,
                netVolumeSat = 50_000_000L,
                label = null
            )
        )

        val patternMatches = CrimePatternEngine.matchPatterns(targetAddress, allTxs, counterparties)
        
        val fanInMatch = patternMatches.find { it.patternId == "PAT_AML_FAN_IN" }
        assertNotNull(fanInMatch)
        assertTrue(fanInMatch!!.similarityScore >= 50.0)
    }

    @Test
    fun testStage7ForensicReportExporterDossierAndCsv() {
        val now = System.currentTimeMillis()
        val txs = createSampleTransactions()
        val counterparties = listOf(
            CounterpartySummary(
                address = "cp_001",
                txCount = 5,
                totalReceivedSatFromCounterparty = 250_000_000L,
                totalSentSatToCounterparty = 120_000_000L,
                netVolumeSat = 130_000_000L,
                label = "Verified Exchange Link"
            )
        )
        val case = InvestigationCase(
            caseId = "case_123",
            referenceNumber = "99-F-2026",
            caseName = "عملیات ردیابی سرد (Operation Cold Trace)",
            targetAddress = targetAddress,
            network = BlockchainNetwork.BITCOIN,
            status = InvestigationStatus.COMPLETED,
            balanceSat = 115_000_000L,
            totalReceivedSat = 325_000_000L,
            totalSentSat = 210_000_000L,
            totalTransactionsFound = txs.size,
            transactions = txs,
            counterparties = counterparties,
            riskIndicators = listOf(
                RiskIndicator(
                    id = "IND_001",
                    code = "PAT_AML_DORMANT_ACTIVATION",
                    title = "Dormant Reactivation",
                    severity = RiskSeverity.HIGH,
                    category = "Temporal",
                    description = "The target address reactivated after 180+ days.",
                    matchingScore = 85.0,
                    confidence = ConfidenceLevel.HIGH_CONFIDENCE,
                    relatedAddresses = listOf(targetAddress),
                    relatedTxHashes = emptyList(),
                    recommendedAction = "Trace the source of the triggering funding transaction."
                )
            ),
            evidenceLog = listOf(
                EvidenceItem(
                    id = UUID.randomUUID().toString(),
                    timestamp = now,
                    category = EvidenceCategory.OBSERVED_ON_CHAIN,
                    title = "Verified Target Format",
                    titleEn = "Verified Target Format",
                    titleFa = "فرمت آدرس تایید شده است",
                    description = "Target is a valid legacy Bitcoin address.",
                    descriptionEn = "Target is a valid legacy Bitcoin address.",
                    descriptionFa = "آدرس یک فرمت قدیمی بیت‌کوین تایید شده است.",
                    rawDataSource = "Crypto Format Checks",
                    providerName = "Local System Checks",
                    confidence = ConfidenceLevel.DEFINITIVE_FACT,
                    relatedAddress = targetAddress,
                    isDirectFact = true,
                    provenance = ProvenanceRecord(
                        sourceName = "System",
                        sourceType = DataSourceType.ON_CHAIN_RPC,
                        retrievalTimestamp = now,
                        transformationPipeline = listOf("Check")
                    ),
                    verificationStatus = VerificationStatus.VERIFIED_OFFICIAL
                )
            ),
            notes = "Forensic case setup complete. Verified ledger data is mapped properly."
        )

        val reportEn = ForensicReportExporter.generateTextDossier(case, AppLanguage.ENGLISH)
        val reportFa = ForensicReportExporter.generateTextDossier(case, AppLanguage.PERSIAN)

        assertNotNull(reportEn)
        assertTrue(reportEn.contains("OFFICIAL BLOCKCHAIN FORENSIC"))
        assertTrue(reportEn.contains("99-F-2026"))
        assertTrue(reportEn.contains("Operation Cold Trace"))
        assertTrue(reportEn.contains("Verified Target Format"))

        assertNotNull(reportFa)
        assertTrue(reportFa.contains("گزارش رسمی جرم‌یابی"))
        assertTrue(reportFa.contains("عملیات"))
        assertTrue(reportFa.contains("موجودی فعلی آدرس"))

        val csv = ForensicReportExporter.generateCsvDataset(case, AppLanguage.ENGLISH)
        assertNotNull(csv)
        assertTrue(csv.contains("TxID,Timestamp_UTC,BlockHeight"))
        assertTrue(csv.contains("tx_001"))
        assertTrue(csv.contains("Counterparty_Address"))
    }

    @Test
    fun testRoleBasedPermissionsAndAuthentication() {
        val admin = AuthManager.usersList.value.find { it.userId == "usr_admin" }
        assertNotNull(admin)
        assertEquals(UserRole.ADMINISTRATOR, admin!!.role)

        val investigator = AuthManager.usersList.value.find { it.userId == "usr_investigator_1" }
        assertNotNull(investigator)
        assertEquals(UserRole.LEAD_INVESTIGATOR, investigator!!.role)

        assertTrue(investigator.grantedPermissions.contains(ForensicPermission.BEHAVIORAL_ANALYSIS))
        assertTrue(investigator.grantedPermissions.contains(ForensicPermission.FORENSIC_REPORTS_EXPORT))

        val auditor = AuthManager.usersList.value.find { it.userId == "usr_auditor_3" }
        assertNotNull(auditor)
        assertEquals(UserRole.AUDITOR, auditor!!.role)
        assertFalse(auditor.grantedPermissions.contains(ForensicPermission.BITCOIN_ANALYSIS)) 
    }
}
