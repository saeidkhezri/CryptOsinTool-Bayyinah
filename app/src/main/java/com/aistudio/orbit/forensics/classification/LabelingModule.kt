package com.aistudio.orbit.forensics.classification

import com.aistudio.orbit.model.*
import kotlinx.serialization.Serializable

@Serializable
enum class LabelSourceType(val displayNameEn: String, val displayNameFa: String) {
    OFFICIAL_EXCHANGE_REGISTRY("Official Exchange Disclosure", "اعلامیه و افشای رسمی صرافی"),
    SANCTIONS_GOVERNMENT_LIST("Sanctions / Law Enforcement SDN List", "فهرست تحریم‌ها و مراجع قانونی (OFAC/LE)"),
    OPEN_SOURCE_TAGPACK("GraphSense / Open-Source TagPack", "پک برچسب‌های متن‌باز و GraphSense"),
    BLOCKCHAIN_EXPLORER_LABEL("Verified Explorer Public Tag", "برچسب عمومی کاوشگر بلاک‌چین"),
    COINJOIN_COORDINATOR_SIGNATURE("CoinJoin Coordinator Signature", "امضای سرور کوین‌جوین / میکسر"),
    CIOH_HEURISTIC_CLUSTER("Common-Input Clustering Heuristic", "خوشه‌بندی مالکان مشترک (CIOH)"),
    INVESTIGATOR_MANUAL_NOTE("Investigator Dossier Annotation", "ثبت دستی کارشناس پرونده")
}

@Serializable
data class AddressTag(
    val key: String,
    val valueEn: String,
    val valueFa: String,
    val category: String = "GENERAL"
)

@Serializable
data class EntityLabelRecord(
    val address: String,
    val network: BlockchainNetwork = BlockchainNetwork.BITCOIN,
    val classification: EntityClassificationType,
    val entityNameEn: String,
    val entityNameFa: String,
    val confidenceScore: Float = 1.0f, // 0.0 to 1.0
    val confidenceLevel: ConfidenceLevel = ConfidenceLevel.DEFINITIVE_FACT,
    val sourceType: LabelSourceType = LabelSourceType.INVESTIGATOR_MANUAL_NOTE,
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED_OFFICIAL,
    val tags: List<String> = emptyList(),
    val notesEn: String = "",
    val notesFa: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun localizedEntityName(isPersian: Boolean): String = if (isPersian) entityNameFa else entityNameEn
    fun localizedNotes(isPersian: Boolean): String = if (isPersian) notesFa else notesEn
}

/**
 * LabelingModule
 * Comprehensive address categorization, tagpacks registry, and verification status tracker.
 * Incorporates GraphSense, Maltego transforms, and open-source forensics intelligence patterns.
 */
object LabelingModule {

    // Known curated database of addresses, exchange clusters, mixers, and DeFi protocols
    private val knownEntities: MutableMap<String, EntityLabelRecord> = mutableMapOf(
        // Binance Hot Wallets
        "1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s" to EntityLabelRecord(
            address = "1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s",
            network = BlockchainNetwork.BITCOIN,
            classification = EntityClassificationType.EXCHANGE_HOT_WALLET,
            entityNameEn = "Binance: Hot Wallet 1",
            entityNameFa = "صرافی بایننس: کیف‌پول گرم ۱",
            confidenceScore = 0.98f,
            confidenceLevel = ConfidenceLevel.DEFINITIVE_FACT,
            sourceType = LabelSourceType.OFFICIAL_EXCHANGE_REGISTRY,
            verificationStatus = VerificationStatus.VERIFIED_OFFICIAL,
            tags = listOf("EXCHANGE", "BINANCE", "HOT_WALLET", "CEX"),
            notesEn = "Official high-volume withdrawal hot wallet for Binance exchange.",
            notesFa = "کیف‌پول گرم رسمی صرافی بایننس جهت پردازش برداشت‌های کاربران با حجم بالا."
        ),
        "34xp4vRoCGJym3xR7yCVPFHoCNxv4Twseo" to EntityLabelRecord(
            address = "34xp4vRoCGJym3xR7yCVPFHoCNxv4Twseo",
            network = BlockchainNetwork.BITCOIN,
            classification = EntityClassificationType.EXCHANGE_COLD_WALLET,
            entityNameEn = "Binance: Cold Storage 1",
            entityNameFa = "صرافی بایننس: ذخیره‌سازی سرد ۱",
            confidenceScore = 0.99f,
            confidenceLevel = ConfidenceLevel.DEFINITIVE_FACT,
            sourceType = LabelSourceType.OFFICIAL_EXCHANGE_REGISTRY,
            verificationStatus = VerificationStatus.VERIFIED_OFFICIAL,
            tags = listOf("EXCHANGE", "BINANCE", "COLD_STORAGE", "CUSTODY"),
            notesEn = "Primary multi-sig cold storage reserve vault for Binance.",
            notesFa = "خزانه ذخیره‌سازی سرد چندامضایی اصلی صرافی بایننس."
        ),
        // Bitfinex Hot Wallet
        "bc1qgdjqv0av3q56jvd82tkdjpy7gdp9ut8tlqmgrpmv24sq90ecnvqqjwvw97" to EntityLabelRecord(
            address = "bc1qgdjqv0av3q56jvd82tkdjpy7gdp9ut8tlqmgrpmv24sq90ecnvqqjwvw97",
            network = BlockchainNetwork.BITCOIN,
            classification = EntityClassificationType.EXCHANGE_HOT_WALLET,
            entityNameEn = "Bitfinex: Hot Wallet",
            entityNameFa = "صرافی بیتفینکس: کیف‌پول گرم",
            confidenceScore = 0.95f,
            confidenceLevel = ConfidenceLevel.DEFINITIVE_FACT,
            sourceType = LabelSourceType.BLOCKCHAIN_EXPLORER_LABEL,
            verificationStatus = VerificationStatus.VERIFIED_OFFICIAL,
            tags = listOf("EXCHANGE", "BITFINEX", "HOT_WALLET"),
            notesEn = "Bitfinex native segwit hot wallet cluster.",
            notesFa = "خوشه کیف‌پول گرم سگویت بومی صرافی بیتفینکس."
        ),
        // Wasabi / CoinJoin Coordinator
        "bc1qs657upuk700svvd8992j3lq9u8h2f2v0k7e68a3v5s4h2s4l8q8z3u4j2k" to EntityLabelRecord(
            address = "bc1qs657upuk700svvd8992j3lq9u8h2f2v0k7e68a3v5s4h2s4l8q8z3u4j2k",
            network = BlockchainNetwork.BITCOIN,
            classification = EntityClassificationType.MIXER_TUMBLER,
            entityNameEn = "Wasabi Wallet: CoinJoin Coordinator",
            entityNameFa = "واسابي والت: هماهنگ‌کننده کوین‌جوین",
            confidenceScore = 0.92f,
            confidenceLevel = ConfidenceLevel.HIGH_CONFIDENCE,
            sourceType = LabelSourceType.COINJOIN_COORDINATOR_SIGNATURE,
            verificationStatus = VerificationStatus.CROWDSOURCED_CONFIRMED,
            tags = listOf("MIXER", "COINJOIN", "WASABI", "PRIVACY_PROTOCOL"),
            notesEn = "Centralized WabiSabi / CoinJoin coordinator address distributing equal-denomination privacy rounds.",
            notesFa = "آدرس هماهنگ‌کننده راندهای کوین‌جوین واسابی والت با خروجی‌های هم‌ارز جهت ناشناس‌سازی."
        ),
        // Ethereum Tornado Cash Router (Mixer)
        "0xd90e2f925da726b50c4ed8d0fb90ad053324f31b" to EntityLabelRecord(
            address = "0xd90e2f925da726b50c4ed8d0fb90ad053324f31b",
            network = BlockchainNetwork.ETHEREUM,
            classification = EntityClassificationType.MIXER_TUMBLER,
            entityNameEn = "Tornado.Cash: Router Contract",
            entityNameFa = "قرارداد روتر تورنادو کش (Tornado.Cash)",
            confidenceScore = 1.0f,
            confidenceLevel = ConfidenceLevel.DEFINITIVE_FACT,
            sourceType = LabelSourceType.SANCTIONS_GOVERNMENT_LIST,
            verificationStatus = VerificationStatus.VERIFIED_OFFICIAL,
            tags = listOf("MIXER", "TORNADO_CASH", "OFAC_SANCTIONED", "DEFI_MIXER"),
            notesEn = "Non-custodial cryptographic mixer smart contract flagged on OFAC SDN sanctions list.",
            notesFa = "قرارداد هوشمند میکسر رمزنگاری‌شده در شبکه اتریوم که در فهرست تحریم‌های OFAC قرار دارد."
        ),
        // Uniswap Universal Router (DeFi)
        "0x3fc91a3afd70395cd496c647d5a6cc9d4b2b7fad" to EntityLabelRecord(
            address = "0x3fc91a3afd70395cd496c647d5a6cc9d4b2b7fad",
            network = BlockchainNetwork.ETHEREUM,
            classification = EntityClassificationType.SMART_CONTRACT,
            entityNameEn = "Uniswap: Universal Router",
            entityNameFa = "صرافی غیرمتمرکز یونی‌سواپ (Uniswap Router)",
            confidenceScore = 0.99f,
            confidenceLevel = ConfidenceLevel.DEFINITIVE_FACT,
            sourceType = LabelSourceType.BLOCKCHAIN_EXPLORER_LABEL,
            verificationStatus = VerificationStatus.VERIFIED_OFFICIAL,
            tags = listOf("DEX", "UNISWAP", "DEFI", "SWAP_ROUTER"),
            notesEn = "Primary swap routing smart contract for Uniswap V2 and V3 decentralized exchange.",
            notesFa = "قرارداد هوشمند اصلی مسیریابی سواپ در صرافی غیرمتمرکز یونی‌سواپ نسخه ۲ و ۳."
        ),
        // Tether USD ERC-20 Token Contract
        "0xdac17f958d2ee523a2206206994597c13d831ec7" to EntityLabelRecord(
            address = "0xdac17f958d2ee523a2206206994597c13d831ec7",
            network = BlockchainNetwork.ETHEREUM,
            classification = EntityClassificationType.SMART_CONTRACT,
            entityNameEn = "Tether USD (USDT) Contract",
            entityNameFa = "قرارداد رسمی تتر (USDT ERC-20)",
            confidenceScore = 1.0f,
            confidenceLevel = ConfidenceLevel.DEFINITIVE_FACT,
            sourceType = LabelSourceType.OFFICIAL_EXCHANGE_REGISTRY,
            verificationStatus = VerificationStatus.VERIFIED_OFFICIAL,
            tags = listOf("STABLECOIN", "USDT", "TETHER", "ERC20"),
            notesEn = "Official Tether USD ERC-20 smart contract token ledger.",
            notesFa = "قرارداد هوشمند رسمی صدور و انتقال توکن دلار تتر (USDT) در بستر اتریوم."
        )
    )

    fun getLabel(address: String): EntityLabelRecord? {
        val directMatch = knownEntities[address]
        if (directMatch != null) return directMatch
        for ((knownAddr, record) in knownEntities) {
            if (knownAddr.equals(address, ignoreCase = true)) {
                return record
            }
        }
        return null
    }

    fun getLabelForAddress(address: String, network: BlockchainNetwork): EntityLabelRecord? {
        val directMatch = knownEntities[address]
        if (directMatch != null && directMatch.network == network) return directMatch

        for ((knownAddr, record) in knownEntities) {
            if (knownAddr.equals(address, ignoreCase = true) && record.network == network) {
                return record
            }
        }
        return null
    }

    fun setManualLabel(
        address: String,
        category: EntityClassificationType,
        entityName: String,
        isVerified: Boolean = true,
        notes: String = "",
        network: BlockchainNetwork = BlockchainNetwork.BITCOIN
    ) {
        val record = EntityLabelRecord(
            address = address,
            network = network,
            classification = category,
            entityNameEn = entityName,
            entityNameFa = entityName,
            confidenceScore = 0.99f,
            confidenceLevel = ConfidenceLevel.DEFINITIVE_FACT,
            sourceType = LabelSourceType.INVESTIGATOR_MANUAL_NOTE,
            verificationStatus = if (isVerified) VerificationStatus.VERIFIED_OFFICIAL else VerificationStatus.INVESTIGATOR_ANNOTATED,
            tags = listOf("MANUAL_LABEL", category.name),
            notesEn = notes,
            notesFa = notes
        )
        knownEntities[address] = record
    }

    fun getAllKnownLabels(): List<EntityLabelRecord> = knownEntities.values.toList()

    fun categorizeAddress(
        address: String,
        network: BlockchainNetwork,
        txCount: Int,
        counterpartyCount: Int,
        hasConsolidation: Boolean,
        hasFanOut: Boolean,
        hasEqualOutputs: Boolean = false
    ): EntityLabelRecord {
        val existing = getLabelForAddress(address, network)
        if (existing != null) return existing

        val lower = address.lowercase()

        // 1. Special EVM Burn / Genesis address
        if (lower.startsWith("0x0000000000000000000000000000000000000000") || lower.startsWith("0x000000000000000000000000000000000000dead")) {
            return EntityLabelRecord(
                address = address,
                network = network,
                classification = EntityClassificationType.SMART_CONTRACT,
                entityNameEn = "Null / Burn Address",
                entityNameFa = "آدرس سوزاندن توکن / تهی (Burn Address)",
                confidenceScore = 1.0f,
                confidenceLevel = ConfidenceLevel.DEFINITIVE_FACT,
                sourceType = LabelSourceType.BLOCKCHAIN_EXPLORER_LABEL,
                verificationStatus = VerificationStatus.VERIFIED_OFFICIAL,
                tags = listOf("EVM_NULL", "BURN_ADDRESS"),
                notesEn = "Cryptographically unspendable burn destination.",
                notesFa = "آدرس ابطال و سوزاندن قطعی توکن‌ها در شبکه بلاک‌چین."
            )
        }

        // 2. Wasabi / Whirlpool CoinJoin Signature (Equal outputs + Multi-party)
        if (hasEqualOutputs && txCount > 10) {
            return EntityLabelRecord(
                address = address,
                network = network,
                classification = EntityClassificationType.MIXER_TUMBLER,
                entityNameEn = "CoinJoin / Privacy Cluster",
                entityNameFa = "خوشه حریم‌خصوصی و کوین‌جوین (CoinJoin Cluster)",
                confidenceScore = 0.82f,
                confidenceLevel = ConfidenceLevel.HIGH_CONFIDENCE,
                sourceType = LabelSourceType.COINJOIN_COORDINATOR_SIGNATURE,
                verificationStatus = VerificationStatus.HEURISTIC_CLUSTER,
                tags = listOf("COINJOIN", "EQUAL_OUTPUTS", "PRIVACY_ENHANCED"),
                notesEn = "Repeated equal-denomination output participation conforming to CoinJoin protocol.",
                notesFa = "مشارکت مستمر در تراکنش‌های با خروجی‌های هم‌ارز مطابق با پروتکل‌های کوین‌جوین و واسابی."
            )
        }

        // 3. High-volume Exchange Hot Wallet
        if (txCount > 500 && counterpartyCount > 100) {
            return EntityLabelRecord(
                address = address,
                network = network,
                classification = EntityClassificationType.EXCHANGE_HOT_WALLET,
                entityNameEn = "Centralized Exchange Aggregator / Hot Wallet",
                entityNameFa = "کیف‌پول تجمیع‌کننده صرافی متمرکز (Hot Wallet)",
                confidenceScore = 0.75f,
                confidenceLevel = ConfidenceLevel.MEDIUM_CONFIDENCE,
                sourceType = LabelSourceType.CIOH_HEURISTIC_CLUSTER,
                verificationStatus = VerificationStatus.HEURISTIC_CLUSTER,
                tags = listOf("EXCHANGE_AGGREGATOR", "HIGH_DEGREE", "LIQUIDITY_HUB"),
                notesEn = "Very high degree centrality and massive bi-directional counterparty dispersion.",
                notesFa = "تعداد بسیار بالای تراکنش‌ها و ارتباطات گسترده با صدها طرف‌حساب مختلف حاکی از عملکرد صرافی است."
            )
        }

        // 4. Mining Pool Payout
        if (hasFanOut && txCount > 50 && counterpartyCount > 30) {
            return EntityLabelRecord(
                address = address,
                network = network,
                classification = EntityClassificationType.MINING_POOL,
                entityNameEn = "Mining Pool / High Fan-Out Distributor",
                entityNameFa = "استخر استخراج / توزیع‌کننده چندخروجی",
                confidenceScore = 0.70f,
                confidenceLevel = ConfidenceLevel.MEDIUM_CONFIDENCE,
                sourceType = LabelSourceType.CIOH_HEURISTIC_CLUSTER,
                verificationStatus = VerificationStatus.HEURISTIC_CLUSTER,
                tags = listOf("MINING_POOL", "REWARD_DISTRIBUTION", "BATCH_PAYOUTS"),
                notesEn = "Periodic batch disbursements with high fan-out output structures.",
                notesFa = "توزیع دوره‌ای و دسته‌ای پاداش استخراج میان تعداد زیادی از آدرس‌های ماینرها."
            )
        }

        // 5. Merchant Processor / Sweep Collector
        if (hasConsolidation && txCount > 20) {
            return EntityLabelRecord(
                address = address,
                network = network,
                classification = EntityClassificationType.MERCHANT_PROCESSOR,
                entityNameEn = "Payment Processor / Sweep Collector",
                entityNameFa = "درگاه پرداخت تجاری / آدرس تجمیع وجوه",
                confidenceScore = 0.60f,
                confidenceLevel = ConfidenceLevel.MEDIUM_CONFIDENCE,
                sourceType = LabelSourceType.CIOH_HEURISTIC_CLUSTER,
                verificationStatus = VerificationStatus.HEURISTIC_CLUSTER,
                tags = listOf("PAYMENT_GATEWAY", "SWEEP_NODE"),
                notesEn = "Multi-input fan-in aggregation pattern indicating automated collection sweeps.",
                notesFa = "الگوی تجمیع مداوم ورودی‌های چندگانه نشان‌دهنده درگاه پرداخت یا جمع‌آوری خودکار وجوه است."
            )
        }

        // 6. Default: Unclassified / Insufficient Data (Master Instruction §4)
        return EntityLabelRecord(
            address = address,
            network = network,
            classification = EntityClassificationType.UNKNOWN,
            entityNameEn = "Unclassified / Insufficient Data",
            entityNameFa = "طبقه‌بندی‌نشده / داده ناکافی",
            confidenceScore = 0.0f,
            confidenceLevel = ConfidenceLevel.UNCERTAIN,
            sourceType = LabelSourceType.CIOH_HEURISTIC_CLUSTER,
            verificationStatus = VerificationStatus.HEURISTIC_CLUSTER,
            tags = listOf("UNCLASSIFIED"),
            notesEn = "No verified entity attribution or high-confidence clustering signatures matched.",
            notesFa = "هیچ انتساب قطعی یا امضای خوشه‌بندی با احتمال بالا برای این آدرس شناسایی نشد."
        )
    }
}
