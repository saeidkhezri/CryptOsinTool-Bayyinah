package com.aistudio.orbit.repository

import android.content.Context
import com.aistudio.orbit.forensics.ai.providers.AiProviderType
import com.aistudio.orbit.security.SecureStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AiProviderConfig(
    val providerType: AiProviderType,
    val enabled: Boolean = false,
    val model: String = "",
    val endpoint: String = "",
    val lastTestSuccess: Boolean? = null,
    val lastTestTimestamp: Long = 0
)

class AiSettingsRepo(context: Context, private val secureStorageManager: SecureStorageManager) {
    private val prefs = context.getSharedPreferences("orbit_ai_settings", Context.MODE_PRIVATE)

    private val _configs = MutableStateFlow<Map<AiProviderType, AiProviderConfig>>(emptyMap())
    val configs: StateFlow<Map<AiProviderType, AiProviderConfig>> = _configs.asStateFlow()

    private val _privacyModeLocalOnly = MutableStateFlow(prefs.getBoolean("privacy_local_only", true))
    val privacyModeLocalOnly: StateFlow<Boolean> = _privacyModeLocalOnly.asStateFlow()

    private val _isSearchEnabled = MutableStateFlow(prefs.getBoolean("ai_search_enabled", true))
    val isSearchEnabled: StateFlow<Boolean> = _isSearchEnabled.asStateFlow()

    init {
        loadConfigs()
    }

    private fun loadConfigs() {
        val jsonString = prefs.getString("provider_configs", "{}")
        try {
            val map = Json.decodeFromString<Map<AiProviderType, AiProviderConfig>>(jsonString ?: "{}")
            _configs.value = map
        } catch (e: Exception) {
            _configs.value = emptyMap()
        }
    }

    private fun saveConfigs(map: Map<AiProviderType, AiProviderConfig>) {
        _configs.value = map
        prefs.edit().putString("provider_configs", Json.encodeToString(map)).apply()
    }

    fun getApiKey(providerType: AiProviderType): String {
        val apiId = when (providerType) {
            AiProviderType.GEMINI -> "google_gemini_ai"
            AiProviderType.OPENAI -> "openai_gpt"
            AiProviderType.ANTHROPIC -> "anthropic_claude"
            AiProviderType.DEEPSEEK -> "deepseek_ai"
            AiProviderType.YOU_COM -> "you_com_search"
            else -> "ai_key_${providerType.name}"
        }
        return secureStorageManager.getApiKeyPrimary(apiId)
    }

    fun updateConfig(config: AiProviderConfig, apiKey: String?) {
        val map = _configs.value.toMutableMap()
        map[config.providerType] = config
        saveConfigs(map)
        
        if (apiKey != null) {
            val apiId = when (config.providerType) {
                AiProviderType.GEMINI -> "google_gemini_ai"
                AiProviderType.OPENAI -> "openai_gpt"
                AiProviderType.ANTHROPIC -> "anthropic_claude"
                AiProviderType.DEEPSEEK -> "deepseek_ai"
                AiProviderType.YOU_COM -> "you_com_search"
                else -> "ai_key_${config.providerType.name}"
            }
            secureStorageManager.saveApiKey(apiId, apiKey, "")
        }
    }

    fun setPrivacyModeLocalOnly(localOnly: Boolean) {
        _privacyModeLocalOnly.value = localOnly
        prefs.edit().putBoolean("privacy_local_only", localOnly).apply()
    }

    fun setSearchEnabled(enabled: Boolean) {
        _isSearchEnabled.value = enabled
        prefs.edit().putBoolean("ai_search_enabled", enabled).apply()
    }
}
