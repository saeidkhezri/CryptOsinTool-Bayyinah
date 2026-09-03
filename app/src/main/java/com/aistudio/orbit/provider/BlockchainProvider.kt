package com.aistudio.orbit.provider

import com.aistudio.orbit.forensics.AddressValidator
import com.aistudio.orbit.model.AddressValidationResult
import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.ForensicTransaction
import kotlinx.serialization.Serializable

@Serializable
data class AddressOverviewDto(
    val address: String,
    val network: BlockchainNetwork,
    val balanceSat: Long,
    val totalReceivedSat: Long,
    val totalSentSat: Long,
    val transactionCount: Int = 0,
    val txCount: Int = transactionCount,
    val unconfirmedBalanceSat: Long = 0,
    val unconfirmedTxCount: Int = 0,
    val providerName: String = "",
    val notes: String = ""
)

interface BlockchainProvider {
    val id: String
    val name: String
    val network: BlockchainNetwork
    val isFree: Boolean get() = isFreeTier
    val isFreeTier: Boolean get() = true
    val requiresApiKey: Boolean
    val rateLimitPerMinute: Int get() = 60

    fun validateAddress(address: String): AddressValidationResult = AddressValidator.validate(address, network)

    suspend fun fetchAddressOverview(address: String): Result<AddressOverviewDto>

    suspend fun fetchTransactions(
        address: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<ForensicTransaction>>

    suspend fun testConnection(): Result<Boolean>
}
