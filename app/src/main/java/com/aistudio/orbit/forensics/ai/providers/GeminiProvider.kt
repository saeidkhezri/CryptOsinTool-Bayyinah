package com.aistudio.orbit.forensics.ai.providers

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class GeminiProvider : AiProvider {
    override val name: String = "Google Gemini"
    override val version: String = "v1beta"
    override val defaultModels: List<String> = listOf(
        "gemini-3.5-flash",
        "gemini-3.1-pro-preview"
    )
    override val providerType: AiProviderType = AiProviderType.GEMINI
    override val costCategory: String = "Usage-based"

    private val httpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    override suspend fun testConnection(apiKey: String, endpoint: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) return@withContext false
            val model = GenerativeModel(
                modelName = defaultModels.first(),
                apiKey = apiKey
            )
            val response = model.generateContent("Hello")
            response.text != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun executePrompt(
        prompt: String,
        apiKey: String,
        modelName: String,
        endpoint: String?,
        temperature: Float,
        maxTokens: Int
    ): AiExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            if (apiKey.isBlank()) {
                return@withContext AiExecutionResult(
                    status = AiOutputStatus.AUTH_FAILED,
                    output = null,
                    modelUsed = modelName,
                    provider = providerType,
                    promptVersion = "1.0",
                    executionDurationMs = System.currentTimeMillis() - startTime,
                    errorMessage = "API Key is missing"
                )
            }

            val config = generationConfig {
                this.temperature = temperature
                this.maxOutputTokens = maxTokens
            }
            
            val model = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey,
                generationConfig = config
            )
            
            val response = model.generateContent(prompt)
            val text = response.text
            
            if (text != null) {
                AiExecutionResult(
                    status = AiOutputStatus.SUCCESS,
                    output = text,
                    modelUsed = modelName,
                    provider = providerType,
                    promptVersion = "1.0",
                    executionDurationMs = System.currentTimeMillis() - startTime
                )
            } else {
                AiExecutionResult(
                    status = AiOutputStatus.INVALID_RESPONSE,
                    output = null,
                    modelUsed = modelName,
                    provider = providerType,
                    promptVersion = "1.0",
                    executionDurationMs = System.currentTimeMillis() - startTime,
                    errorMessage = "Empty response from Gemini"
                )
            }
        } catch (e: Exception) {
            val status = when {
                e.message?.contains("API key not valid", ignoreCase = true) == true -> AiOutputStatus.AUTH_FAILED
                e.message?.contains("quota", ignoreCase = true) == true -> AiOutputStatus.RATE_LIMITED
                e.message?.contains("timeout", ignoreCase = true) == true -> AiOutputStatus.TIMEOUT
                else -> AiOutputStatus.UNAVAILABLE
            }
            AiExecutionResult(
                status = status,
                output = null,
                modelUsed = modelName,
                provider = providerType,
                promptVersion = "1.0",
                executionDurationMs = System.currentTimeMillis() - startTime,
                errorMessage = e.message ?: "Unknown execution error"
            )
        }
    }

    /**
     * Executes prompt with Google Search Grounding enabled using gemini-3.5-flash.
     * Retrieves up-to-date live intelligence directly from Google Search.
     */
    suspend fun executeSearchGroundedPrompt(
        prompt: String,
        apiKey: String,
        modelName: String = "gemini-3.5-flash",
        temperature: Float = 0.2f
    ): AiExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (apiKey.isBlank()) {
            return@withContext AiExecutionResult(
                status = AiOutputStatus.AUTH_FAILED,
                output = null,
                modelUsed = modelName,
                provider = providerType,
                promptVersion = "1.0",
                executionDurationMs = System.currentTimeMillis() - startTime,
                errorMessage = "API Key is missing"
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val requestJson = org.json.JSONObject().apply {
                val contentsArray = org.json.JSONArray().apply {
                    val contentObj = org.json.JSONObject().apply {
                        val partsArray = org.json.JSONArray().apply {
                            put(org.json.JSONObject().put("text", prompt))
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val toolsArray = org.json.JSONArray().apply {
                    put(org.json.JSONObject().put("google_search", org.json.JSONObject()))
                }
                put("tools", toolsArray)

                val configObj = org.json.JSONObject().apply {
                    put("temperature", temperature)
                }
                put("generationConfig", configObj)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)
            val request = okhttp3.Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val respString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                // Fallback to standard prompt execution if tool not supported on this endpoint
                return@withContext executePrompt(prompt, apiKey, modelName, temperature = temperature)
            }

            val respObj = org.json.JSONObject(respString)
            val candidates = respObj.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")

                // Extract grounding search metadata if present
                val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
                val searchQueries = groundingMetadata?.optJSONArray("webSearchQueries")
                val searchNotes = if (searchQueries != null && searchQueries.length() > 0) {
                    val qList = (0 until searchQueries.length()).map { searchQueries.getString(it) }
                    "\n[Google Search Grounding Queries: ${qList.joinToString(", ")}]"
                } else ""

                if (!text.isNullOrBlank()) {
                    return@withContext AiExecutionResult(
                        status = AiOutputStatus.SUCCESS,
                        output = text + searchNotes,
                        modelUsed = modelName,
                        provider = providerType,
                        promptVersion = "1.0-grounded",
                        executionDurationMs = System.currentTimeMillis() - startTime
                    )
                }
            }

            // Fallback
            executePrompt(prompt, apiKey, modelName, temperature = temperature)
        } catch (e: Exception) {
            // Fallback to standard executePrompt
            executePrompt(prompt, apiKey, modelName, temperature = temperature)
        }
    }
}
