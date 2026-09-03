package com.aistudio.orbit

import java.util.Calendar
import java.util.TimeZone

object PersianDateUtils {
    fun formatGregorianToPersian(gregorianDateStr: String?): String? {
        if (gregorianDateStr.isNullOrBlank()) return null
        return try {
            val parts = gregorianDateStr.split("-")
            if (parts.size == 3) {
                val y = parts[0].toInt()
                val m = parts[1].toInt()
                val d = parts[2].toInt()
                gregorianToJalali(y, m, d)
            } else {
                gregorianDateStr
            }
        } catch (e: Exception) {
            gregorianDateStr
        }
    }

    fun formatTimestampToPersian(timestampMs: Long, usePersianDigits: Boolean = false): String {
        val result = getPersianDateTime(timestampMs)
        return if (usePersianDigits) toPersianDigits(result) else result
    }

    fun formatPersianFull(timestampSec: Long, usePersianDigits: Boolean = false): String {
        val ms = if (timestampSec < 100_000_000_000L) timestampSec * 1000L else timestampSec
        return formatTimestampToPersian(ms, usePersianDigits)
    }

    fun formatPersianShort(timestampSec: Long, usePersianDigits: Boolean = false): String {
        val ms = if (timestampSec < 100_000_000_000L) timestampSec * 1000L else timestampSec
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
        cal.timeInMillis = ms
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        val pDate = gregorianToJalali(gy, gm, gd)
        return if (usePersianDigits) toPersianDigits(pDate) else pDate
    }

    fun getPersianDateTime(timestamp: Long): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
        cal.timeInMillis = timestamp
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        
        val pDate = gregorianToJalali(gy, gm, gd)
        val hour = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY))
        val minute = String.format("%02d", cal.get(Calendar.MINUTE))
        return "$pDate $hour:$minute"
    }

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
        return String.format("%04d/%02d/%02d", jy, jm, jd)
    }

    fun toPersianDigits(text: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (ch in text) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
