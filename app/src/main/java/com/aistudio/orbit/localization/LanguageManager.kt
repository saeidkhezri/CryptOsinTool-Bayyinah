package com.aistudio.orbit.localization

import android.content.Context
import androidx.compose.ui.unit.LayoutDirection
import com.aistudio.orbit.repository.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class LanguageManager private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("orbit_lang_prefs", Context.MODE_PRIVATE)

    private val _currentLanguage = MutableStateFlow(
        if (prefs.getString("selected_lang", "FA") == "EN") AppLanguage.ENGLISH else AppLanguage.PERSIAN
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    val isPersian: Boolean
        get() = _currentLanguage.value == AppLanguage.PERSIAN

    val layoutDirection: LayoutDirection
        get() = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
        prefs.edit().putString("selected_lang", if (lang == AppLanguage.ENGLISH) "EN" else "FA").apply()
    }

    fun toggleLanguage() {
        val next = if (isPersian) AppLanguage.ENGLISH else AppLanguage.PERSIAN
        setLanguage(next)
    }

    fun getStrings(): ForensicStrings {
        return AppLocalization.getStrings(_currentLanguage.value)
    }

    fun formatNumber(num: Long): String {
        return if (isPersian) {
            val s = "%,d".format(Locale.US, num)
            toPersianDigits(s)
        } else {
            "%,d".format(Locale.US, num)
        }
    }

    fun formatNumber(num: Double): String {
        return if (isPersian) {
            val s = "%,.4f".format(Locale.US, num)
            toPersianDigits(s)
        } else {
            "%,.4f".format(Locale.US, num)
        }
    }

    private fun toPersianDigits(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    companion object {
        @Volatile
        private var instance: LanguageManager? = null

        fun getInstance(context: Context): LanguageManager {
            return instance ?: synchronized(this) {
                instance ?: LanguageManager(context).also { instance = it }
            }
        }
    }
}
