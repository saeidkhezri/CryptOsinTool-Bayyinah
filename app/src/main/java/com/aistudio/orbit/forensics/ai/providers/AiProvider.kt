package com.aistudio.orbit.forensics.ai.providers

import kotlinx.serialization.Serializable

@Serializable
enum class AiProviderType {
    GEMINI,
    OPENAI,
    ANTHROPIC,
    DEEPSEEK,
    YOU_COM,
    OTHER
}

@Serializable
enum class AiOutputStatus {
    SUCCESS,
    TIMEOUT,
    AUTH_FAILED,
    RATE_LIMITED,
    UNAVAILABLE,
    INVALID_RESPONSE
}

@Serializable
data class AiExecutionResult(
    val status: AiOutputStatus,
    val output: String?,
    val modelUsed: String,
    val provider: AiProviderType,
    val promptVersion: String,
    val executionDurationMs: Long,
    val errorMessage: String? = null
)

interface AiProvider {
    val name: String
    val version: String
    val defaultModels: List<String>
    val providerType: AiProviderType
    val costCategory: String
    
    suspend fun testConnection(apiKey: String, endpoint: String? = null): Boolean
    
    suspend fun executePrompt(
        prompt: String,
        apiKey: String,
        model: String,
        endpoint: String? = null,
        temperature: Float = 0.3f,
        maxTokens: Int = 2048
    ): AiExecutionResult
}
