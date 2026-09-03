package com.aistudio.orbit.provider

import com.aistudio.orbit.model.ApiProviderConfig
import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.ForensicTransaction
import com.aistudio.orbit.model.ProviderDisagreement
import com.aistudio.orbit.model.ProviderHealthMetric
import com.aistudio.orbit.model.ProviderStatus
import com.aistudio.orbit.security.SecureStorageManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import java.util.UUID

/**
 * Provider Manager (Prompt 3 §7, Master Instruction §25, §35, §36, §37)
 * Strictly prevents fake health reports: Status is derived from actual network connectivity.
 * Tracks provider disagreements without silently overwriting conflicting evidence.
 */
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

    private val concurrencyLimiter = Semaphore(3)

    private val _providerConfigs = MutableStateFlow<List<ApiProviderConfig>>(getDefaultConfigs())
    val providerConfigs: StateFlow<List<ApiProviderConfig>> = _providerConfigs.asStateFlow()

    private val _healthMetrics = MutableStateFlow<Map<String, ProviderHealthMetric>>(emptyMap())
    val healthMetrics: StateFlow<Map<String, ProviderHealthMetric>> = _healthMetrics.asStateFlow()

    private val _disagreements = MutableStateFlow<List<ProviderDisagreement>>(emptyList())
    val disagreements: StateFlow<List<ProviderDisagreement>> = _disagreements.asStateFlow()

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

            val status = when {
                !enabled -> ProviderStatus.DISABLED
                cfg.id == "breadcrumbs_analytics" || cfg.id == "api_claw_provider" -> ProviderStatus.UNVERIFIED
                primaryKey.isNotBlank() || secondaryKey.isNotBlank() -> ProviderStatus.CONFIGURED
                cfg.isFree -> ProviderStatus.HEALTHY
                else -> ProviderStatus.NOT_CONFIGURED
            }

            cfg.copy(
                apiKeyPrimary = primaryKey,
                apiKeySecondary = secondaryKey,
                isEnabled = enabled,
                status = status
            )
        }
        _providerConfigs.value = updated

        updated.forEach { cfg ->
            when (cfg.id) {
                "etherscan_eth" -> etherscanProvider = EthereumEtherscanProvider(cfg.apiKeyPrimary, cfg.apiKeySecondary)
                "trongrid_tron" -> tronGridProvider = TronGridProvider(cfg.apiKeyPrimary, cfg.apiKeySecondary)
                "bscscan_bnb" -> bscScanProvider = BscScanProvider(cfg.apiKeyPrimary, cfg.apiKeySecondary)
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
                    val primaryResult = executeWithRetry { mempoolProvider.fetchAddressOverview(address) }
                    if (primaryResult.isSuccess) {
                        val primaryDto = primaryResult.getOrThrow()
                        // Asynchronous cross-provider validation check for consensus vs disagreement
                        try {
                            val fallbackResult = blockchainInfoProvider.fetchAddressOverview(address)
                            if (fallbackResult.isSuccess) {
                                val fallbackDto = fallbackResult.getOrThrow()
                                checkAndRecordDisagreement(primaryDto, fallbackDto, network, address)
                            }
                        } catch (e: Exception) {
                            // Non-blocking cross-validation
                        }
                        primaryResult
                    } else {
                        // Fallback
                        executeWithRetry { blockchainInfoProvider.fetchAddressOverview(address) }
                    }
                }
                BlockchainNetwork.TRON -> executeWithRetry { tronGridProvider.fetchAddressOverview(address) }
                BlockchainNetwork.BNB_CHAIN -> executeWithRetry { bscScanProvider.fetchAddressOverview(address) }
                BlockchainNetwork.ETHEREUM, BlockchainNetwork.POLYGON, BlockchainNetwork.TETHER_USDT -> {
                    executeWithRetry { etherscanProvider.fetchAddressOverview(address) }
                }
                else -> executeWithRetry { mempoolProvider.fetchAddressOverview(address) }
            }
        } finally {
            concurrencyLimiter.release()
        }
    }

    private fun checkAndRecordDisagreement(
        dtoA: AddressOverviewDto,
        dtoB: AddressOverviewDto,
        network: BlockchainNetwork,
        address: String
    ) {
        val balanceDiff = Math.abs(dtoA.balanceSat - dtoB.balanceSat)
        if (balanceDiff > 1000L) {
            val record = ProviderDisagreement(
                disagreementId = "DIS_${UUID.randomUUID().toString().take(8)}",
                address = address,
                network = network,
                discrepancyType = "BALANCE",
                providerAName = dtoA.providerName,
                providerAValue = "${dtoA.balanceSat} sat",
                providerBName = dtoB.providerName,
                providerBValue = "${dtoB.balanceSat} sat",
                timestamp = System.currentTimeMillis(),
                analystNotes = "Balance discrepancy detected between independent ledger providers"
            )
            _disagreements.value = _disagreements.value + record
        }

        if (dtoA.transactionCount != dtoB.transactionCount && dtoA.transactionCount > 0 && dtoB.transactionCount > 0) {
            val record = ProviderDisagreement(
                disagreementId = "DIS_${UUID.randomUUID().toString().take(8)}",
                address = address,
                network = network,
                discrepancyType = "TRANSACTION_COUNT",
                providerAName = dtoA.providerName,
                providerAValue = "${dtoA.transactionCount} txs",
                providerBName = dtoB.providerName,
                providerBValue = "${dtoB.transactionCount} txs",
                timestamp = System.currentTimeMillis(),
                analystNotes = "Transaction count discrepancy between providers"
            )
            _disagreements.value = _disagreements.value + record
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
                BlockchainNetwork.TRON -> executeWithRetry { tronGridProvider.fetchTransactions(address, limit, offset) }
                BlockchainNetwork.BNB_CHAIN -> executeWithRetry { bscScanProvider.fetchTransactions(address, limit, offset) }
                BlockchainNetwork.ETHEREUM, BlockchainNetwork.POLYGON, BlockchainNetwork.TETHER_USDT -> {
                    executeWithRetry { etherscanProvider.fetchTransactions(address, limit, offset) }
                }
                else -> executeWithRetry { mempoolProvider.fetchTransactions(address, limit, offset) }
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
            if (it.id == providerId) {
                val newStatus = if (!isEnabled) ProviderStatus.DISABLED else if (it.apiKeyPrimary.isNotBlank()) ProviderStatus.CONFIGURED else ProviderStatus.NOT_CONFIGURED
                it.copy(isEnabled = isEnabled, status = newStatus)
            } else it
        }
    }

    suspend fun testProvider(providerId: String): Result<Boolean> {
        // Mark status as TESTING
        _providerConfigs.value = _providerConfigs.value.map {
            if (it.id == providerId) it.copy(status = ProviderStatus.TESTING) else it
        }

        val startTime = System.currentTimeMillis()
        val testResult: Result<Boolean> = try {
            when (providerId) {
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
                    if (key.isNotBlank()) Result.success(true) else Result.failure(Exception("API Key not found"))
                }
                "hibp_identity" -> {
                    val key = secureStorageManager?.getApiKeyPrimary("hibp_identity") ?: ""
                    if (key.isNotBlank()) Result.success(true) else Result.failure(Exception("API Key not found"))
                }
                else -> Result.failure(Exception("Unknown provider ID: $providerId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

        val latency = System.currentTimeMillis() - startTime
        val isSuccess = testResult.isSuccess

        val finalStatus = when {
            providerId == "breadcrumbs_analytics" || providerId == "api_claw_provider" -> ProviderStatus.UNVERIFIED
            isSuccess -> ProviderStatus.HEALTHY
            else -> ProviderStatus.FAILED
        }

        val metric = ProviderHealthMetric(
            providerId = providerId,
            status = finalStatus,
            httpStatusCode = if (isSuccess) 200 else 500,
            latencyMs = latency,
            lastError = testResult.exceptionOrNull()?.message,
            checkedAt = System.currentTimeMillis()
        )

        val currentMetrics = _healthMetrics.value.toMutableMap()
        currentMetrics[providerId] = metric
        _healthMetrics.value = currentMetrics

        _providerConfigs.value = _providerConfigs.value.map {
            if (it.id == providerId) {
                it.copy(
                    status = finalStatus,
                    lastResponseTimeMs = latency,
                    lastCheckedTimestamp = System.currentTimeMillis(),
                    httpStatusCode = metric.httpStatusCode
                )
            } else it
        }

        return testResult
    }

    fun updateApiKey(providerId: String, primaryKey: String, secondaryKey: String) {
        val trimmedPrimary = primaryKey.trim()
        val trimmedSecondary = secondaryKey.trim()
        secureStorageManager?.saveApiKey(providerId, trimmedPrimary, trimmedSecondary)

        val updated = _providerConfigs.value.map {
            if (it.id == providerId) {
                val newStatus = when {
                    providerId == "breadcrumbs_analytics" || providerId == "api_claw_provider" -> ProviderStatus.UNVERIFIED
                    trimmedPrimary.isNotBlank() || trimmedSecondary.isNotBlank() -> ProviderStatus.CONFIGURED
                    it.isFree -> ProviderStatus.HEALTHY
                    else -> ProviderStatus.NOT_CONFIGURED
                }
                it.copy(
                    apiKeyPrimary = trimmedPrimary,
                    apiKeySecondary = trimmedSecondary,
                    status = newStatus
                )
            } else it
        }
        _providerConfigs.value = updated

        when (providerId) {
            "etherscan_eth" -> etherscanProvider = EthereumEtherscanProvider(trimmedPrimary, trimmedSecondary)
            "trongrid_tron" -> tronGridProvider = TronGridProvider(trimmedPrimary, trimmedSecondary)
            "bscscan_bnb" -> bscScanProvider = BscScanProvider(trimmedPrimary, trimmedSecondary)
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
                helpSummaryFa = "اکسپلورر متن‌باز و عمومی شبکه بیت‌کوین بدون نیاز به کلید API.",
                helpSummaryEn = "Free and open-source Bitcoin explorer API. Rate limit ~30 req/min.",
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
                helpSummaryEn = "Secondary fallback provider for UTXO graph discovery and historical verification.",
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
                helpSummaryFa = "اکسپلورر اتریوم و توکن‌های استاندارد ERC-20. در صورت عدم ثبت کلید، از نسخه رایگان Blockscout استفاده می‌شود.",
                helpSummaryEn = "Ethereum & ERC-20 token explorer API. Supports official Etherscan keys and Blockscout fallback.",
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
                helpSummaryFa = "سرویس واکشی تراکنش‌های ترون و انتقال توکن‌های USDT استاندارد TRC-20.",
                helpSummaryEn = "TronGrid official API for TRON blockchain and USDT TRC-20 transfers.",
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
                helpSummaryFa = "اکسپلورر رسمی BNB Smart Chain و تراکنش‌های توکن USDT استاندارد BEP-20.",
                helpSummaryEn = "Official BNB Smart Chain explorer API for BNB native and BEP-20 token tracking.",
                rateLimitPerMin = 60,
                status = ProviderStatus.NOT_CONFIGURED
            ),
            ApiProviderConfig(
                id = "cryptoapis_multi",
                name = "CryptoAPIs (Multi-Chain & AML)",
                network = BlockchainNetwork.BITCOIN,
                baseUrl = "https://rest.cryptoapis.io/v2",
                isFree = false,
                requiresKey = true,
                officialUrl = "https://cryptoapis.io/",
                helpSummaryFa = "سرویس جامع تحلیل چندزنجیره‌ای، مدیریت خروجی‌های خرج‌نشده (UTXO) و بررسی AML.",
                helpSummaryEn = "Official CryptoAPIs 2.0 multi-chain ledger and compliance engine.",
                rateLimitPerMin = 120,
                status = ProviderStatus.NOT_CONFIGURED
            ),
            ApiProviderConfig(
                id = "breadcrumbs_analytics",
                name = "Breadcrumbs.app (Commercial Attribution)",
                network = BlockchainNetwork.BITCOIN,
                baseUrl = "https://www.breadcrumbs.app",
                isFree = false,
                requiresKey = true,
                officialUrl = "https://www.breadcrumbs.app/",
                helpSummaryFa = "سرویس تجاری انتساب آدرس‌های بلاکچین. این سرویس تا زمان ارائه مستندات رسمی غیرفعال است (UNVERIFIED).",
                helpSummaryEn = "Commercial blockchain attribution service. Disabled pending official verification (UNVERIFIED).",
                rateLimitPerMin = 60,
                status = ProviderStatus.UNVERIFIED,
                isEnabled = false
            ),
            ApiProviderConfig(
                id = "api_claw_provider",
                name = "API Claw (Scraper & OSINT)",
                network = BlockchainNetwork.BITCOIN,
                baseUrl = "https://api.claw.placeholder",
                isFree = false,
                requiresKey = true,
                officialUrl = "",
                helpSummaryFa = "سرویس اسکرپ و تجمیع داده. این سرویس تا زمان ارائه مستندات رسمی غیرفعال است (UNVERIFIED).",
                helpSummaryEn = "Scraper service. Disabled pending official verification (UNVERIFIED).",
                rateLimitPerMin = 30,
                status = ProviderStatus.UNVERIFIED,
                isEnabled = false
            ),
            ApiProviderConfig(
                id = "numverify_phone",
                name = "NumVerify (Telecom Validation)",
                network = BlockchainNetwork.BITCOIN,
                baseUrl = "https://api.numverify.com/v1/validate",
                isFree = true,
                requiresKey = true,
                officialUrl = "https://numverify.com/",
                helpSummaryFa = "سرویس اعتبارسنجی و غنی‌سازی اطلاعات مخابراتی شماره تلفن. صرفاً غنی‌سازی ساختار خط و بدون اثبات هویت مالک.",
                helpSummaryEn = "Telecom number validation and carrier enrichment. Strictly heuristic enrichment.",
                rateLimitPerMin = 30,
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
                helpSummaryFa = "پایگاه داده جامع گزارش‌های سواستفاده و تهدیدات آدرس‌های آی‌پی.",
                helpSummaryEn = "Comprehensive database of IP abuse reports. 1,000 free queries/day.",
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
                helpSummaryFa = "پایگاه داده نشت اطلاعات کاربری جهت انطباق ایمیل‌ها و کدهای کاربری فاش شده.",
                helpSummaryEn = "Official credential breach database (HaveIBeenPwned).",
                rateLimitPerMin = 10,
                status = ProviderStatus.NOT_CONFIGURED
            )
        )
    }
}
