package com.aistudio.orbit.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage {
    PERSIAN,
    ENGLISH
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("orbit_settings_prefs", Context.MODE_PRIVATE)

    private val KEY_LANG = "app_language"
    private val KEY_USE_JALALI = "use_jalali_calendar"
    private val KEY_USE_TEHRAN_TZ = "use_tehran_tz"
    private val KEY_FORMAT_PERSIAN_NUMBERS = "format_persian_numbers"
    private val KEY_PRIVACY_MODE = "privacy_mode_enabled"
    private val KEY_THEME_MODE = "theme_mode"
    private val KEY_DYNAMIC_COLOR = "use_dynamic_color"
    private val KEY_USE_LUXURY_BACKGROUND = "use_luxury_background"

    // Network & Connectivity Toggles
    private val KEY_ONLINE_QUERIES = "online_queries_enabled"
    private val KEY_AUTO_FAILOVER = "auto_failover_enabled"
    private val KEY_RATE_LIMIT_PROTECTION = "rate_limit_protection_enabled"
    private val KEY_LEDGER_CACHE = "ledger_cache_enabled"
    private val KEY_STRICT_SSL = "strict_ssl_enabled"
    
    private val _useLuxuryBackground = MutableStateFlow(prefs.getBoolean(KEY_USE_LUXURY_BACKGROUND, true))
    val useLuxuryBackground: StateFlow<Boolean> = _useLuxuryBackground.asStateFlow()
    
    private val _language = MutableStateFlow(
        if (prefs.getString(KEY_LANG, "FA") == "EN") AppLanguage.ENGLISH else AppLanguage.PERSIAN
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _themeMode = MutableStateFlow(
        ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _useDynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_COLOR, true))
    val useDynamicColor: StateFlow<Boolean> = _useDynamicColor.asStateFlow()

    private val _useJalali = MutableStateFlow(prefs.getBoolean(KEY_USE_JALALI, true))
    val useJalali: StateFlow<Boolean> = _useJalali.asStateFlow()

    private val _useTehranTz = MutableStateFlow(prefs.getBoolean(KEY_USE_TEHRAN_TZ, true))
    val useTehranTz: StateFlow<Boolean> = _useTehranTz.asStateFlow()

    private val _formatPersianNumbers = MutableStateFlow(prefs.getBoolean(KEY_FORMAT_PERSIAN_NUMBERS, true))
    val formatPersianNumbers: StateFlow<Boolean> = _formatPersianNumbers.asStateFlow()

    private val _isPrivacyModeEnabled = MutableStateFlow(prefs.getBoolean(KEY_PRIVACY_MODE, false))
    val isPrivacyModeEnabled: StateFlow<Boolean> = _isPrivacyModeEnabled.asStateFlow()

    private val _isOnlineQueryEnabled = MutableStateFlow(prefs.getBoolean(KEY_ONLINE_QUERIES, true))
    val isOnlineQueryEnabled: StateFlow<Boolean> = _isOnlineQueryEnabled.asStateFlow()

    private val _isAutoFailoverEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_FAILOVER, true))
    val isAutoFailoverEnabled: StateFlow<Boolean> = _isAutoFailoverEnabled.asStateFlow()

    private val _isRateLimitProtectionEnabled = MutableStateFlow(prefs.getBoolean(KEY_RATE_LIMIT_PROTECTION, true))
    val isRateLimitProtectionEnabled: StateFlow<Boolean> = _isRateLimitProtectionEnabled.asStateFlow()

    private val _isLedgerCacheEnabled = MutableStateFlow(prefs.getBoolean(KEY_LEDGER_CACHE, true))
    val isLedgerCacheEnabled: StateFlow<Boolean> = _isLedgerCacheEnabled.asStateFlow()

    private val _isStrictSslEnabled = MutableStateFlow(prefs.getBoolean(KEY_STRICT_SSL, true))
    val isStrictSslEnabled: StateFlow<Boolean> = _isStrictSslEnabled.asStateFlow()

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        prefs.edit().putString(KEY_LANG, if (lang == AppLanguage.ENGLISH) "EN" else "FA").apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setUseDynamicColor(value: Boolean) {
        _useDynamicColor.value = value
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()
    }

    fun setUseLuxuryBackground(value: Boolean) {
        _useLuxuryBackground.value = value
        prefs.edit().putBoolean(KEY_USE_LUXURY_BACKGROUND, value).apply()
    }

    fun setUseJalali(value: Boolean) {
        _useJalali.value = value
        prefs.edit().putBoolean(KEY_USE_JALALI, value).apply()
    }

    fun setUseTehranTz(value: Boolean) {
        _useTehranTz.value = value
        prefs.edit().putBoolean(KEY_USE_TEHRAN_TZ, value).apply()
    }

    fun setFormatPersianNumbers(value: Boolean) {
        _formatPersianNumbers.value = value
        prefs.edit().putBoolean(KEY_FORMAT_PERSIAN_NUMBERS, value).apply()
    }

    fun setPrivacyModeEnabled(value: Boolean) {
        _isPrivacyModeEnabled.value = value
        prefs.edit().putBoolean(KEY_PRIVACY_MODE, value).apply()
    }

    fun setOnlineQueryEnabled(value: Boolean) {
        _isOnlineQueryEnabled.value = value
        prefs.edit().putBoolean(KEY_ONLINE_QUERIES, value).apply()
    }

    fun setAutoFailoverEnabled(value: Boolean) {
        _isAutoFailoverEnabled.value = value
        prefs.edit().putBoolean(KEY_AUTO_FAILOVER, value).apply()
    }

    fun setRateLimitProtectionEnabled(value: Boolean) {
        _isRateLimitProtectionEnabled.value = value
        prefs.edit().putBoolean(KEY_RATE_LIMIT_PROTECTION, value).apply()
    }

    fun setLedgerCacheEnabled(value: Boolean) {
        _isLedgerCacheEnabled.value = value
        prefs.edit().putBoolean(KEY_LEDGER_CACHE, value).apply()
    }

    fun setStrictSslEnabled(value: Boolean) {
        _isStrictSslEnabled.value = value
        prefs.edit().putBoolean(KEY_STRICT_SSL, value).apply()
    }
}
