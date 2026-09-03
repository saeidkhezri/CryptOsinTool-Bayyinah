package com.aistudio.orbit.model

import kotlinx.serialization.Serializable

@Serializable
enum class CurrencyPair(val symbol: String, val baseAsset: String, val quoteCurrency: String) {
    BTC_USD("BTC/USD", "BTC", "USD"),
    BTC_TOMAN("BTC/TOMAN", "BTC", "TOMAN"),
    ETH_USD("ETH/USD", "ETH", "USD"),
    ETH_TOMAN("ETH/TOMAN", "ETH", "TOMAN"),
    USDT_USD("USDT/USD", "USDT", "USD"),
    USDT_TOMAN("USDT/TOMAN", "USDT", "TOMAN")
}

@Serializable
enum class PriceRateSource {
    MANUAL_CUSTOM,
    HISTORICAL_ESTIMATE,
    LIVE_EXPLORER
}

@Serializable
data class PriceRateRecord(
    val pair: CurrencyPair,
    val rate: Double,
    val source: PriceRateSource = PriceRateSource.MANUAL_CUSTOM,
    val timestamp: Long = System.currentTimeMillis(),
    val timezone: String = "Asia/Tehran",
    val userCustomLabel: String = "Custom Rate"
)

@Serializable
data class ConvertedValue(
    val cryptoAmount: Double,
    val cryptoSymbol: String,
    val usdValue: Double,
    val tomanValue: Double,
    val appliedBtcUsdRate: Double,
    val appliedUsdTomanRate: Double,
    val isCustomRate: Boolean
)
