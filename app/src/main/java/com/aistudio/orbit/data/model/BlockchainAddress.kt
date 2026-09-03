package com.aistudio.orbit.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class BlockchainType(val code: String, val displayName: String, val symbol: String) {
    BITCOIN("btc", "Bitcoin", "BTC"),
    ETHEREUM("eth", "Ethereum", "ETH"),
    TRON("trx", "TRON", "TRX"),
    BSC("bnb", "BNB Smart Chain", "BNB"),
    POLYGON("matic", "Polygon", "POL"),
    SOLANA("sol", "Solana", "SOL");

    companion object {
        fun fromString(value: String): BlockchainType {
            return entries.find { 
                it.name.equals(value, ignoreCase = true) || 
                it.code.equals(value, ignoreCase = true) ||
                it.symbol.equals(value, ignoreCase = true)
            } ?: BITCOIN
        }
    }
}

@Serializable
enum class NetworkType(val code: String, val displayName: String) {
    MAINNET("mainnet", "Mainnet (Production)"),
    TESTNET("testnet", "Testnet (Sandbox)"),
    REGTEST("regtest", "Regtest / Local Private");

    companion object {
        fun fromString(value: String): NetworkType {
            return entries.find { 
                it.name.equals(value, ignoreCase = true) || 
                it.code.equals(value, ignoreCase = true) 
            } ?: MAINNET
        }
    }
}

@Serializable
data class BlockchainAddress(
    val address: String,
    val type: BlockchainType,
    val network: NetworkType = NetworkType.MAINNET,
    val label: String? = null,
    val detectedAt: Long = System.currentTimeMillis(),
    val isValid: Boolean = true,
    val validationError: String? = null,
    val isContract: Boolean = false,
    val tagCategory: String? = null
) {
    val displayShort: String
        get() = if (address.length > 14) {
            "${address.take(6)}...${address.takeLast(6)}"
        } else {
            address
        }
}
