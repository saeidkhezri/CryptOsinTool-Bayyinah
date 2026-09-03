package com.aistudio.orbit.model

import kotlinx.serialization.Serializable

@Serializable
enum class EntityClassificationType(val displayNameEn: String, val displayNameFa: String) {
    EXCHANGE_HOT_WALLET("Centralized Exchange Hot Wallet", "کیف‌پول گرم صرافی متمرکز"),
    EXCHANGE_COLD_WALLET("Centralized Exchange Cold Storage", "کیف‌پول سرد صرافی متمرکز"),
    EXCHANGE_DEPOSIT("Exchange Deposit Address", "آدرس واریز صرافی"),
    MIXER_TUMBLER("Mixer / CoinJoin Service", "سرویس میکسر / کوین‌جوین"),
    SMART_CONTRACT("Smart Contract / DeFi", "قرارداد هوشمند / پروتکل دیفای"),
    MINING_POOL("Mining Pool Payout", "استخر استخراج"),
    MERCHANT_PROCESSOR("Merchant / Payment Processor", "درگاه پرداخت تجاری"),
    INDIVIDUAL_WALLET("Personal / Unhosted Wallet", "کیف‌پول شخصی (غیرامانی)"),
    HIGH_RISK_ILLICIT("Flagged High-Risk Cluster", "خوشه پرخطر مشکوک"),
    UNKNOWN("Unclassified / Unknown Entity", "نامشخص / ثبت‌نشده")
}

@Serializable
data class AddressClassification(
    val address: String,
    val network: BlockchainNetwork,
    val classification: EntityClassificationType,
    val confidence: ConfidenceLevel,
    val evidenceSource: String,
    val attributionTags: List<String> = emptyList(),
    val knownEntityName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
