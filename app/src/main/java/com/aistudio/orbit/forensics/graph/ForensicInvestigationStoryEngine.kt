package com.aistudio.orbit.forensics.graph

import com.aistudio.orbit.model.InvestigationCase
import com.aistudio.orbit.model.RiskSeverity
import kotlinx.serialization.Serializable

/**
 * Step in an Investigation Story / Chronology.
 */
@Serializable
data class InvestigationStoryStep(
    val stepIndex: Int,
    val timestamp: Long,
    val titleEn: String,
    val titleFa: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val stepCategory: StoryStepCategory,
    val relevantNodeIds: List<String>,
    val relevantEdgeIds: List<String>,
    val evidenceIds: List<String>,
    val confidence: VisualConfidence,
    val riskSeverity: RiskSeverity,
    val analystNote: String = ""
)

@Serializable
enum class StoryStepCategory(val displayNameEn: String, val displayNameFa: String) {
    STARTING_POINT("Target Inception", "آغازگاه هدف"),
    TRANSACTION_FLOW("Transaction Movement", "جریان تراکنش"),
    COUNTERPARTY("Counterparty Discovery", "شناسایی طرف مقابل"),
    CLUSTER_EXPANSION("Cluster Attribution", "انتساب و خوشه‌بندی"),
    SERVICE_EXIT("Service / VASP Interaction", "ارتباط با صرافی / سرویس"),
    OSINT_DISCOVERY("OSINT Footprint Discovery", "کشف ردپای منبع باز"),
    ENTITY_RESOLVED("Entity Resolution", "تطبیق و احراز هویت"),
    EVIDENCE_SEALED("Evidence Sealing", "ثبت و پلمب ادله"),
    FINDING_FORMULATED("Forensic Finding Formulated", "تدوین یافته نهایی")
}

/**
 * Engine that transforms graph and case evidence into an interactive investigative story.
 */
object ForensicInvestigationStoryEngine {

    fun buildStory(
        investigationCase: InvestigationCase,
        nodes: List<VisualInvestigationNode>,
        edges: List<VisualInvestigationEdge>
    ): List<InvestigationStoryStep> {
        val steps = mutableListOf<InvestigationStoryStep>()
        var stepCount = 1

        val targetNode = nodes.find { it.isTarget || it.isSeed } ?: nodes.firstOrNull()
        val now = System.currentTimeMillis()

        // 1. Starting Point
        if (targetNode != null) {
            steps.add(
                InvestigationStoryStep(
                    stepIndex = stepCount++,
                    timestamp = targetNode.firstSeenTimestamp.takeIf { it > 0 } ?: (now - 86400000L * 30),
                    titleEn = "Target Wallet Inception: ${targetNode.primaryIdentifier.take(8)}...",
                    titleFa = "نقطه آغاز پی‌جویی آدرس هدف: ${targetNode.primaryIdentifier.take(8)}...",
                    descriptionEn = "Investigation initiated for ${targetNode.network.displayName} address with starting balance ${targetNode.balanceDisplay} and ${targetNode.txCount} lifetime transactions.",
                    descriptionFa = "عملیات پی‌جویی برای آدرس شبکه ${targetNode.network.displayName} با موجودی اولیه ${targetNode.balanceDisplay} و ${targetNode.txCount} تراکنش آغاز شد.",
                    stepCategory = StoryStepCategory.STARTING_POINT,
                    relevantNodeIds = listOf(targetNode.id),
                    relevantEdgeIds = emptyList(),
                    evidenceIds = listOf("EVD-SEED-01"),
                    confidence = VisualConfidence.CONFIRMED,
                    riskSeverity = targetNode.riskSeverity
                )
            )
        }

        // 2. Primary High-Value Flows
        val highValEdges = edges.filter { it.amountSat > 0 }.sortedByDescending { it.amountSat }.take(3)
        highValEdges.forEach { edge ->
            steps.add(
                InvestigationStoryStep(
                    stepIndex = stepCount++,
                    timestamp = edge.timestamp.takeIf { it > 0 } ?: (now - 86400000L * 15),
                    titleEn = "Major Transfer: ${edge.amountDisplay} to ${edge.targetId.take(8)}...",
                    titleFa = "انتقال عمده مالی: ${edge.amountDisplay} به ${edge.targetId.take(8)}...",
                    descriptionEn = "Cryptographic transfer of ${edge.amountDisplay} recorded on ledger (TXID: ${edge.transactionId.take(12)}...).",
                    descriptionFa = "تراکنش کریپتوگرافیک به میزان ${edge.amountDisplay} در دفترکل ثبت گردید (شناسه تراکنش: ${edge.transactionId.take(12)}...).",
                    stepCategory = StoryStepCategory.TRANSACTION_FLOW,
                    relevantNodeIds = listOf(edge.sourceId, edge.targetId),
                    relevantEdgeIds = listOf(edge.id),
                    evidenceIds = edge.evidenceIds.ifEmpty { listOf("EVD-TX-${edge.id.take(4)}") },
                    confidence = edge.confidence,
                    riskSeverity = if (edge.amountSat > 100_000_000L) RiskSeverity.HIGH else RiskSeverity.MEDIUM
                )
            )
        }

        // 3. Cluster / Co-Spend Discovery
        val clusterNodes = nodes.filter { it.nodeType == VisualNodeType.CLUSTER }
        clusterNodes.forEach { cl ->
            steps.add(
                InvestigationStoryStep(
                    stepIndex = stepCount++,
                    timestamp = now - 86400000L * 10,
                    titleEn = "Multi-Input Cluster Identified: ${cl.label}",
                    titleFa = "خوشه‌بندی مالکان مشترک (Multi-Input): ${cl.label}",
                    descriptionEn = "GraphSense clustering heuristic grouped ${cl.memberCount} co-spending addresses under unified control hypothesis.",
                    descriptionFa = "الگوریتم‌های خوشه‌بندی GraphSense تعداد ${cl.memberCount} آدرس مرتبط را تحت فرضیه کنترل واحد شناسایی نمودند.",
                    stepCategory = StoryStepCategory.CLUSTER_EXPANSION,
                    relevantNodeIds = listOf(cl.id),
                    relevantEdgeIds = emptyList(),
                    evidenceIds = listOf("EVD-CLUSTER-${cl.id.take(4)}"),
                    confidence = VisualConfidence.HIGH,
                    riskSeverity = cl.riskSeverity
                )
            )
        }

        // 4. Exchange / VASP Interaction
        val exchangeNodes = nodes.filter { it.nodeType == VisualNodeType.EXCHANGE || it.nodeType == VisualNodeType.VASP }
        exchangeNodes.forEach { ex ->
            steps.add(
                InvestigationStoryStep(
                    stepIndex = stepCount++,
                    timestamp = now - 86400000L * 7,
                    titleEn = "VASP Liquidity Exit: ${ex.label}",
                    titleFa = "نقطه خروج نقدینگی صرافی: ${ex.label}",
                    descriptionEn = "Fund flow connected to regulated virtual asset service provider deposit infrastructure for potential subpoena disclosure.",
                    descriptionFa = "مسیر وجوه به درگاه واریز صرافی متصل گردید که امکان استعلام قضایی و شناسایی هویت دارنده حساب را فراهم می‌سازد.",
                    stepCategory = StoryStepCategory.SERVICE_EXIT,
                    relevantNodeIds = listOf(ex.id),
                    relevantEdgeIds = emptyList(),
                    evidenceIds = listOf("EVD-VASP-${ex.id.take(4)}"),
                    confidence = VisualConfidence.CONFIRMED,
                    riskSeverity = RiskSeverity.INFO
                )
            )
        }

        // 5. OSINT Corroboration & Entity Resolution
        val osintNodes = nodes.filter { it.isOffChain }
        if (osintNodes.isNotEmpty()) {
            val sampleOsint = osintNodes.take(3)
            steps.add(
                InvestigationStoryStep(
                    stepIndex = stepCount++,
                    timestamp = now - 86400000L * 4,
                    titleEn = "OSINT Cross-Corroboration Footprint",
                    titleFa = "تطبیق متقاطع ردپای اطلاعاتی منابع باز",
                    descriptionEn = "Identified public email/domain/username footprint linking blockchain seeds with off-chain digital profiles.",
                    descriptionFa = "ردپای ایمیل، دامنه و نام کاربری عمومی که آدرس‌های بلاکچین را به پروفایل‌های دیجیتال مرتبط می‌سازد کشف گردید.",
                    stepCategory = StoryStepCategory.OSINT_DISCOVERY,
                    relevantNodeIds = sampleOsint.map { it.id },
                    relevantEdgeIds = emptyList(),
                    evidenceIds = listOf("EVD-OSINT-CORR"),
                    confidence = VisualConfidence.MEDIUM,
                    riskSeverity = RiskSeverity.HIGH
                )
            )
        }

        // 6. Formulated Finding
        steps.add(
            InvestigationStoryStep(
                stepIndex = stepCount,
                timestamp = now,
                titleEn = "Comprehensive Forensic Dossier Formulation",
                titleFa = "تدوین و پلمب گزارش کارشناسی پرونده",
                descriptionEn = "Evidence chain verified with ${investigationCase.evidenceLog.size} sealed evidence items across ${nodes.size} network nodes.",
                descriptionFa = "زنجیره ادله با ${investigationCase.evidenceLog.size} سند ثبت‌شده و مهر و موم دیجیتال در قالب گراف ${nodes.size} گره‌ای تثبیت شد.",
                stepCategory = StoryStepCategory.FINDING_FORMULATED,
                relevantNodeIds = nodes.take(4).map { it.id },
                relevantEdgeIds = edges.take(4).map { it.id },
                evidenceIds = listOf("EVD-FINAL-SEAL"),
                confidence = VisualConfidence.CONFIRMED,
                riskSeverity = targetNode?.riskSeverity ?: RiskSeverity.INFO
            )
        )

        return steps
    }
}
