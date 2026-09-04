package com.aistudio.orbit.provider

import com.aistudio.orbit.model.ProviderStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProviderHealthService(
    private val providerManager: ProviderManager
) {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pollJob: Job? = null

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    fun startPeriodicChecks(intervalMs: Long = 60000L) {
        pollJob?.cancel()
        pollJob = serviceScope.launch {
            while (isActive) {
                checkAllProviders()
                delay(intervalMs)
            }
        }
    }

    fun stopPeriodicChecks() {
        pollJob?.cancel()
        pollJob = null
    }

    suspend fun checkAllProviders() {
        if (_isChecking.value) return
        _isChecking.value = true
        try {
            val configs = providerManager.providerConfigs.value
            configs.forEach { config ->
                if (config.isEnabled && config.status != ProviderStatus.DISABLED) {
                    // This performs a real connection check
                    providerManager.testProvider(config.id)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isChecking.value = false
        }
    }
}
