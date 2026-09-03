package com.aistudio.orbit.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Centralized OkHttpClient Factory for Bayyinah providers.
 * Ensures consistent timeouts, interceptors, error handling, and proxy/TLS security.
 */
object ForensicHttpClientFactory {

    private val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Builds a provider-specific OkHttpClient equipped with ProviderNetworkInterceptor.
     */
    fun createProviderClient(
        providerName: String,
        connectTimeoutSec: Long = 15,
        readTimeoutSec: Long = 30,
        maxRetries: Int = 3
    ): OkHttpClient {
        return baseClient.newBuilder()
            .connectTimeout(connectTimeoutSec, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSec, TimeUnit.SECONDS)
            .addInterceptor(ProviderNetworkInterceptor(providerName = providerName, maxRetries = maxRetries))
            .build()
    }

    /**
     * Returns a general shared client.
     */
    fun getSharedClient(): OkHttpClient = baseClient
}
