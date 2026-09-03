package com.aistudio.orbit.forensics.osint.identity

import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * OPSEC, User-Agent Rotation, and Proxy Management for Stealth OSINT Inquiries.
 * Prevents IP bans, cloudflare challenges, and target notification alerts.
 */
object OpsecProxyManager {

    private val USER_AGENTS = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3 Safari/605.1.15",
        "Mozilla/5.0 (X11; Linux x86_64; rv:123.0) Gecko/20100101 Firefox/123.0",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/122.0.6261.62 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.64 Mobile Safari/537.36"
    )

    private val proxyIndex = AtomicInteger(0)
    private val configuredProxies = mutableListOf<ProxyConfig>()

    data class ProxyConfig(
        val host: String,
        val port: Int,
        val type: Proxy.Type = Proxy.Type.HTTP,
        val isHealthy: Boolean = true,
        val latencyMs: Long = 0L
    )

    fun getRandomUserAgent(): String {
        return USER_AGENTS.random()
    }

    fun addProxy(host: String, port: Int, type: Proxy.Type = Proxy.Type.HTTP) {
        configuredProxies.add(ProxyConfig(host, port, type))
    }

    fun getNextProxy(): Proxy? {
        if (configuredProxies.isEmpty()) return null
        val healthyList = configuredProxies.filter { it.isHealthy }
        if (healthyList.isEmpty()) return null
        val idx = proxyIndex.getAndIncrement() % healthyList.size
        val config = healthyList[idx]
        return Proxy(config.type, InetSocketAddress(config.host, config.port))
    }

    /**
     * Builds a stealth OkHttpClient with configured timeouts, proxy rotation, and redirect safety.
     */
    fun createStealthHttpClient(useProxyIfAvailable: Boolean = true): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)

        if (useProxyIfAvailable) {
            val proxy = getNextProxy()
            if (proxy != null) {
                builder.proxy(proxy)
            }
        }
        return builder.build()
    }

    /**
     * Executes an HTTP request with exponential backoff if 429 Too Many Requests is encountered.
     */
    suspend fun executeWithBackoff(
        client: OkHttpClient,
        requestBuilder: Request.Builder,
        maxRetries: Int = 3
    ): okhttp3.Response? {
        var currentAttempt = 0
        var backoffMs = 1500L

        while (currentAttempt < maxRetries) {
            currentAttempt++
            try {
                val request = requestBuilder
                    .header("User-Agent", getRandomUserAgent())
                    .header("Accept-Language", "en-US,en;q=0.9,fa;q=0.8")
                    .header("Sec-Ch-Ua-Mobile", "?0")
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-origin")
                    .build()

                val response = client.newCall(request).execute()
                if (response.code == 429) {
                    response.close()
                    delay(backoffMs)
                    backoffMs *= 2
                    continue
                }
                return response
            } catch (e: Exception) {
                if (currentAttempt >= maxRetries) return null
                delay(backoffMs)
                backoffMs *= 2
            }
        }
        return null
    }
}
