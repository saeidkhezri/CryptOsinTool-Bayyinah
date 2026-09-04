package com.aistudio.orbit.forensics.analysis

import com.aistudio.orbit.model.ConfidenceLevel
import com.aistudio.orbit.model.EvidenceSupportLevel
import com.aistudio.orbit.model.Hypothesis
import com.aistudio.orbit.model.HypothesisEvidenceLink
import com.aistudio.orbit.model.HypothesisReviewState
import java.util.UUID

object HypothesisEngine {

    fun createHypothesis(
        caseId: String,
        investigationId: String,
        title: String,
        description: String,
        confidence: ConfidenceLevel,
        author: String
    ): Hypothesis {
        return Hypothesis(
            id = "HYP_${UUID.randomUUID()}",
            caseId = caseId,
            investigationId = investigationId,
            title = title,
            description = description,
            confidence = confidence,
            createdBy = author,
            updatedBy = author
        )
    }

    fun addEvidenceLink(
        hypothesis: Hypothesis,
        evidenceId: String,
        supportLevel: EvidenceSupportLevel,
        analystNote: String,
        author: String
    ): Hypothesis {
        val updatedLinks = hypothesis.evidenceLinks.toMutableList()
        val existing = updatedLinks.find { it.evidenceId == evidenceId }
        if (existing != null) {
            updatedLinks.remove(existing)
        }
        updatedLinks.add(HypothesisEvidenceLink(evidenceId, supportLevel, analystNote))

        return hypothesis.copy(
            evidenceLinks = updatedLinks,
            updatedBy = author,
            updatedAt = System.currentTimeMillis(),
            version = hypothesis.version + 1
        )
    }

    fun requestReview(hypothesis: Hypothesis, author: String): Hypothesis {
        return hypothesis.copy(
            reviewState = HypothesisReviewState.PENDING_REVIEW,
            updatedBy = author,
            updatedAt = System.currentTimeMillis(),
            version = hypothesis.version + 1
        )
    }

    fun acceptHypothesis(hypothesis: Hypothesis, reviewer: String, reviewerNotes: String): Hypothesis {
        val finalNotes = if (reviewerNotes.isNotBlank()) {
            hypothesis.analystNotes + "\n\nReviewer [$reviewer] accepted: $reviewerNotes"
        } else {
            hypothesis.analystNotes
        }
        return hypothesis.copy(
            reviewState = HypothesisReviewState.ACCEPTED,
            analystNotes = finalNotes,
            updatedBy = reviewer,
            updatedAt = System.currentTimeMillis(),
            version = hypothesis.version + 1
        )
    }

    fun rejectHypothesis(hypothesis: Hypothesis, reviewer: String, reviewerNotes: String): Hypothesis {
        val finalNotes = if (reviewerNotes.isNotBlank()) {
            hypothesis.analystNotes + "\n\nReviewer [$reviewer] rejected: $reviewerNotes"
        } else {
            hypothesis.analystNotes
        }
        return hypothesis.copy(
            reviewState = HypothesisReviewState.REJECTED,
            analystNotes = finalNotes,
            updatedBy = reviewer,
            updatedAt = System.currentTimeMillis(),
            version = hypothesis.version + 1
        )
    }
}
