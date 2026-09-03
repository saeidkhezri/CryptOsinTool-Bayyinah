package com.aistudio.orbit.forensics.osint.bridge

import com.aistudio.orbit.db.*
import com.aistudio.orbit.forensics.correlation.CorrelationFinding
import com.aistudio.orbit.forensics.osint.bus.IndicatorType
import com.aistudio.orbit.forensics.osint.bus.OsintEvent
import com.aistudio.orbit.model.*
import org.json.JSONArray
import java.security.MessageDigest

/**
 * OSINT to Evidence, Graph, and Finding Bridge (Master Instruction §34, §35, §36).
 * Safely converts verified OSINT events and correlation findings into:
 * 1. Immutable Evidence records (with SHA-256 hash and provenance).
 * 2. Case Graph nodes and edges (Address -> Entity -> Domain -> IP -> ASN).
 * 3. Candidate Findings for investigator review and confirmation.
 */
object OsintForensicBridges {

    /**
     * Converts an accepted OSINT Event into an immutable Room Evidence Entity (Master Instruction §34).
     */
    fun eventToEvidence(
        event: OsintEvent,
        caseId: String,
        investigatorNote: String = ""
    ): EvidenceEntity {
        val payloadHash = sha256("${event.source}:${event.indicatorType}:${event.normalizedValue}:${event.observedAt}")
        val claim = "Discovered ${event.indicatorType.displayNameEn} [${event.indicatorValue}] via ${event.source}."

        return EvidenceEntity(
            evidenceId = "EVD_${event.eventId.removePrefix("EVT_")}",
            caseId = caseId,
            investigationId = event.investigationId,
            observation = event.provenance.ifBlank { "Observed ${event.indicatorValue} via ${event.source}" },
            claim = claim,
            source = event.source,
            sourceUrl = "",
            sourceType = "OSINT_DISCOVERY",
            retrievedAt = event.createdAt,
            observedAt = event.observedAt,
            blockchain = if (event.indicatorType == IndicatorType.ADDRESS) "BITCOIN" else "OFF_CHAIN_OSINT",
            address = if (event.indicatorType == IndicatorType.ADDRESS) event.normalizedValue else null,
            transactionHash = if (event.indicatorType == IndicatorType.TXID) event.normalizedValue else null,
            entityName = if (event.indicatorType == IndicatorType.ENTITY || event.indicatorType == IndicatorType.ORGANIZATION) event.normalizedValue else null,
            method = "Automated Reconnaissance / Adapter Query",
            confidence = event.confidence,
            epistemicStatus = event.epistemicStatus,
            analystNote = investigatorNote,
            hash = payloadHash,
            isFinalized = false,
            metadataJson = event.payloadJson
        )
    }

    /**
     * Converts a Correlation Finding into a Candidate Investigation Finding Entity (Master Instruction §36).
     */
    fun correlationToFinding(
        correlation: CorrelationFinding,
        caseId: String
    ): FindingEntity {
        return FindingEntity(
            findingId = "FIND_${correlation.correlationId.removePrefix("CORR_")}",
            caseId = caseId,
            title = correlation.titleEn,
            titleFa = correlation.titleFa,
            description = "${correlation.descriptionEn} (Composite Confidence: ${(correlation.compositeConfidence * 100).toInt()}%)",
            descriptionFa = "${correlation.descriptionFa} (ضریب اطمینان ترکیبی: ${(correlation.compositeConfidence * 100).toInt()}٪)",
            severity = if (correlation.compositeConfidence > 0.85f) "HIGH" else "MEDIUM",
            category = correlation.ruleType.name,
            epistemicStatus = correlation.epistemicStatus,
            confidenceScore = correlation.compositeConfidence,
            supportingEvidenceIdsJson = JSONArray(correlation.linkedEventIds).toString(),
            recommendedAction = "Review participating intelligence sources (${correlation.participatingSources.joinToString(", ")}) and confirm lead."
        )
    }

    /**
     * Enriches a case graph with OSINT nodes and provenance-backed edges (Master Instruction §35).
     */
    fun mapEventsToGraphElements(
        events: List<OsintEvent>,
        baseAddress: String
    ): Pair<List<InteractiveCaseNode>, List<InteractiveCaseEdge>> {
        val nodes = mutableListOf<InteractiveCaseNode>()
        val edges = mutableListOf<InteractiveCaseEdge>()

        for (event in events) {
            val nodeId = "NODE_${event.indicatorType.name}_${event.normalizedValue.hashCode()}"
            val category = when (event.indicatorType) {
                IndicatorType.ADDRESS -> ForensicEntityCategory.BLOCKCHAIN_ADDRESS
                IndicatorType.TXID -> ForensicEntityCategory.TRANSACTION
                IndicatorType.DOMAIN -> ForensicEntityCategory.DOMAIN_NAME
                IndicatorType.IP -> ForensicEntityCategory.IP_NETWORK_NODE
                IndicatorType.ASN -> ForensicEntityCategory.IP_NETWORK_NODE
                IndicatorType.EMAIL -> ForensicEntityCategory.EMAIL_ADDRESS
                IndicatorType.USERNAME -> ForensicEntityCategory.ALIAS_USERNAME
                IndicatorType.SOCIAL_PROFILE -> ForensicEntityCategory.SOCIAL_ACCOUNT
                IndicatorType.ENTITY, IndicatorType.ORGANIZATION -> ForensicEntityCategory.PUBLIC_ORGANIZATION
                IndicatorType.SERVICE -> ForensicEntityCategory.PUBLIC_SERVICE
                IndicatorType.CERTIFICATE -> ForensicEntityCategory.IP_NETWORK_NODE
                else -> ForensicEntityCategory.ALIAS_USERNAME
            }

            val epistemic = when (event.epistemicStatus) {
                EpistemicStatus.FACT -> ForensicEpistemicStatus.UNDISPUTED_LEDGER_FACT
                EpistemicStatus.INFERENCE -> ForensicEpistemicStatus.DERIVED_OSINT_INFERENCE
                EpistemicStatus.HYPOTHESIS -> ForensicEpistemicStatus.WORKING_HYPOTHESIS
                EpistemicStatus.UNKNOWN -> ForensicEpistemicStatus.DERIVED_OSINT_INFERENCE
            }

            nodes.add(
                InteractiveCaseNode(
                    id = nodeId,
                    label = "${event.indicatorType.name}: ${event.indicatorValue.take(24)}",
                    entityCategory = category,
                    riskSeverity = if (event.confidence > 0.8f) RiskSeverity.MEDIUM else RiskSeverity.INFO,
                    epistemicStatus = epistemic,
                    confidencePercent = (event.confidence * 100).toInt(),
                    tags = listOf(event.source)
                )
            )

            val edgeCategory = when (event.indicatorType) {
                IndicatorType.DOMAIN -> ForensicEdgeCategory.PUBLICLY_ASSOCIATED_WITH
                IndicatorType.IP -> ForensicEdgeCategory.NETWORK_PROPAGATION
                IndicatorType.ASN -> ForensicEdgeCategory.NETWORK_PROPAGATION
                IndicatorType.ENTITY -> ForensicEdgeCategory.ATTRIBUTED_BY_SOURCE
                IndicatorType.SERVICE -> ForensicEdgeCategory.PUBLICLY_ASSOCIATED_WITH
                IndicatorType.SOCIAL_PROFILE, IndicatorType.USERNAME -> ForensicEdgeCategory.TRANSFORM_ATTRIBUTION
                else -> ForensicEdgeCategory.TRANSFORM_ATTRIBUTION
            }

            edges.add(
                InteractiveCaseEdge(
                    id = "EDGE_${baseAddress.hashCode()}_${nodeId.hashCode()}",
                    sourceId = baseAddress,
                    targetId = nodeId,
                    category = edgeCategory,
                    confidencePercent = (event.confidence * 100).toInt(),
                    transformLabel = event.source
                )
            )
        }

        return Pair(nodes, edges)
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
