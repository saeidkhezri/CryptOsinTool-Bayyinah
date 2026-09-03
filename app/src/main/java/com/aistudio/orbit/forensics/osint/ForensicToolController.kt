package com.aistudio.orbit.forensics.osint

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable

/**
 * Execution status for live forensic OSINT reconnaissance modules.
 */
@Serializable
enum class ToolExecutionStatus(val displayNameEn: String, val displayNameFa: String) {
    QUEUED("Queued", "در صف اجرا"),
    RUNNING("Active Scanning...", "در حال پویش..."),
    SUCCESS("Indicators Discovered", "کشف موفق سرنخ"),
    VERIFIED("Verified Match", "تطابق قطعی تاییدشده"),
    RATE_LIMITED("Throttled / Rate Limited", "محدودیت نرخ (Rate-Limit)"),
    NO_DATA("No Matches Found", "بدون داده منطبق"),
    FAILED("Execution Error", "خطای ارتباطی ماژول")
}

/**
 * Configuration item for individual forensic tools.
 */
@Serializable
data class ForensicToolConfig(
    val toolId: String,
    val name: String,
    val category: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val isEnabled: Boolean = true,
    val isStealthMode: Boolean = true,
    val maxHopsOrThreads: Int = 3,
    val timeoutMs: Long = 12000L,
    val referenceRepoUrl: String
)

/**
 * Real-time execution log item emitted during active recon.
 */
@Serializable
data class ForensicExecutionLogItem(
    val id: String = "LOG_${System.currentTimeMillis()}_${(1000..9999).random()}",
    val timestamp: Long = System.currentTimeMillis(),
    val toolId: String,
    val toolName: String,
    val target: String,
    val status: ToolExecutionStatus,
    val messageEn: String,
    val messageFa: String,
    val discoveredIndicator: String? = null
)

/**
 * Central Controller for Modular Forensic Tools (Holehe, email2phonenumber, GHunt,
 * Ignorant, PhoneInfoga, Epieos, GraphSense, SpiderFoot) and live reconnaissance logging.
 */
object ForensicToolController {

    private val _tools = MutableStateFlow(defaultTools())
    val tools: StateFlow<List<ForensicToolConfig>> = _tools.asStateFlow()

    private val _executionLogs = MutableStateFlow<List<ForensicExecutionLogItem>>(emptyList())
    val executionLogs: StateFlow<List<ForensicExecutionLogItem>> = _executionLogs.asStateFlow()

    fun toggleTool(toolId: String, enabled: Boolean) {
        _tools.update { list ->
            list.map { if (it.toolId == toolId) it.copy(isEnabled = enabled) else it }
        }
    }

    fun toggleStealth(toolId: String, isStealth: Boolean) {
        _tools.update { list ->
            list.map { if (it.toolId == toolId) it.copy(isStealthMode = isStealth) else it }
        }
    }

    fun addLog(log: ForensicExecutionLogItem) {
        _executionLogs.update { current ->
            listOf(log) + current.take(99)
        }
    }

    fun clearLogs() {
        _executionLogs.value = emptyList()
    }

    private fun defaultTools(): List<ForensicToolConfig> = listOf(
        ForensicToolConfig(
            toolId = "HOLEHE",
            name = "Email Platform Footprinting (Holehe)",
            category = "Email & Platform OSINT",
            descriptionEn = "Password recovery and account enumeration across 120+ platforms (Adobe, Twitter, Telegram, Mail.ru).",
            descriptionFa = "پایش و استخراج حساب‌های متصل به ایمیل هدف در ۱۲۰ سکو و شبکه اجتماعی معتبر.",
            referenceRepoUrl = "https://github.com/megadose/holehe"
        ),
        ForensicToolConfig(
            toolId = "EMAIL2PHONE",
            name = "Phone Number Reconstruction Engine",
            category = "Phone Reconstruction",
            descriptionEn = "Scrapes masked digit hints and generates candidate phone numbers for carrier reverse-lookup.",
            descriptionFa = "بازسازی ارقام ماسک‌شده تلفن همراه و انطباق با پیش‌شماره‌های مخابراتی ایران و بین‌الملل.",
            referenceRepoUrl = "https://github.com/martinvigo/email2phonenumber"
        ),
        ForensicToolConfig(
            toolId = "GHUNT",
            name = "Google Account Reconnaissance (GHunt)",
            category = "Google Intelligence",
            descriptionEn = "Inspects Google Accounts, Gaia IDs, Maps reviews, calendar, and device footprint.",
            descriptionFa = "کاوش هوشمند حساب کاربری گوگل، استخراج شناسه گایا، نظرات نقشه و دستگاه‌های MFA متصل.",
            referenceRepoUrl = "https://github.com/mxrch/GHunt"
        ),
        ForensicToolConfig(
            toolId = "IGNORANT",
            name = "Social Registration Verifier (Ignorant)",
            category = "Social Phone OSINT",
            descriptionEn = "Silent Instagram and social phone number registration and partial identifier validator.",
            descriptionFa = "اعتبارسنجی ثبت‌نام شماره تلفن همراه در اینستاگرام و شبکه‌های اجتماعی بدون ارسال هشدار.",
            referenceRepoUrl = "https://github.com/megadose/ignorant"
        ),
        ForensicToolConfig(
            toolId = "PHONEINFOGA",
            name = "Telecom & HLR Analysis (PhoneInfoga)",
            category = "Telecom & HLR",
            descriptionEn = "Parses E.164 numbers, detects carrier (MCI, Irancell, Rightel), timezone and HLR routes.",
            descriptionFa = "تحلیل پیش‌شماره و اپراتورهای مخابراتی، مسیرهای HLR، نوع خط و اعتبارسنجی قالب E.164.",
            referenceRepoUrl = "https://github.com/sundowndev/phoneinfoga"
        ),
        ForensicToolConfig(
            toolId = "EPIEOS",
            name = "Web Breadcrumb Profiler (Epieos)",
            category = "Breadcrumb OSINT",
            descriptionEn = "Aggregates gravatar avatars, search breadcrumbs, and profile metadata without notifications.",
            descriptionFa = "ردیابی داده‌های هویتی وب، تصاویر آواتار و ردپاهای عمومی بدون فعال‌سازی آلارم امنیتی متهم.",
            referenceRepoUrl = "https://epieos.com"
        ),
        ForensicToolConfig(
            toolId = "GRAPHSENSE",
            name = "Wallet Clustering & TagPacks (GraphSense)",
            category = "On-Chain Clustering",
            descriptionEn = "Multi-input co-spend heuristics, exchange TagPacks, and peel-chain graph decomposition.",
            descriptionFa = "خوشه‌بندی کیف‌پول‌ها با هیوریستیک ورودی مشترک و انتساب برچسب صرافی‌های معتبر.",
            referenceRepoUrl = "https://github.com/graphsense/graphsense-dashboard"
        ),
        ForensicToolConfig(
            toolId = "SPIDERFOOT",
            name = "Threat & Darknet Correlation (SpiderFoot)",
            category = "Threat Correlation",
            descriptionEn = "Correlates Tor exit nodes, VPN subnets, darknet pastes, and OFAC sanctions list.",
            descriptionFa = "پایش همبستگی تهدیدات، گره‌های خروجی تور، ساب‌نت‌های وی‌پی‌ان و لیست‌های تحریم بین‌المللی.",
            referenceRepoUrl = "https://github.com/smicallef/spiderfoot"
        )
    )
}
