package com.aistudio.orbit.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole(val displayNameEn: String, val displayNameFa: String) {
    ADMINISTRATOR("Administrator (Full Access)", "مدیر ارشد سامانه (دسترسی کامل)"),
    LEAD_INVESTIGATOR("Lead Forensic Investigator", "سرپرست جرم‌یابی و تحلیل ادله"),
    ANALYST("Financial Crime Analyst", "تحلیل‌گر جرائم مالی و تراکنش‌ها"),
    AUDITOR("Compliance & Audit Officer", "ناظر انطباق و ممیزی ادله")
}

@Serializable
enum class ForensicPermission(val code: String, val titleEn: String, val titleFa: String) {
    BITCOIN_ANALYSIS("perm_btc_analysis", "Bitcoin Blockchain Analysis", "تحلیل و ردیابی شبکه بیت‌کوین"),
    ETHEREUM_ANALYSIS("perm_eth_analysis", "Ethereum & EVM Analysis", "تحلیل و ردیابی شبکه اتریوم و EVM"),
    TOKEN_USDT_ANALYSIS("perm_token_analysis", "Tether (USDT) & Token Analysis", "تحلیل توکن‌های تتر (USDT) و استیبل‌کوین‌ها"),
    TRANSACTION_FILTERING("perm_tx_filter", "Deep Flow & Transaction Filtering", "فیلترگذاری پیشرفته تراکنش‌ها و جریان‌ها"),
    COUNTERPARTY_ANALYSIS("perm_counterparty", "Counterparty & Matrix Analysis", "تحلیل عمیق ماتریس طرف‌های مقابل"),
    GRAPH_VISUALIZATION("perm_graph", "Interactive Network Graph", "کاوش و ترسیم گراف تعاملی ارتباطات"),
    BEHAVIORAL_ANALYSIS("perm_behavior", "Behavioral & Velocity Analysis", "تحلیل الگوهای رفتاری و سرعت انتقال"),
    CRIME_PATTERN_MATCHING("perm_crime_pattern", "Crime-Pattern Library & Matching", "تطبیق با کتابخانه الگوهای جرائم مالی"),
    GEOGRAPHIC_TIME_INFERENCE("perm_geo_time", "Temporal & Geographic-Time Inference", "تحلیل شبانه‌روزی و استنباط زمانی-جغرافیایی"),
    FORENSIC_REPORTS_EXPORT("perm_export", "Forensic Reports & Dossier Export", "صدور گزارشات رسمی و مستندات پرونده"),
    API_PROVIDER_SETTINGS("perm_api_settings", "API Provider & Key Management", "مدیریت ارائه‌دهندگان و کلیدهای API"),
    CASE_ARCHIVE_MANAGEMENT("perm_case_archives", "Case History & Archive Access", "دسترسی به بایگانی پرونده‌ها و تاریخچه"),
    PATTERN_DATABASE_MANAGE("perm_pattern_db", "Pattern Library Management", "مدیریت کتابخانه الگوهای جرم"),
    USER_ACCESS_MANAGEMENT("perm_user_mgmt", "User Management & Access Control", "مدیریت کاربران و سطوح دسترسی")
}

@Serializable
data class UserAccount(
    val userId: String,
    val username: String,
    val displayName: String,
    val role: UserRole,
    val isEnabled: Boolean = true,
    val requiresPasswordChange: Boolean = false,
    val grantedPermissions: Set<ForensicPermission>,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val lastLoginTimestamp: Long? = null
)

@Serializable
data class AuditLogEntry(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String,
    val username: String,
    val operation: String,
    val target: String,
    val details: String,
    val ipOrContext: String = "Local Android Client"
)
