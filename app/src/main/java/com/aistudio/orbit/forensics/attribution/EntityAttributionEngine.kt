package com.aistudio.orbit.forensics.attribution

import com.aistudio.orbit.db.TagPackEntity
import com.aistudio.orbit.model.*
import kotlinx.serialization.Serializable

/**
 * Entity Attribution & Conflict Resolution Engine (Master Instruction §13, §15).
 * Preserves all competing attributions.
 * If multiple sources report contradictory labels for an address:
 * Emits CONFLICTING_ATTRIBUTION and allows individual inspection of each source.
 */
object EntityAttributionEngine {

    @Serializable
    enum class AttributionStatus {
        NO_ATTRIBUTION,
        SINGLE_SOURCE_ATTRIBUTION,
        CORROBORATED_ATTRIBUTION,
        CONFLICTING_ATTRIBUTION
    }

    @Serializable
    data class CandidateAttribution(
        val entityName: String,
        val label: String,
        val category: String,
        val source: String,
        val sourceUrl: String,
        val confidence: Float,
        val isSanctioned: Boolean = false,
        val tagpackVersion: String = ""
    )

    @Serializable
    data class AttributionResolutionResult(
        val address: String,
        val status: AttributionStatus,
        val primaryCandidate: CandidateAttribution?,
        val allCandidates: List<CandidateAttribution>,
        val conflictExplanationEn: String?,
        val conflictExplanationFa: String?,
        val epistemicNoteEn: String = "Attribution reflects recorded dataset claims (SOURCE_ATTRIBUTION), not proven legal identity."
    )

    /**
     * Evaluates multiple candidate attribution records and resolves consensus or conflict.
     */
    fun resolveAttribution(
        address: String,
        records: List<TagPackEntity>
    ): AttributionResolutionResult {
        if (records.isEmpty()) {
            return AttributionResolutionResult(
                address = address,
                status = AttributionStatus.NO_ATTRIBUTION,
                primaryCandidate = null,
                allCandidates = emptyList(),
                conflictExplanationEn = null,
                conflictExplanationFa = null
            )
        }

        val candidates = records.map {
            CandidateAttribution(
                entityName = it.entity,
                label = it.label,
                category = it.category,
                source = it.source,
                sourceUrl = it.sourceUrl,
                confidence = it.confidence,
                isSanctioned = it.category.equals("sanction", ignoreCase = true) || it.category.equals("ransomware", ignoreCase = true),
                tagpackVersion = it.tagpackVersion
            )
        }

        // Group by normalized entity name to check for conflicts
        val groupedByEntity = candidates.groupBy { it.entityName.trim().lowercase() }

        if (groupedByEntity.size == 1) {
            val primary = candidates.first()
            val isCorroborated = candidates.size > 1
            return AttributionResolutionResult(
                address = address,
                status = if (isCorroborated) AttributionStatus.CORROBORATED_ATTRIBUTION else AttributionStatus.SINGLE_SOURCE_ATTRIBUTION,
                primaryCandidate = primary,
                allCandidates = candidates,
                conflictExplanationEn = null,
                conflictExplanationFa = null
            )
        }

        // Multiple distinct entities claiming control/association -> CONFLICT
        val entitiesList = groupedByEntity.keys.joinToString(" vs ")
        val conflictEn = "Conflicting attributions detected across independent TagPacks ($entitiesList). Individual dataset claims preserved without unilateral override."
        val conflictFa = "تعارض در انتساب هویت میان چندین منبع شناسایی شد ($entitiesList). ادعاهای تمام پایگاه‌های داده جهت بررسی کارشناسی حفظ شده است."

        return AttributionResolutionResult(
            address = address,
            status = AttributionStatus.CONFLICTING_ATTRIBUTION,
            primaryCandidate = candidates.maxByOrNull { it.confidence },
            allCandidates = candidates,
            conflictExplanationEn = conflictEn,
            conflictExplanationFa = conflictFa
        )
    }
}
