package com.aistudio.orbit.forensics.ai.providers

import com.aistudio.orbit.network.ForensicHttpClientFactory
import com.aistudio.orbit.forensics.ai.search.GroundedSearchSource
import com.aistudio.orbit.forensics.ai.search.YouSearchQueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class YouSearchProvider : AiProvider {
    override val name: String = "You.com Search & Research Intelligence"
    override val version: String = "v1"
    override val defaultModels: List<String> = listOf("you-search-v1", "you-research-v1")
    override val providerType: AiProviderType = AiProviderType.YOU_COM
    override val costCategory: String = "Freemium / API Key"

    private val defaultEndpoint = "https://api.ydc-index.io/v1/search"

    private val client = ForensicHttpClientFactory.createProviderClient("YouSearch", connectTimeoutSec = 20, readTimeoutSec = 30)

    override suspend fun testConnection(apiKey: String, endpoint: String?): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext false
        val targetUrl = (endpoint?.takeIf { it.isNotBlank() } ?: defaultEndpoint) + "?query=bitcoin+forensics"

        val request = Request.Builder()
            .url(targetUrl)
            .addHeader("X-API-Key", apiKey)
            .addHeader("Accept", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful || response.code == 200 || response.code == 202
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun executePrompt(
        prompt: String,
        apiKey: String,
        model: String,
        endpoint: String?,
        temperature: Float,
        maxTokens: Int
    ): AiExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (apiKey.isBlank()) {
            return@withContext AiExecutionResult(
                status = AiOutputStatus.AUTH_FAILED,
                output = null,
                modelUsed = model,
                provider = providerType,
                promptVersion = "1.0",
                executionDurationMs = System.currentTimeMillis() - startTime,
                errorMessage = "You.com API Key is missing"
            )
        }

        // Privacy Filter Sanitization
        val filterResult = YouSearchQueryBuilder.sanitizeAndFilter(prompt)
        val cleanQuery = filterResult.sanitizedQuery

        val encodedQuery = try {
            URLEncoder.encode(cleanQuery, "UTF-8")
        } catch (e: Exception) {
            cleanQuery
        }

        val baseUrl = endpoint?.takeIf { it.isNotBlank() } ?: defaultEndpoint
        val fullUrl = "$baseUrl?query=$encodedQuery"

        val request = Request.Builder()
            .url(fullUrl)
            .addHeader("X-API-Key", apiKey)
            .addHeader("Accept", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val body = response.body?.string() ?: ""

                if (code == 200) {
                    val json = JSONObject(body)
                    val hits = json.optJSONArray("hits")
                    val sources = mutableListOf<GroundedSearchSource>()

                    val formattedSb = StringBuilder()
                    formattedSb.append("### نتایج جستجوی پیشرفته و مستند You.com (Search & Research Intelligence)\n\n")

                    if (filterResult.warnings.isNotEmpty()) {
                        formattedSb.append("⚠️ **ملاحظات حریم خصوصی:** ")
                        formattedSb.append(filterResult.warnings.joinToString(" - "))
                        formattedSb.append("\n\n")
                    }

                    if (hits != null && hits.length() > 0) {
                        for (i in 0 until minOf(hits.length(), 10)) {
                            val hit = hits.optJSONObject(i) ?: continue
                            val title = hit.optString("title", "بدون عنوان")
                            val url = hit.optString("url", "")
                            val snippet = hit.optString("snippet", hit.optString("description", ""))
                            val domain = try {
                                java.net.URI(url).host ?: "web"
                            } catch (e: Exception) {
                                "web"
                            }

                            sources.add(
                                GroundedSearchSource(
                                    title = title,
                                    url = url,
                                    domain = domain,
                                    snippet = snippet,
                                    queryUsed = cleanQuery
                                )
                            )

                            formattedSb.append("${i + 1}. **[$title]($url)**\n")
                            formattedSb.append("   - **منبع:** `$domain` | **مستند اولیه (Evidence Candidate)**\n")
                            formattedSb.append("   - **خلاصه یافته:** $snippet\n\n")
                        }

                        formattedSb.append("---\n*تمام نتایج فوق به عنوان «شواهد اولیه تحت بررسی (Evidence Candidates)» ثبت شده و نیازمند تایید ارزیاب پرونده می‌باشند.*")

                        AiExecutionResult(
                            status = AiOutputStatus.SUCCESS,
                            output = formattedSb.toString(),
                            modelUsed = model,
                            provider = providerType,
                            promptVersion = "1.0",
                            executionDurationMs = System.currentTimeMillis() - startTime
                        )
                    } else {
                        AiExecutionResult(
                            status = AiOutputStatus.SUCCESS,
                            output = "هیچ منبع مستند یا یافته‌ای برای کوئری فوق پیدا نشد.\nکوئری فیلترشده: `$cleanQuery`",
                            modelUsed = model,
                            provider = providerType,
                            promptVersion = "1.0",
                            executionDurationMs = System.currentTimeMillis() - startTime
                        )
                    }
                } else if (code == 401 || code == 403) {
                    AiExecutionResult(
                        status = AiOutputStatus.AUTH_FAILED,
                        output = null,
                        modelUsed = model,
                        provider = providerType,
                        promptVersion = "1.0",
                        executionDurationMs = System.currentTimeMillis() - startTime,
                        errorMessage = "کلید API معتبر نیست (HTTP $code)"
                    )
                } else if (code == 429) {
                    AiExecutionResult(
                        status = AiOutputStatus.RATE_LIMITED,
                        output = null,
                        modelUsed = model,
                        provider = providerType,
                        promptVersion = "1.0",
                        executionDurationMs = System.currentTimeMillis() - startTime,
                        errorMessage = "محدودیت تعداد درخواست You.com (Rate Limit)"
                    )
                } else {
                    AiExecutionResult(
                        status = AiOutputStatus.UNAVAILABLE,
                        output = null,
                        modelUsed = model,
                        provider = providerType,
                        promptVersion = "1.0",
                        executionDurationMs = System.currentTimeMillis() - startTime,
                        errorMessage = "خطای سرور You.com: HTTP $code"
                    )
                }
            }
        } catch (e: Exception) {
            val status = if (e is java.net.SocketTimeoutException) AiOutputStatus.TIMEOUT else AiOutputStatus.UNAVAILABLE
            AiExecutionResult(
                status = status,
                output = null,
                modelUsed = model,
                provider = providerType,
                promptVersion = "1.0",
                executionDurationMs = System.currentTimeMillis() - startTime,
                errorMessage = e.message ?: "خطای ارتباط با You.com"
            )
        }
    }
}
