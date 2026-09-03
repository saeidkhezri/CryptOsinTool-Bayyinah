package com.aistudio.orbit.forensics.ai.context

import com.aistudio.orbit.db.AppDatabase
import com.aistudio.orbit.model.InvestigationCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiContextBuilder(
    private val db: AppDatabase
) {
    suspend fun buildCaseSummaryContext(
        caseObj: InvestigationCase,
        osintSessionContext: String? = null,
        graphTopologyContext: String? = null
    ): String = withContext(Dispatchers.IO) {
        val evidenceList = db.evidenceDao().getEvidenceListForCase(caseObj.id)
        val findingsList = db.findingDao().getFindingsListForCase(caseObj.id)
        
        val builder = java.lang.StringBuilder()
        builder.append("Case Objective: ${caseObj.title}\n")
        builder.append("Network: ${caseObj.network}\n")
        builder.append("Target Address: ${caseObj.targetAddress}\n\n")

        builder.append("--- Evidence & Provenance ---\n")
        evidenceList.take(50).forEach { e ->
            builder.append("[${e.evidenceId}] ${e.observation} (Source: ${e.source}, Confidence: ${e.confidence})\n")
        }

        builder.append("\n--- Hypotheses & Findings ---\n")
        findingsList.take(20).forEach { f ->
            builder.append("[${f.findingId}] [${f.category}] [Status: ${f.epistemicStatus}] ${f.title} - ${f.description} (Confidence: ${f.confidenceScore})\n")
            if (f.supportingEvidenceIdsJson.isNotBlank() && f.supportingEvidenceIdsJson != "[]") {
                builder.append("    Supports: ${f.supportingEvidenceIdsJson}\n")
            }
        }
        
        if (osintSessionContext != null) {
            builder.append("\n--- OSINT Intelligence Fusion ---\n")
            builder.append(osintSessionContext).append("\n")
        }
        
        if (graphTopologyContext != null) {
            builder.append("\n--- Graph Topology & Relationships ---\n")
            builder.append(graphTopologyContext).append("\n")
        }
        
        builder.append("\nINSTRUCTIONS FOR AI COPILOT (BAYYINAH MASTER INSTRUCTION 2.0):\n")
        builder.append("1. GROUNDING: You must answer strictly based on the provided evidence, OSINT, graph, and findings.\n")
        builder.append("2. EVIDENCE CITATION: When making any statement or claim, YOU MUST explicitly reference the evidence IDs, e.g. [EVID-123].\n")
        builder.append("3. INSUFFICIENT EVIDENCE: If the evidence is incomplete or missing, explicitly state 'INSUFFICIENT EVIDENCE' / 'شواهد کافی نیست' and specify what data is missing and what next step is recommended.\n")
        builder.append("4. CAUTIOUS FORENSIC LANGUAGE: Use cautious phrasing such as 'نشانه‌هایی مشاهده شد...' ('Indicators were observed...'), 'با الگوی X سازگاری دارد...' ('Is compatible with pattern X...'), 'شواهد موجود از این فرضیه پشتیبانی می‌کنند...' ('Available evidence supports this hypothesis...').\n")
        builder.append("5. ABSOLUTE PROHIBITION: NEVER state 'این فرد مجرم است' ('This person is a criminal') or 'این کیف‌پول متعلق به فرد X است' ('This wallet belongs to Person X') unless definitive primary evidence proves attribution.\n")
        builder.append("6. CERTAINTY LEVELS: Clearly indicate whether a claim is an Observed Fact, Calculated Result, Inference, or Hypothesis.\n")
        builder.append("7. The deterministic forensic engine remains authoritative. You are an investigative assistant copilot.\n")

        builder.toString()
    }
    
    suspend fun buildTransactionContext(caseId: String, txHash: String, additionalContext: String = ""): String = withContext(Dispatchers.IO) {
        val evidenceList = db.evidenceDao().getEvidenceListForCase(caseId).filter { it.observation.contains(txHash, ignoreCase = true) }
        
        val builder = java.lang.StringBuilder()
        builder.append("Transaction Analysis Context for: $txHash\n\n")
        
        if (additionalContext.isNotBlank()) {
            builder.append("Transaction Details:\n$additionalContext\n\n")
        }

        builder.append("--- Related Evidence ---\n")
        if (evidenceList.isEmpty()) {
            builder.append("No explicit evidence mapped to this transaction yet.\n")
        } else {
            evidenceList.forEach { e ->
                builder.append("[${e.evidenceId}] ${e.observation} (Source: ${e.source})\n")
            }
        }

        builder.append("\nINSTRUCTIONS FOR AI:\n")
        builder.append("Explain what happened in this transaction. Use citations [EVID-xxx] if referencing evidence. Do not hallucinate ownership.\n")

        builder.toString()
    }
}
