package com.aistudio.orbit.data.provider

import com.aistudio.orbit.data.model.BlockchainType
import com.aistudio.orbit.forensics.AddressValidator
import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.ForensicTransaction
import com.aistudio.orbit.provider.BitcoinBlockchainInfoProvider
import com.aistudio.orbit.provider.BitcoinMempoolProvider

class BitcoinProvider(
    private val mempoolProvider: BitcoinMempoolProvider = BitcoinMempoolProvider(),
    private val blockchainInfoProvider: BitcoinBlockchainInfoProvider = BitcoinBlockchainInfoProvider()
) : BlockchainDataProvider {

    override val providerName: String = "Bitcoin Multi-Source Forensic Adapter"
    override val supportedType: BlockchainType = BlockchainType.BITCOIN

    override suspend fun getAddressBalance(address: String): Result<Long> {
        // Attempt primary node via Mempool.space
        val primaryOverview = mempoolProvider.fetchAddressOverview(address)
        if (primaryOverview.isSuccess) {
            val balance = primaryOverview.getOrNull()?.balanceSat ?: 0L
            return Result.success(balance)
        }

        // Fallback to secondary node via Blockchain.info
        val secondaryOverview = blockchainInfoProvider.fetchAddressOverview(address)
        if (secondaryOverview.isSuccess) {
            val balance = secondaryOverview.getOrNull()?.balanceSat ?: 0L
            return Result.success(balance)
        }

        return Result.failure(
            primaryOverview.exceptionOrNull() 
                ?: secondaryOverview.exceptionOrNull() 
                ?: Exception("Could not fetch Bitcoin balance for address $address")
        )
    }

    override suspend fun getTransactions(
        address: String,
        limit: Int,
        offset: Int
    ): Result<List<ForensicTransaction>> {
        // Primary lookup
        val primaryTxs = mempoolProvider.fetchTransactions(address, limit, offset)
        if (primaryTxs.isSuccess && primaryTxs.getOrNull()?.isNotEmpty() == true) {
            return primaryTxs
        }

        // Fallback lookup
        val fallbackTxs = blockchainInfoProvider.fetchTransactions(address, limit, offset)
        if (fallbackTxs.isSuccess) {
            return fallbackTxs
        }

        return if (primaryTxs.isSuccess) {
            primaryTxs
        } else {
            Result.failure(
                primaryTxs.exceptionOrNull() ?: Exception("Transaction discovery failed for $address")
            )
        }
    }

    override suspend fun validateAddress(address: String): Boolean {
        return AddressValidator.validate(address, BlockchainNetwork.BITCOIN).isValid
    }
}
