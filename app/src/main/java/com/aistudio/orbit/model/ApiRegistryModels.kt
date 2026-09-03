package com.aistudio.orbit.model

import kotlinx.serialization.Serializable

/**
 * Category of external API or Service in the Bayyinah Platform.
 */
@Serializable
enum class ApiCategory(
    val titleFa: String,
    val titleEn: String,
    val descriptionFa: String,
    val descriptionEn: String
) {
    AI("هوش مصنوعی و تحلیل زبانی", "Artificial Intelligence", "سرویس‌های LLM جهت تحلیل، سناریوسازی و خلاصه‌سازی پرونده", "Large Language Models for automated synthesis and forensic copilot"),
    OSINT("هوشمندی منابع آشکار (OSINT)", "Open Source Intelligence", "استعلام هویت، ایمیل، تلفن، نام‌کاربری و پایگاه‌های نشت اطلاعات", "Reconnaissance for identity, leaked credentials, domains, and phone traces"),
    BLOCKCHAIN("داده‌های بلاکچین و دفترکل", "Blockchain & Ledgers", "کاوشگرهای تراکنش، مانده‌حساب، UTXO و اکسپلوررهای چندزنجیره‌ای", "On-chain explorers, multi-chain indexers, and balance decoders"),
    THREAT_INTEL("هوشمندی تهدیدات و امنیت", "Threat Intelligence", "پایگاه‌های نشانه‌های آلودگی (IOC)، بدافزار، فیشینگ و سرورهای MISP", "IOC feeds, phishing repositories, malware databases, and MISP nodes"),
    MARKET_DATA("داده‌های بازار و صرافی‌ها", "Exchange & Market Data", "نرخ‌های لحظه‌ای و تاریخی رمزارزها جهت ارزش‌گذاری ریالی و دلاری", "Live and historical price feeds for asset valuation and exchange tracking"),
    GEOLOCATION("موقعیت‌یابی و تحلیل جغرافیایی", "Maps & Geolocation", "سرویس‌های تحلیل IP، ثبت ASN و تعیین محدوده جغرافیایی متهم", "IP geolocation, ASN mapping, and ISP infrastructure tracing"),
    OSINT_TOOLS("ماژول‌ها و ابزارهای کاوش (Sherlock)", "OSINT Tools & Modules", "تنظیمات اختصاصی ابزارهای کاوش هویت و سرویس‌های واسط", "Dedicated modules for username discovery and specialized recon tools")
}

/**
 * Connection and verification state of an API.
 */
@Serializable
enum class ApiConnectionState(
    val titleFa: String,
    val titleEn: String
) {
    CONNECTED("متصل و معتبر", "Connected"),
    INVALID_KEY("کلید نامعتبر است", "Invalid Key"),
    EXPIRED("منقضی شده", "Expired"),
    UNAUTHORIZED("دسترسی غیرمجاز", "Unauthorized"),
    RATE_LIMITED("محدودیت نرخ (Rate Limited)", "Rate Limited"),
    QUOTA_EXHAUSTED("سهمیه پایان یافته", "Quota Exhausted"),
    SERVICE_UNAVAILABLE("سرویس در دسترس نیست", "Service Unavailable"),
    NOT_CONFIGURED("پیکربندی نشده", "Not Configured"),
    TESTING("در حال آزمایش...", "Testing...")
}

/**
 * Quota and Rate Limit information for an API.
 */
@Serializable
data class ApiQuotaInfo(
    val remainingPercent: Int = 100,
    val remainingCount: Int? = null,
    val totalCount: Int? = null,
    val resetTimestamp: Long? = null,
    val rateLimitStr: String = "30 req/min"
)

/**
 * Comprehensive API Configuration specification.
 */
@Serializable
data class ComprehensiveApiConfig(
    val id: String,
    val name: String,
    val category: ApiCategory,
    val baseUrl: String,
    val apiKey: String = "",
    val secondaryKey: String = "",
    val isFree: Boolean = true,
    val requiresKey: Boolean = false,
    val officialUrl: String = "",
    val docUrl: String = "",
    val pricingUrl: String = "",
    val descriptionFa: String = "",
    val descriptionEn: String = "",
    val appUsageFa: String = "",
    val appUsageEn: String = "",
    val limitationsFa: String = "",
    val limitationsEn: String = "",
    val rateLimitPerMin: Int = 30,
    val quotaInfo: ApiQuotaInfo = ApiQuotaInfo(),
    val connectionState: ApiConnectionState = ApiConnectionState.NOT_CONFIGURED,
    val lastCheckedTimestamp: Long = 0,
    val isEnabled: Boolean = true
)

/**
 * Indexing status for Forensic Database.
 */
@Serializable
enum class DatabaseIndexStatus(val titleFa: String, val titleEn: String) {
    INDEXED("ایندکس شده و آماده", "Indexed & Ready"),
    NOT_INDEXED("ایندکس نشده", "Not Indexed"),
    REBUILDING("در حال بازسازی ایندکس...", "Rebuilding Index..."),
    CORRUPTED("شاخص خراب است", "Corrupted")
}

/**
 * Download and deployment status for offline database.
 */
@Serializable
enum class DatabaseDownloadStatus(val titleFa: String, val titleEn: String) {
    IDLE("آماده بارگیری", "Available for Download"),
    DOWNLOADING("در حال بارگیری...", "Downloading..."),
    VERIFYING("راستی‌آزمایی هش SHA-256...", "Verifying Integrity..."),
    EXTRACTING("استخراج و بارگذاری داده...", "Extracting Data..."),
    INDEXING("ایجاد شاخص و ایندکس...", "Building Fast Index..."),
    ENCRYPTING("رمزگذاری با کلید امن...", "Encrypting Storage..."),
    COMPLETED("نصب و فعال", "Installed & Active"),
    ERROR("خطا در بارگیری", "Download Error")
}

/**
 * Detailed specification for a forensic database or offline dataset.
 */
@Serializable
data class ForensicDatabaseInfo(
    val id: String,
    val name: String,
    val nameFa: String,
    val tier: String, // TIER 1, TIER 2, TIER 3, TIER 4
    val category: String, // SANCTIONS, TAGPACKS, VASP_REGISTRY, GEOIP, THREAT_INTEL, MIXER_HEURISTICS
    val recommendedSizeBytes: Long,
    val currentSizeBytes: Long,
    val recordCount: Long,
    val lastUpdated: String,
    val version: String,
    val isIndexed: Boolean = true,
    val indexStatus: DatabaseIndexStatus = DatabaseIndexStatus.INDEXED,
    val integritySha256: String,
    val storagePath: String = "/data/user/0/com.aistudio.orbit/databases/",
    val isEncrypted: Boolean = true,
    val dependencies: String = "Room SQLite + AES-256-GCM",
    val descriptionFa: String = "",
    val descriptionEn: String = "",
    val isInstalled: Boolean = false,
    val isEnabled: Boolean = true,
    val downloadUrl: String = "",
    val downloadProgress: Float = 0.0f,
    val downloadSpeedMbS: Float = 0.0f,
    val downloadedBytes: Long = 0,
    val downloadStatus: DatabaseDownloadStatus = DatabaseDownloadStatus.IDLE,
    val estimatedRemainingSeconds: Int = 0
)

/**
 * Result of API validation during Import.
 */
@Serializable
data class ValidatedApiKeyItem(
    val id: String,
    val serviceName: String,
    val category: ApiCategory,
    val maskedKey: String,
    val rawKey: String,
    val connectionState: ApiConnectionState,
    val quotaPercent: Int,
    val quotaText: String,
    val errorMessage: String? = null
)

/**
 * Conflict resolution policy for importing Cases and Forensic Datasets.
 */
enum class ImportConflictPolicy(val titleFa: String, val titleEn: String) {
    KEEP_EXISTING("حفظ اطلاعات موجود (رد داده جدید)", "Keep Existing"),
    REPLACE("جایگزینی کامل (بازنویسی)", "Replace (Overwrite)"),
    MERGE("ادغام هوشمند شواهد", "Merge Evidence"),
    SKIP("رد پرونده تکراری", "Skip")
}
