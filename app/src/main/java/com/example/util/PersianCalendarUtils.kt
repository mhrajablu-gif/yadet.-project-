package com.example.util

import java.util.Calendar
import java.util.TimeZone

data class JalaliDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int    // 1..31
) : Comparable<JalaliDate> {
    override fun compareTo(other: JalaliDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    fun formatFull(): String {
        val monthName = PersianCalendarUtils.jalaliMonthName(month)
        return "${PersianCalendarUtils.toPersianDigits(day)} $monthName ${PersianCalendarUtils.toPersianDigits(year)}"
    }

    fun formatShort(): String {
        val m = month.toString().padStart(2, '0')
        val d = day.toString().padStart(2, '0')
        return PersianCalendarUtils.toPersianDigits("$year/$m/$d")
    }
}

enum class RecurrenceType(val persianTitle: String) {
    NONE("بدون تکرار"),
    DAILY("هر روز"),
    WEEKLY("هر هفته"),
    MONTHLY("هر ماه"),
    YEARLY("هر سال"),
    SELECTED_WEEKDAYS("روزهای انتخابی هفته"),
    EVERY_N_DAYS("هر چند روز یک بار"),
    EVERY_N_HOURS("هر چند ساعت یک بار"),
    EVEN_DAYS("روزهای زوج (شنبه، دوشنبه، چهارشنبه)"),
    ODD_DAYS("روزهای فرد (یکشنبه، سه‌شنبه، پنج‌شنبه)"),
    FIRST_DAY_OF_MONTH("اولین روز ماه"),
    LAST_DAY_OF_MONTH("آخرین روز ماه"),
    FIRST_FRIDAY_OF_MONTH("اولین جمعه ماه"),
    LAST_FRIDAY_OF_MONTH("آخرین جمعه ماه"),
    SATURDAY_TO_WEDNESDAY("شنبه تا چهارشنبه"),
    ALL_EXCEPT_SELECTED("تمام روزها به‌جز روز انتخابی")
}

object PersianCalendarUtils {

    private val jalaliMonths = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    private val gregorianMonthsPersian = listOf(
        "ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن",
        "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر"
    )

    private val weekDaysPersian = listOf(
        "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"
    )

    fun jalaliMonthName(month: Int): String =
        jalaliMonths.getOrElse(month - 1) { "" }

    fun gregorianMonthName(month: Int): String =
        gregorianMonthsPersian.getOrElse(month - 1) { "" }

    fun toPersianDigits(input: Int): String = toPersianDigits(input.toString())

    fun toPersianDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '0'..'9' -> sb.append(('۰'.code + (ch - '0')).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toEnglishDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '۰'..'۹' -> sb.append(('0'.code + (ch - '۰')).toChar())
                in '٠'..'٩' -> sb.append(('0'.code + (ch - '٠')).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun isJalaliLeapYear(jy: Int): Boolean {
        val breaks = intArrayOf(
            -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
            1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
        )
        var jp = breaks[0]
        var jump = 0
        for (i in 1 until breaks.size) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            jp = jm
        }
        var n = jy - jp
        if (jump - n < 6) n = n - jump + ((jump + 4) / 33) * 33
        var leap = (((n + 1) % 33) - 1) % 4
        if (leap == -1) leap = 4
        return leap == 0
    }

    fun jalaliMonthLength(jy: Int, jm: Int): Int = when {
        jm in 1..6 -> 31
        jm in 7..11 -> 30
        jm == 12 -> if (isJalaliLeapYear(jy)) 30 else 29
        else -> 30
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) + gd + gdm[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }
        return JalaliDate(jy, jm, jd)
    }

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        val jy1 = jy + 1595
        var days = -355668 + (365 * jy1) + ((jy1 / 33) * 8) + (((jy1 % 33) + 3) / 4) + jd +
            if (jm < 7) (jm - 1) * 31 else ((jm - 7) * 30) + 186
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            gy += 100 * (--days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val salA = intArrayOf(
            0, 31, if ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)) 29 else 28,
            31, 30, 31, 30, 31, 31, 30, 31, 30, 31
        )
        var gm = 0
        while (gm < 13 && gd > salA[gm]) {
            gd -= salA[gm]
            gm++
        }
        return Triple(gy, gm, gd)
    }

    fun millisToJalali(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): JalaliDate {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun jalaliToMillis(
        jalali: JalaliDate,
        hour: Int = 9,
        minute: Int = 0,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val (gy, gm, gd) = jalaliToGregorian(jalali.year, jalali.month, jalali.day)
        val cal = Calendar.getInstance(timeZone).apply {
            set(gy, gm - 1, gd, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Returns 0 for Saturday (شنبه) .. 6 for Friday (جمعه)
     */
    fun getPersianDayOfWeekIndex(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): Int {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    fun getPersianDayOfWeekName(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String =
        weekDaysPersian[getPersianDayOfWeekIndex(millis, timeZone)]

    fun formatDateTime(
        millis: Long,
        isJalali: Boolean,
        is24Hour: Boolean,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        val dateStr = if (isJalali) {
            millisToJalali(millis, timeZone).formatFull()
        } else {
            val gy = cal.get(Calendar.YEAR)
            val gm = cal.get(Calendar.MONTH) + 1
            val gd = cal.get(Calendar.DAY_OF_MONTH)
            "${toPersianDigits(gd)} ${gregorianMonthName(gm)} ${toPersianDigits(gy)}"
        }
        val timeStr = formatTimeOnly(millis, is24Hour, timeZone)
        return "$dateStr - $timeStr"
    }

    fun formatTimeOnly(
        millis: Long,
        is24Hour: Boolean,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val minStr = minute.toString().padStart(2, '0')
        return if (is24Hour) {
            toPersianDigits("${hour24.toString().padStart(2, '0')}:$minStr")
        } else {
            val period = if (hour24 < 12) "صبح" else "عصر"
            val hour12 = when {
                hour24 == 0 -> 12
                hour24 > 12 -> hour24 - 12
                else -> hour24
            }
            "${toPersianDigits("$hour12:$minStr")} $period"
        }
    }

    fun startOfDayMillis(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDayMillis(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    /**
     * Calculates the next occurrence timestamp after [currentTriggerMillis] for a recurring reminder.
     * Returns null if recurrence is NONE or if the next occurrence exceeds [recurrenceEndMillis].
     */
    fun calculateNextOccurrence(
        currentTriggerMillis: Long,
        recurrenceType: RecurrenceType,
        interval: Int = 1,
        daysMask: Int = 0, // bit 0 = Saturday .. bit 6 = Friday
        recurrenceEndMillis: Long? = null,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long? {
        if (recurrenceType == RecurrenceType.NONE) return null
        val safeInterval = interval.coerceAtLeast(1)
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = currentTriggerMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        val nextMillis: Long = when (recurrenceType) {
            RecurrenceType.NONE -> return null
            RecurrenceType.EVERY_N_HOURS -> {
                cal.add(Calendar.HOUR_OF_DAY, safeInterval)
                cal.timeInMillis
            }
            RecurrenceType.DAILY -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            RecurrenceType.EVERY_N_DAYS -> {
                cal.add(Calendar.DAY_OF_YEAR, safeInterval)
                cal.timeInMillis
            }
            RecurrenceType.WEEKLY -> {
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                cal.timeInMillis
            }
            RecurrenceType.MONTHLY -> {
                val j = millisToJalali(currentTriggerMillis, timeZone)
                val nextMonth = if (j.month == 12) 1 else j.month + 1
                val nextYear = if (j.month == 12) j.year + 1 else j.year
                val maxDay = jalaliMonthLength(nextYear, nextMonth)
                jalaliToMillis(JalaliDate(nextYear, nextMonth, j.day.coerceAtMost(maxDay)), hour, minute, timeZone)
            }
            RecurrenceType.YEARLY -> {
                val j = millisToJalali(currentTriggerMillis, timeZone)
                val nextYear = j.year + 1
                val maxDay = jalaliMonthLength(nextYear, j.month)
                jalaliToMillis(JalaliDate(nextYear, j.month, j.day.coerceAtMost(maxDay)), hour, minute, timeZone)
            }
            RecurrenceType.EVEN_DAYS -> {
                findNextMatchingWeekday(cal, setOf(0, 2, 4)) // شنبه، دوشنبه، چهارشنبه
            }
            RecurrenceType.ODD_DAYS -> {
                findNextMatchingWeekday(cal, setOf(1, 3, 5)) // یکشنبه، سه‌شنبه، پنج‌شنبه
            }
            RecurrenceType.SATURDAY_TO_WEDNESDAY -> {
                findNextMatchingWeekday(cal, setOf(0, 1, 2, 3, 4))
            }
            RecurrenceType.SELECTED_WEEKDAYS -> {
                val allowed = (0..6).filter { (daysMask and (1 shl it)) != 0 }.toSet()
                if (allowed.isEmpty()) {
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                    cal.timeInMillis
                } else {
                    findNextMatchingWeekday(cal, allowed)
                }
            }
            RecurrenceType.ALL_EXCEPT_SELECTED -> {
                val excluded = (0..6).filter { (daysMask and (1 shl it)) != 0 }.toSet()
                val allowed = (0..6).filter { it !in excluded }.toSet()
                if (allowed.isEmpty()) return null
                findNextMatchingWeekday(cal, allowed)
            }
            RecurrenceType.FIRST_DAY_OF_MONTH -> {
                val j = millisToJalali(currentTriggerMillis, timeZone)
                val nextMonth = if (j.month == 12) 1 else j.month + 1
                val nextYear = if (j.month == 12) j.year + 1 else j.year
                jalaliToMillis(JalaliDate(nextYear, nextMonth, 1), hour, minute, timeZone)
            }
            RecurrenceType.LAST_DAY_OF_MONTH -> {
                val j = millisToJalali(currentTriggerMillis, timeZone)
                val nextMonth = if (j.month == 12) 1 else j.month + 1
                val nextYear = if (j.month == 12) j.year + 1 else j.year
                val lastDay = jalaliMonthLength(nextYear, nextMonth)
                jalaliToMillis(JalaliDate(nextYear, nextMonth, lastDay), hour, minute, timeZone)
            }
            RecurrenceType.FIRST_FRIDAY_OF_MONTH -> {
                val j = millisToJalali(currentTriggerMillis, timeZone)
                val nextMonth = if (j.month == 12) 1 else j.month + 1
                val nextYear = if (j.month == 12) j.year + 1 else j.year
                findNthFridayOfJalaliMonth(nextYear, nextMonth, first = true, hour, minute, timeZone)
            }
            RecurrenceType.LAST_FRIDAY_OF_MONTH -> {
                val j = millisToJalali(currentTriggerMillis, timeZone)
                val nextMonth = if (j.month == 12) 1 else j.month + 1
                val nextYear = if (j.month == 12) j.year + 1 else j.year
                findNthFridayOfJalaliMonth(nextYear, nextMonth, first = false, hour, minute, timeZone)
            }
        }

        if (recurrenceEndMillis != null && nextMillis > recurrenceEndMillis) {
            return null
        }
        return nextMillis
    }

    private fun findNextMatchingWeekday(baseCal: Calendar, allowedPersianDays: Set<Int>): Long {
        val cal = baseCal.clone() as Calendar
        for (i in 1..7) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val pDay = getPersianDayOfWeekIndex(cal.timeInMillis, cal.timeZone)
            if (pDay in allowedPersianDays) {
                return cal.timeInMillis
            }
        }
        return cal.timeInMillis
    }

    private fun findNthFridayOfJalaliMonth(
        jy: Int,
        jm: Int,
        first: Boolean,
        hour: Int,
        minute: Int,
        timeZone: TimeZone
    ): Long {
        val maxDay = jalaliMonthLength(jy, jm)
        val range = if (first) (1..maxDay) else (maxDay downTo 1)
        for (d in range) {
            val m = jalaliToMillis(JalaliDate(jy, jm, d), hour, minute, timeZone)
            if (getPersianDayOfWeekIndex(m, timeZone) == 6) {
                return m
            }
        }
        return jalaliToMillis(JalaliDate(jy, jm, 1), hour, minute, timeZone)
    }
}
