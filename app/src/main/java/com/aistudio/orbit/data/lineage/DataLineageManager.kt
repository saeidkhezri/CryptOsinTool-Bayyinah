package com.aistudio.orbit.data.lineage

import com.aistudio.orbit.db.DataLineageStage
import com.aistudio.orbit.db.EpistemicStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Data Lineage Manager (Master Instruction §31, Prompt 3 §2)
 *
 * Enforces strict epistemic progression:
 * RAW (Unmodified network/ledger/OSINT payloads)
 * -> NORMALIZED (Parsed into standard forensic models)
 * -> DERIVED (Calculated balances, counterparties, UTXO graphs)
 * -> ANALYTICAL (Clustering heuristics, pattern matching, risk scoring)
 * -> REVIEWED (Human investigator assessment, hypothesis testing)
 * -> FINALIZED (Immutable evidentiary records, signed reports)
 */
object DataLineageManager {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Validates whether a state transition follows valid unidirectional forensic lineage.
     */
    fun isValidTransition(from: DataLineageStage, to: DataLineageStage): Boolean {
        if (from == to) return true
        return when (from) {
            DataLineageStage.RAW -> to == DataLineageStage.NORMALIZED
            DataLineageStage.NORMALIZED -> to == DataLineageStage.DERIVED || to == DataLineageStage.ANALYTICAL
            DataLineageStage.DERIVED -> to == DataLineageStage.ANALYTICAL || to == DataLineageStage.REVIEWED
            DataLineageStage.ANALYTICAL -> to == DataLineageStage.REVIEWED
            DataLineageStage.REVIEWED -> to == DataLineageStage.FINALIZED
            DataLineageStage.FINALIZED -> false // Finalized records are immutable
        }
    }

    /**
     * Serializes parent input identifiers to preserve data provenance.
     */
    fun buildParentInputsJson(parentIds: List<String>): String {
        return json.encodeToString(parentIds.filter { it.isNotBlank() }.distinct())
    }

    /**
     * Deserializes parent input identifiers.
     */
    fun parseParentInputs(jsonString: String): List<String> {
        return try {
            json.decodeFromString<List<String>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Determines default epistemic status from a given lineage stage.
     */
    fun defaultEpistemicStatusForStage(stage: DataLineageStage): EpistemicStatus {
        return when (stage) {
            DataLineageStage.RAW -> EpistemicStatus.OBSERVED_FACT
            DataLineageStage.NORMALIZED -> EpistemicStatus.OBSERVED_FACT
            DataLineageStage.DERIVED -> EpistemicStatus.DERIVED_CALCULATION
            DataLineageStage.ANALYTICAL -> EpistemicStatus.ANALYTICAL_INFERENCE
            DataLineageStage.REVIEWED -> EpistemicStatus.INVESTIGATOR_ASSESSMENT
            DataLineageStage.FINALIZED -> EpistemicStatus.FACT
        }
    }

    /**
     * Formats Persian badge for data provenance in the UI.
     */
    fun getStageLabelFa(stage: DataLineageStage): String {
        return when (stage) {
            DataLineageStage.RAW -> "داده خام (RAW)"
            DataLineageStage.NORMALIZED -> "استانداردشده (NORMALIZED)"
            DataLineageStage.DERIVED -> "محاسبه‌شده (DERIVED)"
            DataLineageStage.ANALYTICAL -> "تحلیل تحلیلی (ANALYTICAL)"
            DataLineageStage.REVIEWED -> "ارزیابی کارشناس (REVIEWED)"
            DataLineageStage.FINALIZED -> "نهایی و تثبیت‌شده (FINALIZED)"
        }
    }
}
