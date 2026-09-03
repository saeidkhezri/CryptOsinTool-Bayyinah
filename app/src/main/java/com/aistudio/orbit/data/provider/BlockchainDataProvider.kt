package com.aistudio.orbit.data.provider

import com.aistudio.orbit.data.model.BlockchainAddress
import com.aistudio.orbit.data.model.BlockchainType
import com.aistudio.orbit.model.ForensicTransaction

interface BlockchainDataProvider {
    val providerName: String
    val supportedType: BlockchainType

    suspend fun getAddressBalance(address: String): Result<Long>

    suspend fun getTransactions(
        address: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<ForensicTransaction>>

    suspend fun validateAddress(address: String): Boolean
}
