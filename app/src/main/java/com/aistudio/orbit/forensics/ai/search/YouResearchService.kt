package com.aistudio.orbit.forensics.ai.search

import com.aistudio.orbit.network.ForensicHttpClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URI
import java.util.UUID

/**
 * YouResearchService
 * Official You.com Deep Research API (Prompt 3 §4, Master Instruction §23)
 * Endpoint: POST https://api.you.com/v1/research
 *
 * Enforces evidence integrity: Research outputs are emitted as Source & Evidence Candidates,
 * requiring explicit investigator review before becoming permanent evidence.
 */
class YouResearchService(
    private val endpointUrl: String = "https://api.you.com/v1/research"
) {
    private val client = ForensicHttpClientFactory.createProviderClient("YouResearch", connectTimeoutSec = 30, readTimeoutSec = 60)
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    enum class ResearchEffort(val apiValue: String, val displayNameFa: String) {
        LITE("lite", "سریع و مقدماتی (Lite)"),
        STANDARD("standard", "استاندارد فارنزیک (Standard)"),
        DEEP("deep", "عمیق تحلیلی (Deep)"),
        EXHAUSTIVE("exhaustive", "جامع چندمنبعی (Exhaustive)"),
        FRONTIER("frontier", "پیشرفته‌ترین حد کاوش (Frontier)")
    }

    data class ResearchRequest(
        val query: String,
        val effort: ResearchEffort = ResearchEffort.STANDARD
    )

    data class ResearchResponse(
        val isSuccess: Boolean,
        val queryUsed: String,
        val summarySynthesis: String?,
        val sources: List<GroundedSearchSource>,
        val candidates: List<SearchEvidenceCandidate>,
        val errorMessage: String? = null,
        val httpCode: Int = 200
    )

    suspend fun conductResearch(apiKey: String, request: ResearchRequest): ResearchResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext ResearchResponse(
                isSuccess = false,
                queryUsed = request.query,
                summarySynthesis = null,
                sources = emptyList(),
                candidates = emptyList(),
                errorMessage = "You.com API Key is missing"
            )
        }

        val bodyJson = JSONObject().apply {
            put("query", request.query)
            put("effort", request.effort.apiValue)
        }

        val httpRequest = Request.Builder()
            .url(endpointUrl)
            .addHeader("X-API-Key", apiKey.trim())
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(bodyJson.toString().toRequestBody(jsonMedia))
            .build()

        try {
            client.newCall(httpRequest).execute().use { response ->
                val code = response.code
                val bodyStr = response.body?.string() ?: ""

                if (response.isSuccessful && code in 200..204) {
                    val json = JSONObject(bodyStr)
                    val synthesis = json.optString("answer", json.optString("content", json.optString("synthesis", "")))
                    
                    val results = json.optJSONArray("sources") 
                        ?: json.optJSONArray("web_results")
                        ?: json.optJSONArray("hits")

                    val sources = mutableListOf<GroundedSearchSource>()
                    val candidates = mutableListOf<SearchEvidenceCandidate>()

                    if (results != null) {
                        for (i in 0 until results.length()) {
                            val res = results.optJSONObject(i) ?: continue
                            val title = res.optString("title", res.optString("name", "Research Reference"))
                            val url = res.optString("url", res.optString("link", ""))
                            val snippet = res.optString("snippet", res.optString("description", res.optString("text", "")))
                            val domain = try {
                                if (url.isNotBlank()) URI(url).host ?: "web" else "web"
                            } catch (e: Exception) {
                                "web"
                            }

                            val source = GroundedSearchSource(
                                title = title,
                                url = url,
                                domain = domain,
                                snippet = snippet,
                                sourceCategory = "Deep Research / OSINT",
                                provider = "You.com Research API (${request.effort.apiValue})",
                                queryUsed = request.query,
                                retrievedAt = System.currentTimeMillis(),
                                relevanceScore = 0.90f - (i * 0.02f).coerceAtLeast(0f)
                            )
                            sources.add(source)

                            candidates.add(
                                SearchEvidenceCandidate(
                                    id = "RES_CAND_${UUID.randomUUID().toString().take(8)}",
                                    claim = if (snippet.isNotBlank()) snippet else title,
                                    source = source,
                                    confidence = (0.85f - (i * 0.02f)).coerceAtLeast(0.40f),
                                    status = EvidenceCandidateStatus.PENDING_REVIEW,
                                    analystNotes = "Deep Research Source from $domain (Effort: ${request.effort.apiValue})"
                                )
                            )
                        }
                    }

                    ResearchResponse(
                        isSuccess = true,
                        queryUsed = request.query,
                        summarySynthesis = synthesis.takeIf { it.isNotBlank() },
                        sources = sources,
                        candidates = candidates,
                        httpCode = code
                    )
                } else {
                    ResearchResponse(
                        isSuccess = false,
                        queryUsed = request.query,
                        summarySynthesis = null,
                        sources = emptyList(),
                        candidates = emptyList(),
                        errorMessage = "You.com Research API returned HTTP $code",
                        httpCode = code
                    )
                }
            }
        } catch (e: Exception) {
            ResearchResponse(
                isSuccess = false,
                queryUsed = request.query,
                summarySynthesis = null,
                sources = emptyList(),
                candidates = emptyList(),
                errorMessage = e.message ?: "Failed to connect to You.com Research API",
                httpCode = 500
            )
        }
    }
}
