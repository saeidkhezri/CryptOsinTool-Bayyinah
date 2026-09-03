package com.aistudio.orbit.forensics.ai.providers

import com.aistudio.orbit.forensics.ai.search.GroundedSearchSource
import com.aistudio.orbit.forensics.ai.search.YouResearchService
import com.aistudio.orbit.forensics.ai.search.YouSearchQueryBuilder
import com.aistudio.orbit.forensics.ai.search.YouSearchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * YouSearchProvider
 * Official POST-based contract for You.com Search & Deep Research Intelligence.
 * (Prompt 3 §4, Master Instruction §23)
 */
class YouSearchProvider : AiProvider {
    override val name: String = "You.com Search & Research Intelligence"
    override val version: String = "v2-rest"
    override val defaultModels: List<String> = listOf("you-search-v1", "you-research-v1")
    override val providerType: AiProviderType = AiProviderType.YOU_COM
    override val costCategory: String = "Freemium / API Key"

    private val searchService = YouSearchService()
    private val researchService = YouResearchService()

    override suspend fun testConnection(apiKey: String, endpoint: String?): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext false
        try {
            val response = searchService.search(
                apiKey = apiKey,
                request = YouSearchService.SearchRequest(
                    query = "cryptocurrency aml forensics",
                    numWebResults = 1
                )
            )
            response.isSuccess || response.httpCode in 200..204
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
                promptVersion = "2.0",
                executionDurationMs = System.currentTimeMillis() - startTime,
                errorMessage = "You.com API Key is missing or empty"
            )
        }

        // Privacy Filter Sanitization
        val filterResult = YouSearchQueryBuilder.sanitizeAndFilter(prompt)
        val cleanQuery = filterResult.sanitizedQuery

        val isResearchMode = model.contains("research", ignoreCase = true)

        if (isResearchMode) {
            val researchResponse = researchService.conductResearch(
                apiKey = apiKey,
                request = YouResearchService.ResearchRequest(
                    query = cleanQuery,
                    effort = YouResearchService.ResearchEffort.STANDARD
                )
            )

            if (!researchResponse.isSuccess) {
                val status = when (researchResponse.httpCode) {
                    401, 403 -> AiOutputStatus.AUTH_FAILED
                    429 -> AiOutputStatus.RATE_LIMITED
                    else -> AiOutputStatus.UNAVAILABLE
                }
                return@withContext AiExecutionResult(
                    status = status,
                    output = null,
                    modelUsed = model,
                    provider = providerType,
                    promptVersion = "2.0",
                    executionDurationMs = System.currentTimeMillis() - startTime,
                    errorMessage = researchResponse.errorMessage ?: "You.com Research failed"
                )
            }

            val formattedSb = StringBuilder()
            formattedSb.append("### نتایج کاوش عمیق You.com (Deep Research Intelligence)\n\n")

            if (filterResult.warnings.isNotEmpty()) {
                formattedSb.append("⚠️ **ملاحظات حریم خصوصی:** ")
                formattedSb.append(filterResult.warnings.joinToString(" - "))
                formattedSb.append("\n\n")
            }

            if (!researchResponse.summarySynthesis.isNullOrBlank()) {
                formattedSb.append("#### تحلیل ترکیبی اولیه (تولید هوش‌مصنوعی - نیازمند ارزیابی کارشناس):\n")
                formattedSb.append(researchResponse.summarySynthesis)
                formattedSb.append("\n\n")
            }

            formattedSb.append("#### مستندات و منابع شناسایی‌شده (Source Candidates):\n")
            researchResponse.sources.forEachIndexed { i, source ->
                formattedSb.append("${i + 1}. **[${source.title}](${source.url})**\n")
                formattedSb.append("   - دامنه: `${source.domain}` | اعتبار تخمینی: `${(source.relevanceScore * 100).toInt()}%`\n")
                formattedSb.append("   - گزیده: ${source.snippet}\n\n")
            }

            formattedSb.append("---\n*قانون اثبات‌پذیری: خروجی فوق به عنوان «شواهد اولیه (Candidates)» تلقی شده و تا پیش از تایید کارشناس، در گزارش رسمی لحاظ نمی‌گردد.*")

            AiExecutionResult(
                status = AiOutputStatus.SUCCESS,
                output = formattedSb.toString(),
                modelUsed = model,
                provider = providerType,
                promptVersion = "2.0",
                executionDurationMs = System.currentTimeMillis() - startTime
            )
        } else {
            // Search Mode
            val searchResponse = searchService.search(
                apiKey = apiKey,
                request = YouSearchService.SearchRequest(
                    query = cleanQuery,
                    numWebResults = 10
                )
            )

            if (!searchResponse.isSuccess) {
                val status = when (searchResponse.httpCode) {
                    401, 403 -> AiOutputStatus.AUTH_FAILED
                    429 -> AiOutputStatus.RATE_LIMITED
                    else -> AiOutputStatus.UNAVAILABLE
                }
                return@withContext AiExecutionResult(
                    status = status,
                    output = null,
                    modelUsed = model,
                    provider = providerType,
                    promptVersion = "2.0",
                    executionDurationMs = System.currentTimeMillis() - startTime,
                    errorMessage = searchResponse.errorMessage ?: "You.com Search failed"
                )
            }

            val formattedSb = StringBuilder()
            formattedSb.append("### نتایج جستجوی پیشرفته You.com (Search Intelligence)\n\n")

            if (filterResult.warnings.isNotEmpty()) {
                formattedSb.append("⚠️ **ملاحظات حریم خصوصی:** ")
                formattedSb.append(filterResult.warnings.joinToString(" - "))
                formattedSb.append("\n\n")
            }

            if (searchResponse.sources.isNotEmpty()) {
                searchResponse.sources.forEachIndexed { i, source ->
                    formattedSb.append("${i + 1}. **[${source.title}](${source.url})**\n")
                    formattedSb.append("   - منبع: `${source.domain}` | رده: `${source.sourceCategory}`\n")
                    formattedSb.append("   - خلاصه: ${source.snippet}\n\n")
                }
                formattedSb.append("---\n*تمام یافته‌های فوق به عنوان «نامزد مدرک (Evidence Candidate)» ثبت شده و نیازمند بررسی دستی کارشناس پرونده است.*")
            } else {
                formattedSb.append("هیچ یافته‌ای برای کوئری فوق ثبت نشد.\nکوئری: `$cleanQuery`")
            }

            AiExecutionResult(
                status = AiOutputStatus.SUCCESS,
                output = formattedSb.toString(),
                modelUsed = model,
                provider = providerType,
                promptVersion = "2.0",
                executionDurationMs = System.currentTimeMillis() - startTime
            )
        }
    }
}
