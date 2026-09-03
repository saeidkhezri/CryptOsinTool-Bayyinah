package com.aistudio.orbit.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

sealed class ProviderStateException(message: String, cause: Throwable? = null) : IOException(message, cause) {
    class ProviderUnavailable(val providerName: String, val statusCode: Int, message: String) :
        ProviderStateException("Provider [$providerName] Unavailable ($statusCode): $message")

    class RateLimited(val providerName: String, val retryAfterSeconds: Int, message: String) :
        ProviderStateException("Provider [$providerName] Rate-Limited (429). Retry after ${retryAfterSeconds}s: $message")

    class NetworkTimeout(val providerName: String, message: String) :
        ProviderStateException("Provider [$providerName] Network Timeout: $message")

    class AuthenticationFailed(val providerName: String, message: String) :
        ProviderStateException("Provider [$providerName] Auth Failed (401/403): $message")
}

enum class ProviderHealthStatus {
    HEALTHY,
    DEGRADED,
    RATE_LIMITED,
    UNAVAILABLE,
    AUTHENTICATION_FAILED,
    OFFLINE
}

data class ProviderHealthState(
    val providerName: String,
    val status: ProviderHealthStatus,
    val lastCheckTime: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

object ProviderHealthRegistry {
    private val healthMap = ConcurrentHashMap<String, ProviderHealthState>()

    fun updateStatus(providerName: String, status: ProviderHealthStatus, errorMessage: String? = null) {
        healthMap[providerName] = ProviderHealthState(
            providerName = providerName,
            status = status,
            lastCheckTime = System.currentTimeMillis(),
            errorMessage = errorMessage
        )
    }

    fun getStatus(providerName: String): ProviderHealthState {
        return healthMap[providerName] ?: ProviderHealthState(providerName, ProviderHealthStatus.HEALTHY)
    }

    fun getAllStates(): Map<String, ProviderHealthState> = healthMap.toMap()
}

/**
 * Enterprise Network Interceptor for external blockchain & intelligence providers.
 * Handles timeouts, rate limits (429), server errors (5xx), exponential backoff retries,
 * and reports explicit provider states to ProviderHealthRegistry.
 */
class ProviderNetworkInterceptor(
    private val providerName: String,
    private val maxRetries: Int = 3,
    private val initialBackoffMs: Long = 1000L
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var lastException: Exception? = null
        var attempt = 0

        while (attempt < maxRetries) {
            attempt++
            try {
                response = chain.proceed(request)

                val code = response.code
                if (response.isSuccessful) {
                    ProviderHealthRegistry.updateStatus(providerName, ProviderHealthStatus.HEALTHY)
                    return response
                }

                if (code == 429) {
                    val retryAfter = response.header("Retry-After")?.toIntOrNull() ?: (attempt * 2)
                    val msg = "HTTP 429 Too Many Requests"
                    ProviderHealthRegistry.updateStatus(
                        providerName,
                        ProviderHealthStatus.RATE_LIMITED,
                        msg
                    )

                    if (attempt < maxRetries) {
                        response.close()
                        Thread.sleep((retryAfter * 1000L).coerceAtLeast(initialBackoffMs * attempt))
                        continue
                    } else {
                        throw ProviderStateException.RateLimited(providerName, retryAfter, msg)
                    }
                }

                if (code == 401 || code == 403) {
                    val msg = "HTTP $code Unauthorized / Forbidden"
                    ProviderHealthRegistry.updateStatus(
                        providerName,
                        ProviderHealthStatus.AUTHENTICATION_FAILED,
                        msg
                    )
                    throw ProviderStateException.AuthenticationFailed(providerName, msg)
                }

                if (code in 500..599) {
                    val msg = "HTTP $code Server Error"
                    ProviderHealthRegistry.updateStatus(
                        providerName,
                        ProviderHealthStatus.DEGRADED,
                        msg
                    )

                    if (attempt < maxRetries) {
                        response.close()
                        Thread.sleep(initialBackoffMs * (1L shl (attempt - 1))) // Exponential backoff: 1s, 2s, 4s...
                        continue
                    } else {
                        throw ProviderStateException.ProviderUnavailable(providerName, code, msg)
                    }
                }

                // Other status codes (404, 400, etc.)
                return response

            } catch (e: Exception) {
                lastException = e
                if (e is ProviderStateException) throw e

                val isTimeout = e is SocketTimeoutException
                val isUnknownHost = e is UnknownHostException

                val status = when {
                    isTimeout -> ProviderHealthStatus.DEGRADED
                    isUnknownHost -> ProviderHealthStatus.OFFLINE
                    else -> ProviderHealthStatus.UNAVAILABLE
                }

                ProviderHealthRegistry.updateStatus(
                    providerName = providerName,
                    status = status,
                    errorMessage = e.localizedMessage ?: "Network connection failed"
                )

                if (attempt < maxRetries && isTimeout) {
                    Thread.sleep(initialBackoffMs * attempt)
                } else {
                    if (isTimeout) {
                        throw ProviderStateException.NetworkTimeout(
                            providerName,
                            e.message ?: "Socket connection timed out"
                        )
                    }
                    throw IOException("Network request failed for provider [$providerName]: ${e.localizedMessage}", e)
                }
            }
        }

        return response ?: throw (lastException ?: IOException("Request failed after $maxRetries attempts"))
    }
}
