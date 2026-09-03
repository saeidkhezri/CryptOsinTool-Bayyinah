package com.aistudio.orbit.provider

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

/**
 * Model representing a provider or dataset error caught by the Global Error Boundary.
 */
data class ProviderFailureState(
    val providerId: String,
    val providerNameFa: String,
    val providerNameEn: String,
    val errorMessage: String,
    val missingDataImpactFa: String,
    val missingDataImpactEn: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRecoverable: Boolean = true
)

/**
 * Global Error Boundary for Network and Data Providers in Bayyinah Platform.
 * Ensures that any single failing API call or dataset fetch never crashes the app,
 * and instead transitions gracefully to a 'Partial Result' state with full transparency.
 */
object ProviderErrorBoundary {

    private val _providerFailures = MutableStateFlow<List<ProviderFailureState>>(emptyList())
    val providerFailures: StateFlow<List<ProviderFailureState>> = _providerFailures.asStateFlow()

    private val _isPartialResult = MutableStateFlow(false)
    val isPartialResult: StateFlow<Boolean> = _isPartialResult.asStateFlow()

    fun recordFailure(
        providerId: String,
        providerNameFa: String,
        providerNameEn: String,
        error: Throwable,
        impactFa: String = "برخی شاخص‌های تکمیلی در دسترس نیستند، اما تحلیل‌های محلی و دفترکل با موفقیت انجام شد.",
        impactEn: String = "Some supplementary indicators are unavailable, but local and ledger analytics completed successfully."
    ) {
        val failure = ProviderFailureState(
            providerId = providerId,
            providerNameFa = providerNameFa,
            providerNameEn = providerNameEn,
            errorMessage = error.localizedMessage ?: error.message ?: "Network or dataset timeout",
            missingDataImpactFa = impactFa,
            missingDataImpactEn = impactEn
        )
        val current = _providerFailures.value.toMutableList()
        current.removeAll { it.providerId == providerId }
        current.add(0, failure)
        _providerFailures.value = current
        _isPartialResult.value = true
    }

    fun clearFailures() {
        _providerFailures.value = emptyList()
        _isPartialResult.value = false
    }

    fun removeFailure(providerId: String) {
        val current = _providerFailures.value.filterNot { it.providerId == providerId }
        _providerFailures.value = current
        _isPartialResult.value = current.isNotEmpty()
    }

    /**
     * Executes a suspend provider function within the global error boundary.
     * Returns fallbackValue if an exception occurs, recording the failure for partial result state.
     */
    suspend fun <T> executeWithFallback(
        providerId: String,
        providerNameFa: String,
        providerNameEn: String,
        fallbackValue: T,
        impactFa: String = "اطلاعات این بخش از منبع اصلی دریافت نشد و نتیجه به‌صورت جزئی ثبت گردید.",
        impactEn: String = "Data for this module was not retrieved from the primary source; result marked as partial.",
        block: suspend () -> T
    ): T {
        return try {
            val result = block()
            // If successful, clear previous failure for this provider
            removeFailure(providerId)
            result
        } catch (e: Exception) {
            recordFailure(
                providerId = providerId,
                providerNameFa = providerNameFa,
                providerNameEn = providerNameEn,
                error = e,
                impactFa = impactFa,
                impactEn = impactEn
            )
            fallbackValue
        }
    }
}
