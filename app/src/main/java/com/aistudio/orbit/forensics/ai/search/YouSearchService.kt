package com.aistudio.orbit.forensics.ai.search

import com.aistudio.orbit.network.ForensicHttpClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.UUID

/**
 * YouSearchService
 * Official You.com Search API (Prompt 3 §4, Master Instruction §23)
 * Endpoint: POST https://ydc-index.io/v1/search
 */
class YouSearchService(
    private val endpointUrl: String = "https://ydc-index.io/v1/search"
) {
    private val client = ForensicHttpClientFactory.createProviderClient("YouSearch", connectTimeoutSec = 20, readTimeoutSec = 30)
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    data class SearchRequest(
        val query: String,
        val numWebResults: Int = 10,
        val freshness: String? = null, // day, week, month, year
        val domainFilters: List<String> = emptyList()
    )

    data class SearchResponse(
        val isSuccess: Boolean,
        val queryUsed: String,
        val sources: List<GroundedSearchSource>,
        val candidates: List<SearchEvidenceCandidate>,
        val errorMessage: String? = null,
        val httpCode: Int = 200
    )

    suspend fun search(apiKey: String, request: SearchRequest): SearchResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext SearchResponse(
                isSuccess = false,
                queryUsed = request.query,
                sources = emptyList(),
                candidates = emptyList(),
                errorMessage = "You.com API Key is missing or empty"
            )
        }

        val bodyJson = JSONObject().apply {
            put("query", request.query)
            put("num_web_results", request.numWebResults.coerceIn(1, 20))
            request.freshness?.takeIf { it.isNotBlank() }?.let { put("freshness", it) }
            if (request.domainFilters.isNotEmpty()) {
                val domainsArray = JSONArray()
                request.domainFilters.forEach { domainsArray.put(it) }
                put("domain_filters", domainsArray)
            }
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
                    val hits = json.optJSONArray("hits") 
                        ?: json.optJSONObject("data")?.optJSONArray("web_results")
                        ?: json.optJSONArray("web_results")

                    val sources = mutableListOf<GroundedSearchSource>()
                    val candidates = mutableListOf<SearchEvidenceCandidate>()

                    if (hits != null) {
                        for (i in 0 until hits.length()) {
                            val hit = hits.optJSONObject(i) ?: continue
                            val title = hit.optString("title", hit.optString("name", "Web Reference"))
                            val url = hit.optString("url", hit.optString("link", ""))
                            val snippet = hit.optString("snippet", hit.optString("description", ""))
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
                                sourceCategory = "Web / OSINT",
                                provider = "You.com Search API",
                                queryUsed = request.query,
                                retrievedAt = System.currentTimeMillis(),
                                relevanceScore = 0.85f - (i * 0.03f).coerceAtLeast(0f)
                            )
                            sources.add(source)

                            // Formulate Evidence Candidate for investigator review
                            candidates.add(
                                SearchEvidenceCandidate(
                                    id = "CAND_${UUID.randomUUID().toString().take(8)}",
                                    claim = if (snippet.isNotBlank()) snippet else title,
                                    source = source,
                                    confidence = 0.40f, // Search relevance is not evidentiary confidence.
                                    
                                    status = EvidenceCandidateStatus.PENDING_REVIEW,
                                    analystNotes = "Search candidate only. Requires analyst review and source validation before admission as evidence. Domain=$domain"
                                )
                            )
                        }
                    }

                    SearchResponse(
                        isSuccess = true,
                        queryUsed = request.query,
                        sources = sources,
                        candidates = candidates,
                        httpCode = code
                    )
                } else {
                    SearchResponse(
                        isSuccess = false,
                        queryUsed = request.query,
                        sources = emptyList(),
                        candidates = emptyList(),
                        errorMessage = "You.com Search API returned HTTP $code",
                        httpCode = code
                    )
                }
            }
        } catch (e: Exception) {
            SearchResponse(
                isSuccess = false,
                queryUsed = request.query,
                sources = emptyList(),
                candidates = emptyList(),
                errorMessage = e.message ?: "Failed to connect to You.com Search API",
                httpCode = 500
            )
        }
    }
}
