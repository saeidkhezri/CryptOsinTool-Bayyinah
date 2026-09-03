package com.aistudio.orbit.provider

import com.aistudio.orbit.model.ApiProviderConfig
import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.ForensicTransaction
import com.aistudio.orbit.model.ProviderStatus
import com.aistudio.orbit.security.SecureStorageManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore

class ProviderManager(
    private var mempoolProvider: BitcoinMempoolProvider = BitcoinMempoolProvider(),
    private var blockchainInfoProvider: BitcoinBlockchainInfoProvider = BitcoinBlockchainInfoProvider(),
    private var etherscanProvider: EthereumEtherscanProvider = EthereumEtherscanProvider(),
    private var tronGridProvider: TronGridProvider = TronGridProvider(),
    private var bscScanProvider: BscScanProvider = BscScanProvider(),
    private var secureStorageManager: SecureStorageManager? = null
) {
    private var cryptoApisProvider: CryptoApisProvider = CryptoApisProvider(secureStorageManager)
    private var numVerifyProvider: NumVerifyProvider = NumVerifyProvider(secureStorageManager)
    private var breadcrumbsProvider: BreadcrumbsProvider = BreadcrumbsProvider(secureStorageManager)
    private var clawProvider: ClawProvider = ClawProvider(secureStorageManager)
    // Semaphore to enforce maximum concurrent queries (Rate-limit and QoS protection)
    private val concurrencyLimiter = Semaphore(3)

    private val _providerConfigs = MutableStateFlow<List<ApiProviderConfig>>(getDefaultConfigs())
    val providerConfigs: StateFlow<List<ApiProviderConfig>> = _providerConfigs.asStateFlow()

    init {
        secureStorageManager?.let { loadStoredConfigs(it) }
    }

    fun attachSecureStorage(storage: SecureStorageManager) {
        this.secureStorageManager = storage
        cryptoApisProvider = CryptoApisProvider(storage)
        numVerifyProvider = NumVerifyProvider(storage)
        breadcrumbsProvider = BreadcrumbsProvider(storage)
        clawProvider = ClawProvider(storage)
        loadStoredConfigs(storage)
    }

    private fun loadStoredConfigs(storage: SecureStorageManager) {
        val updated = _providerConfigs.value.map { cfg ->
            val primaryKey = storage.getApiKeyPrimary(cfg.id)
            val secondaryKey = storage.getApiKeySecondary(cfg.id)
            val enabled = storage.isProviderEnabled(cfg.id, cfg.isEnabled)

            val status = if (primaryKey.isNotBlank() || secondaryKey.isNotBlank() || cfg.isFree) {
                ProviderStatus.HEALTHY
            } else {
                ProviderStatus.NOT_CONFIGURED
            }

            cfg.copy(
                apiKeyPrimary = primaryKey,
                apiKeySecondary = secondaryKey,
                isEnabled = enabled,
                status = status
            )
        }
        _providerConfigs.value = updated

        // Instantiating providers with loaded keys
        updated.forEach { cfg ->
            when (cfg.id) {
                "etherscan_eth" -> {
                    etherscanProvider = EthereumEtherscanProvider(
                        apiKeyPrimary = cfg.apiKeyPrimary,
                        apiKeySecondary = cfg.apiKeySecondary
                    )
                }
                "trongrid_tron" -> {
                    tronGridProvider = TronGridProvider(
                        apiKeyPrimary = cfg.apiKeyPrimary,
                        apiKeySecondary = cfg.apiKeySecondary
                    )
                }
                "bscscan_bnb" -> {
                    bscScanProvider = BscScanProvider(
                        apiKeyPrimary = cfg.apiKeyPrimary,
                        apiKeySecondary = cfg.apiKeySecondary
                    )
                }
            }
        }
    }

    fun getProvider(network: BlockchainNetwork): BlockchainProvider {
        return when (network) {
            BlockchainNetwork.BITCOIN -> mempoolProvider
            BlockchainNetwork.ETHEREUM, BlockchainNetwork.POLYGON -> etherscanProvider
            BlockchainNetwork.TRON -> tronGridProvider
            BlockchainNetwork.BNB_CHAIN -> bscScanProvider
            BlockchainNetwork.TETHER_USDT -> etherscanProvider
            else -> mempoolProvider
        }
    }

    suspend fun fetchAddressOverviewWithFallback(
        network: BlockchainNetwork,
        address: String
    ): Result<AddressOverviewDto> {
        concurrencyLimiter.acquire()
        return try {
            when (network) {
                BlockchainNetwork.BITCOIN -> {
                    // Try Primary Mempool.space first with retry
                    val primaryResult = executeWithRetry { mempoolProvider.fetchAddressOverview(address) }
                    if (primaryResult.isSuccess) {
                        primaryResult
                    } else {
                        // Fallback to Blockchain.info
                        executeWithRetry { blockchainInfoProvider.fetchAddressOverview(address) }
                    }
                }
                BlockchainNetwork.TRON -> {
                    executeWithRetry { tronGridProvider.fetchAddressOverview(address) }
                }
                BlockchainNetwork.BNB_CHAIN -> {
                    executeWithRetry { bscScanProvider.fetchAddressOverview(address) }
                }
                BlockchainNetwork.ETHEREUM, BlockchainNetwork.POLYGON, BlockchainNetwork.TETHER_USDT -> {
                    executeWithRetry { etherscanProvider.fetchAddressOverview(address) }
                }
                else -> {
                    executeWithRetry { mempoolProvider.fetchAddressOverview(address) }
                }
            }
        } finally {
            concurrencyLimiter.release()
        }
    }

    suspend fun fetchTransactionsWithFallback(
        network: BlockchainNetwork,
        address: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<ForensicTransaction>> {
        concurrencyLimiter.acquire()
        return try {
            when (network) {
                BlockchainNetwork.BITCOIN -> {
                    val primaryResult = executeWithRetry { mempoolProvider.fetchTransactions(address, limit, offset) }
                    if (primaryResult.isSuccess && primaryResult.getOrNull()?.isNotEmpty() == true) {
                        primaryResult
                    } else {
                        executeWithRetry { blockchainInfoProvider.fetchTransactions(address, limit, offset) }
                    }
                }
                BlockchainNetwork.TRON -> {
                    executeWithRetry { tronGridProvider.fetchTransactions(address, limit, offset) }
                }
                BlockchainNetwork.BNB_CHAIN -> {
                    executeWithRetry { bscScanProvider.fetchTransactions(address, limit, offset) }
                }
                BlockchainNetwork.ETHEREUM, BlockchainNetwork.POLYGON, BlockchainNetwork.TETHER_USDT -> {
                    executeWithRetry { etherscanProvider.fetchTransactions(address, limit, offset) }
                }
                else -> {
                    executeWithRetry { mempoolProvider.fetchTransactions(address, limit, offset) }
                }
            }
        } finally {
            concurrencyLimiter.release()
        }
    }

    private suspend fun <T> executeWithRetry(
        maxAttempts: Int = 2,
        initialDelayMs: Long = 400,
        block: suspend () -> Result<T>
    ): Result<T> {
        var currentDelay = initialDelayMs
        var lastResult: Result<T>? = null
        for (attempt in 1..maxAttempts) {
            val result = block()
            if (result.isSuccess) return result
            lastResult = result
            if (attempt < maxAttempts) {
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        return lastResult ?: Result.failure(Exception("Execution failed after $maxAttempts attempts"))
    }

    fun toggleProvider(providerId: String, isEnabled: Boolean) {
        secureStorageManager?.setProviderEnabled(providerId, isEnabled)
        _providerConfigs.value = _providerConfigs.value.map {
            if (it.id == providerId) it.copy(isEnabled = isEnabled) else it
        }
    }

    suspend fun testProvider(providerId: String): Result<Boolean> {
        return when (providerId) {
            "mempool_space_btc" -> mempoolProvider.testConnection()
            "blockchain_info_btc" -> blockchainInfoProvider.testConnection()
            "etherscan_eth" -> etherscanProvider.testConnection()
            "trongrid_tron" -> tronGridProvider.testConnection()
            "bscscan_bnb" -> bscScanProvider.testConnection()
            "cryptoapis_multi" -> cryptoApisProvider.testConnection()
            "numverify_phone" -> numVerifyProvider.testConnection()
            "breadcrumbs_analytics" -> breadcrumbsProvider.testConnection()
            "api_claw_provider" -> clawProvider.testConnection()
            "abuseipdb_threat" -> {
                val key = secureStorageManager?.getApiKeyPrimary("abuseipdb_threat") ?: ""
                if (key.isNotBlank()) Result.success(true) else Result.failure(Exception("API Key not found / کلید پیکربندی نشده است"))
            }
            "hibp_identity" -> {
                val key = secureStorageManager?.getApiKeyPrimary("hibp_identity") ?: ""
                if (key.isNotBlank()) Result.success(true) else Result.failure(Exception("API Key not found / کلید پیکربندی نشده است"))
            }
            else -> Result.failure(Exception("Unknown provider ID: $providerId"))
        }
    }

    fun updateApiKey(providerId: String, primaryKey: String, secondaryKey: String) {
        val trimmedPrimary = primaryKey.trim()
        val trimmedSecondary = secondaryKey.trim()
        secureStorageManager?.saveApiKey(providerId, trimmedPrimary, trimmedSecondary)

        val updated = _providerConfigs.value.map {
            if (it.id == providerId) {
                it.copy(
                    apiKeyPrimary = trimmedPrimary,
                    apiKeySecondary = trimmedSecondary,
                    status = if (trimmedPrimary.isNotBlank() || trimmedSecondary.isNotBlank() || it.isFree) ProviderStatus.HEALTHY else ProviderStatus.NOT_CONFIGURED
                )
            } else it
        }
        _providerConfigs.value = updated

        // Re-instantiate provider with updated keys
        when (providerId) {
            "etherscan_eth" -> {
                etherscanProvider = EthereumEtherscanProvider(
                    apiKeyPrimary = trimmedPrimary,
                    apiKeySecondary = trimmedSecondary
                )
            }
            "trongrid_tron" -> {
                tronGridProvider = TronGridProvider(
                    apiKeyPrimary = trimmedPrimary,
                    apiKeySecondary = trimmedSecondary
                )
            }
            "bscscan_bnb" -> {
                bscScanProvider = BscScanProvider(
                    apiKeyPrimary = trimmedPrimary,
                    apiKeySecondary = trimmedSecondary
                )
            }
        }
    }

    private fun getDefaultConfigs(): List<ApiProviderConfig> {
        return listOf(
            ApiProviderConfig(
                id = "mempool_space_btc",
                name = "Mempool.space (Bitcoin Mainnet)",
                network = BlockchainNetwork.BITCOIN,
                baseUrl = "https://mempool.space/api",
                isFree = true,
                requiresKey = false,
                officialUrl = "https://mempool.space/docs/api/rest",
                helpSummaryFa = "اکسپلورر متن‌باز و عمومی شبکه بیت‌کوین بدون نیاز به کلید API. پشتیبانی از تمامی فرمت‌های Legacy, P2SH, SegWit و Taproot.",
                helpSummaryEn = "Free and open-source Bitcoin explorer API. No API key required for standard forensic lookups. Rate limit ~30 req/min.",
                rateLimitPerMin = 30,
                status = ProviderStatus.HEALTHY
            ),
            ApiProviderConfig(
                id = "blockchain_info_btc",
                name = "Blockchain.info (Bitcoin Fallback)",
                network = BlockchainNetwork.BITCOIN,
                baseUrl = "https://blockchain.info",
                isFree = true,
                requiresKey = false,
                officialUrl = "https://www.blockchain.com/explorer/api",
                helpSummaryFa = "سرویس پشتیبان عمومی بیت‌کوین جهت تجمیع گراف ورودی/خروجی و اعتبارسنجی مستقل دفترکل.",
                helpSummaryEn = "Secondary fallback provider for UTXO graph discovery and historical confirmation verification on Bitcoin.",
                rateLimitPerMin = 20,
                status = ProviderStatus.HEALTHY
            ),
            ApiProviderConfig(
                id = "etherscan_eth",
                name = "Etherscan & Blockscout (Ethereum & USDT ERC-20)",
                network = BlockchainNetwork.ETHEREUM,
                baseUrl = "https://api.etherscan.io/api",
                isFree = true,
                requiresKey = false,
                officialUrl = "https://etherscan.io/apis",
                helpSummaryFa = "اکسپلورر اتریوم و توکن‌های استاندارد ERC-20 به ویژه USDT. در صورت عدم ثبت کلید، از نسخه رایگان Blockscout استفاده می‌شود.",
                helpSummaryEn = "Ethereum & ERC-20 token explorer API. Supports official Etherscan keys and automatic fallback to Blockscout open API.",
                rateLimitPerMin = 60,
                status = ProviderStatus.HEALTHY
            ),
            ApiProviderConfig(
                id = "trongrid_tron",
                name = "TronGrid (TRON & USDT TRC-20)",
                network = BlockchainNetwork.TRON,
                baseUrl = "https://api.trongrid.io",
                isFree = true,
                requiresKey = false,
                officialUrl = "https://www.trongrid.io",
                helpSummaryFa = "سرویس واکشی تراکنش‌های ترون و انتقال توکن‌های USDT استاندارد TRC-20 با پشتیبانی از حساب عمومی رایگان.",
                helpSummaryEn = "TronGrid official API for TRON blockchain and USDT TRC-20 transfers. Public free tier supported with optional API key.",
                rateLimitPerMin = 30,
                status = ProviderStatus.HEALTHY
            ),
            ApiProviderConfig(
                id = "bscscan_bnb",
                name = "BscScan (BNB Chain & USDT BEP-20)",
                network = BlockchainNetwork.BNB_CHAIN,
                baseUrl = "https://api.bscscan.com/api",
                isFree = true,
                requiresKey = true,
                officialUrl = "https://bscscan.com/apis",
                helpSummaryFa = "اکسپلورر رسمی BNB Smart Chain و تراکنش‌های توکن USDT استاندارد BEP-20. دریافت کلید با ثبت‌نام رایگان در bscscan.com.",
                helpSummaryEn = "Official BNB Smart Chain explorer API for BNB native and BEP-20 token tracking. Free API key available at bscscan.com.",
                rateLimitPerMin = 60,
                status = ProviderStatus.NOT_CONFIGURED
            ),
            ApiProviderConfig(
                id = "abuseipdb_threat",
                name = "AbuseIPDB (IP Threat Intelligence)",
                network = BlockchainNetwork.BITCOIN,
                baseUrl = "https://api.abuseipdb.com/api/v2",
                isFree = true,
                requiresKey = true,
                officialUrl = "https://www.abuseipdb.com/",
                helpSummaryFa = "پایگاه داده جامع گزارش‌های سواستفاده و تهدیدات آدرس‌های آی‌پی. امتیاز ریسک و فعالیت‌های مخرب (مانند حملات هک، اسپم، بات‌نت و دی‌داس) را در لحظه استعلام می‌کند. ثبت‌نام رایگان و دریافت کلید API در سایت رسمی.",
                helpSummaryEn = "Comprehensive database of IP abuse reports. Queries real-time risk scores and malicious traffic associations (hacking, spam, DDoS). Get a free API key at abuseipdb.com (1,000 free queries/day).",
                rateLimitPerMin = 60,
                status = ProviderStatus.NOT_CONFIGURED
            ),
            ApiProviderConfig(
                id = "hibp_identity",
                name = "HaveIBeenPwned (Identity Leak Database)",
                network = BlockchainNetwork.BITCOIN,
                baseUrl = "https://haveibeenpwned.com/api/v3",
                isFree = false,
                requiresKey = true,
                officialUrl = "https://haveibeenpwned.com/API/Key",
                helpSummaryFa = "پایگاه داده رسمی نشت اطلاعات کاربری (HaveIBeenPwned) جهت انطباق ایمیل‌ها و کدهای کاربری فاش شده در هک‌های تاریخی بزرگ. در صورت عدم پیکربندی کلید، سیستم به صورت هوشمند از تحلیل رایگان عمومی Gravatar برای راستی‌آزمایی استفاده می‌کند.",
                helpSummaryEn = "Official credential breach database (HaveIBeenPwned) to correlate leaked emails and aliases in major historical data dumps. If no key is entered, a smart public Gravatar lookup is executed as a free fallback.",
                rateLimitPerMin = 10,
                status = ProviderStatus.NOT_CONFIGURED
            ),
            ApiProviderConfig(
                id = "blockcypher_multi",
                name = "BlockCypher (Multi-chain API)",
                network = BlockchainNetwork.BITCOIN,
                baseUrl = "https://api.blockcypher.com/v1",
                isFree = true,
                requiresKey = true,
                officialUrl = "https://accounts.blockcypher.com/",
                helpSummaryFa = "وب‌سرویس مولتی‌چین BlockCypher جهت استخراج سریع مانده حساب، تراکنش‌ها و همبستگی آدرس‌ها در شبکه‌های بیت‌کوین و اتریوم. دریافت توکن رایگان پس از ثبت‌نام.",
                helpSummaryEn = "Multi-chain explorer API providing fast address queries, unspent outputs, and transaction histories for Bitcoin & Ethereum.",
                rateLimitPerMin = 100,
                status = ProviderStatus.NOT_CONFIGURED
            ),
            ApiProviderConfig(
                id = "ipstack_geo",
                name = "IPStack (Premium Geolocation API)",
                network = BlockchainNetwork.BITCOIN,
                baseUrl = "http://api.ipstack.com",
                isFree = true,
                requiresKey = true,
                officialUrl = "https://ipstack.com/",
                helpSummaryFa = "سرویس موقعیت‌یابی دقیق جغرافیایی آدرس‌های آی‌پی هدف. استخراج شهر، کشور، قاره، کد پستی و مختصات نقشه به صورت زنده با ورود کلید اختصاصی.",
                helpSummaryEn = "Premium IP geolocation lookup engine. Fetches highly accurate city, country, zip, and map coordinates for operator trace.",
                rateLimitPerMin = 120,
                status = ProviderStatus.NOT_CONFIGURED
            )
        )
    }
}
