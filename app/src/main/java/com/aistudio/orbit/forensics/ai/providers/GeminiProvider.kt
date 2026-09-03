package com.aistudio.orbit.forensics.ai.providers

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiProvider : AiProvider {
    override val name: String = "Google Gemini"
    override val version: String = "v1beta"
    override val defaultModels: List<String> = listOf(
        "gemini-1.5-pro",
        "gemini-1.5-flash",
        "gemini-2.0-flash-exp"
    )
    override val providerType: AiProviderType = AiProviderType.GEMINI
    override val costCategory: String = "Usage-based"

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
}
