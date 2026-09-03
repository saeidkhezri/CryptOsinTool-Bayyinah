package com.aistudio.orbit.forensics

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TemporalUtils {

    private val TEHRAN_TIMEZONE = TimeZone.getTimeZone("Asia/Tehran")
    private val UTC_TIMEZONE = TimeZone.getTimeZone("UTC")

    /**
     * Converts English numerals to Persian numerals if isPersian is true.
     */
    fun formatNumber(str: String, isPersian: Boolean): String {
        if (!isPersian) return str
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (ch in str) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatNumber(number: Number, isPersian: Boolean): String {
        return formatNumber(number.toString(), isPersian)
    }

    fun formatCryptoAmount(
        amountSat: Long,
        network: com.aistudio.orbit.model.BlockchainNetwork = com.aistudio.orbit.model.BlockchainNetwork.BITCOIN,
        isPersian: Boolean = false,
        customSymbol: String? = null
    ): String {
        val symbol = customSymbol ?: network.symbol
        val divisor = 100_000_000.0
        val value = amountSat.toDouble() / divisor
        val formatted = String.format(Locale.US, "%.6f", value).trimEnd('0').let {
            if (it.endsWith(".")) it + "0" else it
        }
        val localizedNum = if (isPersian) formatNumber(formatted, true) else formatted
        return "$localizedNum $symbol"
    }

    fun formatBtc(amountSat: Long, isPersian: Boolean): String {
        return formatCryptoAmount(amountSat, com.aistudio.orbit.model.BlockchainNetwork.BITCOIN, isPersian)
    }

    /**
     * Format timestamp (in seconds or millis) into localized display string.
     */
    fun formatDateTime(
        epochSeconds: Long,
        isPersian: Boolean = true,
        useTehranTime: Boolean = true
    ): String {
        if (epochSeconds <= 0) return if (isPersian) "نامشخص" else "N/A"
        val millis = if (epochSeconds < 100_000_000_000L) epochSeconds * 1000L else epochSeconds
        val targetTz = if (useTehranTime) TEHRAN_TIMEZONE else UTC_TIMEZONE

        val cal = Calendar.getInstance(targetTz)
        cal.timeInMillis = millis

        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val min = cal.get(Calendar.MINUTE)
        val sec = cal.get(Calendar.SECOND)

        val timeStr = String.format(Locale.US, "%02d:%02d:%02d", hour, min, sec)
        val tzSuffix = if (useTehranTime) " (تهران)" else " (UTC)"

        return if (isPersian) {
            val jalali = gregorianToJalali(gy, gm, gd)
            val fullStr = "$jalali $timeStr$tzSuffix"
            formatNumber(fullStr, true)
        } else {
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", gy, gm, gd)
            val engTzSuffix = if (useTehranTime) " (Tehran)" else " (UTC)"
            "$dateStr $timeStr$engTzSuffix"
        }
    }

    /**
     * Converts Gregorian year, month (1-12), day to Jalali "YYYY/MM/DD"
     */
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): String {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30)

        val isGregorianLeap = (gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)
        if (isGregorianLeap) {
            gDaysInMonth[2] = 29
        }

        var gDayNo = 0
        for (i in 1 until gm) {
            gDayNo += gDaysInMonth[i]
        }
        gDayNo += gd

        var jDayNo = gDayNo - 79
        var jy = gy - 621

        if (jDayNo <= 0) {
            jy -= 1
            val isPriorJalaliLeap = (jy % 4 == 0)
            jDayNo += if (isPriorJalaliLeap) 366 else 365
        }

        var jm = 1
        for (i in 1..12) {
            val days = jDaysInMonth[i]
            if (jDayNo <= days) {
                jm = i
                break
            }
            jDayNo -= days
        }
        val jd = jDayNo
        return String.format(Locale.US, "%04d/%02d/%02d", jy, jm, jd)
    }

    /**
     * Calculates 24-hour histogram distribution (00:00 to 23:00) in Asia/Tehran timezone.
     */
    fun computeHourlyActivityDistribution(timestampsSec: List<Long>): IntArray {
        val distribution = IntArray(24) { 0 }
        val cal = Calendar.getInstance(TEHRAN_TIMEZONE)
        for (ts in timestampsSec) {
            if (ts <= 0) continue
            val millis = if (ts < 100_000_000_000L) ts * 1000L else ts
            cal.timeInMillis = millis
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (hour in 0..23) {
                distribution[hour]++
            }
        }
        return distribution
    }
}
