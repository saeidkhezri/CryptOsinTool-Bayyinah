package com.aistudio.orbit.architecture.provider

import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.network.ProviderHealthState
import com.aistudio.orbit.network.ProviderHealthStatus

/**
 * Base Pluggable Provider Interface for all external intelligence and data providers in Bayyinah.
 */
interface PluggableProvider {
    val providerId: String
    val displayName: String
    val isEnabled: Boolean
    val requiresApiKey: Boolean
    
    fun getHealthState(): ProviderHealthState
    suspend fun testConnection(apiKey: String, customEndpoint: String? = null): Boolean
}

/**
 * Clean Interface for Blockchain Data & Infrastructure Providers.
 */
interface BlockchainProvider : PluggableProvider {
    val supportedNetworks: List<BlockchainNetwork>

    suspend fun fetchAddressBalance(address: String, network: BlockchainNetwork): Double?
    suspend fun fetchTransactionCount(address: String, network: BlockchainNetwork): Int?
    suspend fun fetchUtxoCount(address: String, network: BlockchainNetwork): Int?
    suspend fun fetchRawTransactions(address: String, network: BlockchainNetwork, limit: Int = 50): List<Any>
}

/**
 * Clean Interface for Web Search & Deep AI Intelligence Providers (e.g. You.com).
 */
interface SearchProvider : PluggableProvider {
    suspend fun performForensicSearch(
        rawQuery: String,
        domainFilter: String? = null,
        maxResults: Int = 10
    ): SearchResultData
}

data class SearchResultData(
    val query: String,
    val providerId: String,
    val totalCount: Int,
    val results: List<SearchItem>,
    val timestamp: Long = System.currentTimeMillis()
)

data class SearchItem(
    val title: String,
    val url: String,
    val snippet: String,
    val domain: String,
    val publishedDate: String? = null
)

/**
 * Clean Interface for OSINT & Identity Enrichment Providers (e.g. NumVerify, Holehe).
 */
interface OSINTProvider : PluggableProvider {
    suspend fun queryIdentifier(identifier: String, identifierType: String): Map<String, Any?>
}

/**
 * Global Registry for Pluggable Providers to allow runtime swapping and fallback routing.
 */
object PluggableProviderRegistry {
    private val blockchainProviders = mutableMapOf<String, BlockchainProvider>()
    private val searchProviders = mutableMapOf<String, SearchProvider>()
    private val osintProviders = mutableMapOf<String, OSINTProvider>()

    fun registerBlockchainProvider(provider: BlockchainProvider) {
        blockchainProviders[provider.providerId] = provider
    }

    fun registerSearchProvider(provider: SearchProvider) {
        searchProviders[provider.providerId] = provider
    }

    fun registerOsintProvider(provider: OSINTProvider) {
        osintProviders[provider.providerId] = provider
    }

    fun getBlockchainProvider(id: String): BlockchainProvider? = blockchainProviders[id]
    fun getSearchProvider(id: String): SearchProvider? = searchProviders[id]
    fun getOsintProvider(id: String): OSINTProvider? = osintProviders[id]

    fun getAllBlockchainProviders(): List<BlockchainProvider> = blockchainProviders.values.toList()
    fun getAllSearchProviders(): List<SearchProvider> = searchProviders.values.toList()
    fun getAllOsintProviders(): List<OSINTProvider> = osintProviders.values.toList()
}
