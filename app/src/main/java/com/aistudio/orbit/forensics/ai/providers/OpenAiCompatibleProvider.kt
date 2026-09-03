package com.aistudio.orbit.forensics.ai.providers

import com.aistudio.orbit.network.ForensicHttpClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

abstract class OpenAiCompatibleProvider : AiProvider {
    
    protected abstract val defaultEndpoint: String

    private val client = ForensicHttpClientFactory.createProviderClient("OpenAiCompatible", connectTimeoutSec = 30, readTimeoutSec = 60)

    override suspend fun testConnection(apiKey: String, endpoint: String?): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext false
        val url = (endpoint?.takeIf { it.isNotBlank() } ?: defaultEndpoint).removeSuffix("/") + "/models"
        
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
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

        val url = (endpoint?.takeIf { it.isNotBlank() } ?: defaultEndpoint).removeSuffix("/") + "/chat/completions"
        
        val payload = JSONObject().apply {
            put("model", modelName)
            put("temperature", temperature)
            put("max_tokens", maxTokens)
            
            val messages = JSONArray()
            messages.put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
            put("messages", messages)
        }

        val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val responseBody = response.body?.string() ?: ""
                
                when (code) {
                    200 -> {
                        val json = JSONObject(responseBody)
                        val choices = json.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content")
                            AiExecutionResult(
                                status = AiOutputStatus.SUCCESS,
                                output = content,
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
                                errorMessage = "No choices in response"
                            )
                        }
                    }
                    401 -> AiExecutionResult(
                        status = AiOutputStatus.AUTH_FAILED,
                        output = null,
                        modelUsed = modelName,
                        provider = providerType,
                        promptVersion = "1.0",
                        executionDurationMs = System.currentTimeMillis() - startTime,
                        errorMessage = "Unauthorized"
                    )
                    429 -> AiExecutionResult(
                        status = AiOutputStatus.RATE_LIMITED,
                        output = null,
                        modelUsed = modelName,
                        provider = providerType,
                        promptVersion = "1.0",
                        executionDurationMs = System.currentTimeMillis() - startTime,
                        errorMessage = "Rate limited"
                    )
                    else -> AiExecutionResult(
                        status = AiOutputStatus.UNAVAILABLE,
                        output = null,
                        modelUsed = modelName,
                        provider = providerType,
                        promptVersion = "1.0",
                        executionDurationMs = System.currentTimeMillis() - startTime,
                        errorMessage = "HTTP $code: $responseBody"
                    )
                }
            }
        } catch (e: Exception) {
            val status = if (e is java.net.SocketTimeoutException) AiOutputStatus.TIMEOUT else AiOutputStatus.UNAVAILABLE
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
}

class OpenAiProvider : OpenAiCompatibleProvider() {
    override val name: String = "OpenAI"
    override val version: String = "v1"
    override val defaultModels: List<String> = listOf("gpt-4o", "gpt-4o-mini", "o1-mini")
    override val providerType: AiProviderType = AiProviderType.OPENAI
    override val costCategory: String = "Usage-based (Paid)"
    override val defaultEndpoint: String = "https://api.openai.com/v1"
}

class DeepSeekProvider : OpenAiCompatibleProvider() {
    override val name: String = "DeepSeek"
    override val version: String = "v1"
    override val defaultModels: List<String> = listOf("deepseek-chat", "deepseek-reasoner")
    override val providerType: AiProviderType = AiProviderType.DEEPSEEK
    override val costCategory: String = "Usage-based (Paid)"
    override val defaultEndpoint: String = "https://api.deepseek.com/v1"
}
