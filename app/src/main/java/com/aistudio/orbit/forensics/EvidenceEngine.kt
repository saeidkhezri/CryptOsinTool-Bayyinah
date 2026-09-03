package com.aistudio.orbit.forensics

import com.aistudio.orbit.forensics.classification.LabelingModule
import com.aistudio.orbit.forensics.clustering.ClusteringEngine
import com.aistudio.orbit.model.*
import java.util.UUID

object EvidenceEngine {

    fun generateEvidenceChain(
        targetAddress: String,
        addressValidation: AddressValidationResult,
        balanceSat: Long,
        totalReceivedSat: Long,
        totalSentSat: Long,
        transactions: List<ForensicTransaction>,
        counterparties: List<CounterpartySummary>,
        riskIndicators: List<RiskIndicator>,
        providerName: String
    ): List<EvidenceItem> {
        val evidenceList = mutableListOf<EvidenceItem>()
        val now = System.currentTimeMillis()

        // 1. On-Chain Ledger Verification (Fact)
        evidenceList.add(
            EvidenceItem(
                id = UUID.randomUUID().toString(),
                timestamp = now,
                category = EvidenceCategory.OBSERVED_ON_CHAIN,
                title = "Verified On-Chain Address Format & Existence",
                titleEn = "Verified On-Chain Address Format & Existence",
                titleFa = "تایید ساختار و فرمت آدرس در داده‌های بلاکچین",
                description = "Address '$targetAddress' cryptographically conforms to ${addressValidation.addressType.name} standard on ${addressValidation.network.displayName}.",
                descriptionEn = "Address '$targetAddress' cryptographically conforms to ${addressValidation.addressType.name} standard on ${addressValidation.network.displayName}.",
                descriptionFa = "آدرس '$targetAddress' از نظر ساختاری و رمزنگاری منطبق بر استاندارد ${addressValidation.addressType.name} در شبکه ${addressValidation.network.displayName} است.",
                rawDataSource = "Cryptographic Address Checksum & Format Specification",
                providerName = providerName,
                confidence = ConfidenceLevel.DEFINITIVE_FACT,
                relatedAddress = targetAddress,
                isDirectFact = true,
                provenance = ProvenanceRecord(
                    sourceName = providerName,
                    sourceType = DataSourceType.ON_CHAIN_RPC,
                    retrievalTimestamp = now,
                    transformationPipeline = listOf("Address Validation", "Base58/Bech32 Decoding")
                ),
                verificationStatus = VerificationStatus.VERIFIED_OFFICIAL
            )
        )

        // 2. On-Chain UTXO / Balance State (Fact)
        val balanceBtc = balanceSat.toDouble() / 100_000_000.0
        val recvBtc = totalReceivedSat.toDouble() / 100_000_000.0
        val sentBtc = totalSentSat.toDouble() / 100_000_000.0
        evidenceList.add(
            EvidenceItem(
                id = UUID.randomUUID().toString(),
                timestamp = now,
                category = EvidenceCategory.OBSERVED_ON_CHAIN,
                title = "Public Ledger Balance & Flow Verification",
                titleEn = "Public Ledger Balance & Flow Verification",
                titleFa = "تایید قطعی موجودی و حجم گردش مالی در داده‌های بلاکچین",
                description = "Current unspent balance is ${String.format("%.8f", balanceBtc)} BTC (Total Received: ${String.format("%.8f", recvBtc)} BTC, Total Sent: ${String.format("%.8f", sentBtc)} BTC).",
                descriptionEn = "Current unspent balance is ${String.format("%.8f", balanceBtc)} BTC (Total Received: ${String.format("%.8f", recvBtc)} BTC, Total Sent: ${String.format("%.8f", sentBtc)} BTC).",
                descriptionFa = "موجودی غیرخرج‌شده فعلی معادل ${String.format("%.8f", balanceBtc)} بیت‌کوین است (مجموع ورودی تاییدشده: ${String.format("%.8f", recvBtc)}، مجموع خروجی: ${String.format("%.8f", sentBtc)}).",
                rawDataSource = "Blockchain Node / Provider UTXO Index",
                providerName = providerName,
                confidence = ConfidenceLevel.DEFINITIVE_FACT,
                relatedAddress = targetAddress,
                isDirectFact = true,
                provenance = ProvenanceRecord(
                    sourceName = providerName,
                    sourceType = DataSourceType.ON_CHAIN_RPC,
                    retrievalTimestamp = now,
                    transformationPipeline = listOf("UTXO Ledger Query", "Balance Satoshis Summation")
                ),
                verificationStatus = VerificationStatus.VERIFIED_OFFICIAL
            )
        )

        // 3. Algorithmic Transaction Timeline & Normalization (Calculation)
        if (transactions.isNotEmpty()) {
            val confirmedCount = transactions.count { it.isConfirmed }
            val firstTx = transactions.minByOrNull { it.timestamp }
            val lastTx = transactions.maxByOrNull { it.timestamp }

            val firstSeenStr = firstTx?.let { TemporalUtils.formatDateTime(it.timestamp, false, true) } ?: "N/A"
            val lastSeenStr = lastTx?.let { TemporalUtils.formatDateTime(it.timestamp, false, true) } ?: "N/A"

            val firstSeenFa = firstTx?.let { TemporalUtils.formatDateTime(it.timestamp, true, true) } ?: "نامشخص"
            val lastSeenFa = lastTx?.let { TemporalUtils.formatDateTime(it.timestamp, true, true) } ?: "نامشخص"

            evidenceList.add(
                EvidenceItem(
                    id = UUID.randomUUID().toString(),
                    timestamp = now,
                    category = EvidenceCategory.ALGORITHMIC_RESULT,
                    title = "Transaction Timeline & Flow Normalization",
                    titleEn = "Transaction Timeline & Flow Normalization",
                    titleFa = "جدول زمانی تراکنش‌ها و نرمال‌سازی جریان وجوه",
                    description = "Processed ${transactions.size} transactions ($confirmedCount confirmed). Earliest observed activity on $firstSeenStr, latest on $lastSeenStr.",
                    descriptionEn = "Processed ${transactions.size} transactions ($confirmedCount confirmed). Earliest observed activity on $firstSeenStr, latest on $lastSeenStr.",
                    descriptionFa = "تعداد ${transactions.size} تراکنش پردازش شد ($confirmedCount تاییدشده). اولین فعالیت ثبت‌شده در تاریخ $firstSeenFa و آخرین فعالیت در $lastSeenFa بوده است.",
                    rawDataSource = "Normalized Transaction History",
                    providerName = "Forensic Engine Algorithmic Calculation",
                    confidence = ConfidenceLevel.DEFINITIVE_FACT,
                    relatedAddress = targetAddress,
                    isDirectFact = false,
                    provenance = ProvenanceRecord(
                        sourceName = "Orbit Normalization Pipeline",
                        sourceType = DataSourceType.CLUSTERING_ALGORITHM,
                        retrievalTimestamp = now,
                        transformationPipeline = listOf("Transaction Ingestion", "Input/Output Normalization", "Timestamp Sorting")
                    ),
                    verificationStatus = VerificationStatus.VERIFIED_OFFICIAL
                )
            )
        }

        // 4. Counterparty Relationship Aggregation (Calculation)
        if (counterparties.isNotEmpty()) {
            val top3En = counterparties.take(3).joinToString(", ") { "${it.address.take(8)}... (${it.txCount} txs)" }
            val top3Fa = counterparties.take(3).joinToString(", ") { "${it.address.take(8)}... (${it.txCount} تراکنش)" }

            evidenceList.add(
                EvidenceItem(
                    id = UUID.randomUUID().toString(),
                    timestamp = now,
                    category = EvidenceCategory.ALGORITHMIC_RESULT,
                    title = "Counterparty Aggregation Matrix",
                    titleEn = "Counterparty Aggregation Matrix",
                    titleFa = "ماتریس تجمیع طرف‌های مقابل و جریان‌های مالی",
                    description = "Identified ${counterparties.size} unique counterparty addresses. Top connected counterparties: $top3En.",
                    descriptionEn = "Identified ${counterparties.size} unique counterparty addresses. Top connected counterparties: $top3En.",
                    descriptionFa = "تعداد ${counterparties.size} طرف مقابل یکتا شناسایی شد. بیشترین تعاملات مالی با: $top3Fa.",
                    rawDataSource = "Input/Output Graph Aggregation",
                    providerName = "Forensic Engine Graph Reducer",
                    confidence = ConfidenceLevel.DEFINITIVE_FACT,
                    relatedAddress = targetAddress,
                    isDirectFact = false,
                    provenance = ProvenanceRecord(
                        sourceName = "Orbit Graph Reducer",
                        sourceType = DataSourceType.CLUSTERING_ALGORITHM,
                        retrievalTimestamp = now,
                        transformationPipeline = listOf("Counterparty Aggregation", "Volume Netting")
                    ),
                    verificationStatus = VerificationStatus.VERIFIED_OFFICIAL
                )
            )
        }

        // 5. CIOH Multi-Input Clustering Evidence (Project Sources Heuristic)
        val clusters = ClusteringEngine.clusterCIOH(transactions)
        if (clusters.isNotEmpty()) {
            val totalClusterAddresses = clusters.sumOf { it.memberAddresses.size }
            evidenceList.add(
                EvidenceItem(
                    id = UUID.randomUUID().toString(),
                    timestamp = now,
                    category = EvidenceCategory.ALGORITHMIC_RESULT,
                    title = "Common-Input Ownership Clustering (CIOH)",
                    titleEn = "Common-Input Ownership Clustering (CIOH)",
                    titleFa = "خوشه‌بندی مالکیت مشترک ورودی‌ها (CIOH)",
                    description = "Identified ${clusters.size} multi-input co-spending clusters comprising $totalClusterAddresses linked addresses with empirical confidence score ${String.format("%.2f", clusters.first().confidenceScore)}.",
                    descriptionEn = "Identified ${clusters.size} multi-input co-spending clusters comprising $totalClusterAddresses linked addresses with empirical confidence score ${String.format("%.2f", clusters.first().confidenceScore)}.",
                    descriptionFa = "تعداد ${clusters.size} خوشه مالکان مشترک ورودی (شامل $totalClusterAddresses آدرس مرتبط) با ضریب اطمینان تجربی ${String.format("%.2f", clusters.first().confidenceScore)} استخراج گردید.",
                    rawDataSource = "Multi-Input Co-Spending Disjoint Set (UnionFind)",
                    providerName = "CIOH Clustering Engine",
                    confidence = ConfidenceLevel.HIGH_CONFIDENCE,
                    relatedAddress = targetAddress,
                    isDirectFact = false,
                    provenance = ProvenanceRecord(
                        sourceName = "UnionFind CIOH Disjoint Engine",
                        sourceType = DataSourceType.CLUSTERING_ALGORITHM,
                        retrievalTimestamp = now,
                        transformationPipeline = listOf("Input Address Extraction", "Pairwise Union-Find", "Capped Linear Scoring")
                    ),
                    verificationStatus = VerificationStatus.HEURISTIC_CLUSTER
                )
            )
        }

        // 6. Known TagPack Attribution (GraphSense / Open Source Tagpack Reference)
        val knownLabel = LabelingModule.getLabelForAddress(targetAddress, addressValidation.network)
        if (knownLabel != null) {
            evidenceList.add(
                EvidenceItem(
                    id = UUID.randomUUID().toString(),
                    timestamp = now,
                    category = EvidenceCategory.ATTRIBUTION,
                    title = "Entity Attribution: ${knownLabel.entityNameEn}",
                    titleEn = "Entity Attribution: ${knownLabel.entityNameEn}",
                    titleFa = "انتساب هویت نهاد: ${knownLabel.entityNameFa}",
                    description = knownLabel.notesEn,
                    descriptionEn = knownLabel.notesEn,
                    descriptionFa = knownLabel.notesFa,
                    rawDataSource = knownLabel.sourceType.displayNameEn,
                    providerName = "TagPack & Entity Registry",
                    confidence = knownLabel.confidenceLevel,
                    relatedAddress = targetAddress,
                    isDirectFact = knownLabel.confidenceLevel == ConfidenceLevel.DEFINITIVE_FACT,
                    provenance = ProvenanceRecord(
                        sourceName = knownLabel.sourceType.displayNameEn,
                        sourceType = DataSourceType.OSINT_DATABASE,
                        retrievalTimestamp = now,
                        transformationPipeline = listOf("TagPack Index Lookup", "Entity Verification Check")
                    ),
                    verificationStatus = knownLabel.verificationStatus
                )
            )
        }

        // 7. Diurnal / Temporal Analysis (Statistical Estimation)
        if (transactions.size >= 3) {
            val timestamps = transactions.map { it.timestamp }
            val hourly = TemporalUtils.computeHourlyActivityDistribution(timestamps)
            val peakHour = hourly.indices.maxByOrNull { hourly[it] } ?: 0
            evidenceList.add(
                EvidenceItem(
                    id = UUID.randomUUID().toString(),
                    timestamp = now,
                    category = EvidenceCategory.TEMPORAL_ANALYSIS,
                    title = "Diurnal Activity Distribution (Tehran Local Time)",
                    titleEn = "Diurnal Activity Distribution (Tehran Local Time)",
                    titleFa = "تحلیل زمانی و توزیع شبانه‌روزی فعالیت (افق تهران)",
                    description = "Activity shows highest transaction density around ${String.format("%02d:00", peakHour)} Tehran Local Time (${hourly[peakHour]} events). Note: Temporal timing does not prove physical geographic location.",
                    descriptionEn = "Activity shows highest transaction density around ${String.format("%02d:00", peakHour)} Tehran Local Time (${hourly[peakHour]} events). Note: Temporal timing does not prove physical geographic location.",
                    descriptionFa = "بیشترین تراکم تراکنش‌ها در حوالی ساعت ${String.format("%02d:00", peakHour)} به وقت محلی تهران (${hourly[peakHour]} رخداد) مشاهده شد. تذکر: تطابق زمانی به تنهایی اثبات‌کننده موقعیت فیزیکی نیست.",
                    rawDataSource = "Block Header Timestamps normalized to Asia/Tehran",
                    providerName = "Temporal Analysis Module",
                    confidence = ConfidenceLevel.MEDIUM_CONFIDENCE,
                    relatedAddress = targetAddress,
                    isDirectFact = false,
                    provenance = ProvenanceRecord(
                        sourceName = "Orbit Temporal Engine",
                        sourceType = DataSourceType.HEURISTIC_ENGINE,
                        retrievalTimestamp = now,
                        transformationPipeline = listOf("UTC to Asia/Tehran Conversion", "24-Hour Diurnal Binning")
                    ),
                    verificationStatus = VerificationStatus.HEURISTIC_CLUSTER
                )
            )
        }

        // 8. Risk & Crime Pattern Indicators
        riskIndicators.forEach { risk ->
            evidenceList.add(
                EvidenceItem(
                    id = UUID.randomUUID().toString(),
                    timestamp = now,
                    category = EvidenceCategory.BEHAVIORAL_PATTERN,
                    title = risk.title,
                    titleEn = risk.title,
                    titleFa = risk.title,
                    description = "${risk.description} (Matching Score: ${String.format("%.1f", risk.matchingScore)}%). Lead: ${risk.recommendedAction}",
                    descriptionEn = "${risk.description} (Matching Score: ${String.format("%.1f", risk.matchingScore)}%). Lead: ${risk.recommendedAction}",
                    descriptionFa = "${risk.description} (درصد انطباق: ${String.format("%.1f", risk.matchingScore)}٪). اقدام پیشنهادی: ${risk.recommendedAction}",
                    rawDataSource = "Heuristic Typology Engine Evaluation",
                    providerName = "Crime Pattern Engine",
                    confidence = if (risk.matchingScore >= 80.0) ConfidenceLevel.HIGH_CONFIDENCE else ConfidenceLevel.MEDIUM_CONFIDENCE,
                    relatedAddress = targetAddress,
                    isDirectFact = false,
                    provenance = ProvenanceRecord(
                        sourceName = "Crime Pattern Library",
                        sourceType = DataSourceType.HEURISTIC_ENGINE,
                        retrievalTimestamp = now,
                        transformationPipeline = listOf("Topology Matching", "Entropy Analysis", "Scoring Evaluation")
                    ),
                    verificationStatus = VerificationStatus.HEURISTIC_CLUSTER
                )
            )
        }

        return evidenceList
    }
}
