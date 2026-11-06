package com.example

import com.example.backup.BackupManager
import com.example.util.JalaliDate
import com.example.util.PersianCalendarUtils
import com.example.util.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YadetUnitTest {

    @Test
    fun testJalaliGregorianRoundTrip() {
        val testDates = listOf(
            JalaliDate(1405, 8, 14),
            JalaliDate(1405, 8, 25),
            JalaliDate(1405, 8, 30),
            JalaliDate(1405, 1, 15),
            JalaliDate(1405, 6, 31),
            JalaliDate(1403, 12, 30),
            JalaliDate(1404, 12, 29),
            JalaliDate(1405, 1, 1)
        )
        for (jalali in testDates) {
            val millis = PersianCalendarUtils.jalaliToMillis(jalali, 8, 0)
            val back = PersianCalendarUtils.millisToJalali(millis)
            assertEquals("Failed for $jalali", jalali, back)
        }
    }

    @Test
    fun testJalaliLeapYear() {
        assertTrue(PersianCalendarUtils.isJalaliLeapYear(1403))
        assertFalse(PersianCalendarUtils.isJalaliLeapYear(1404))
        assertEquals(30, PersianCalendarUtils.jalaliMonthLength(1403, 12))
        assertEquals(29, PersianCalendarUtils.jalaliMonthLength(1404, 12))
    }

    @Test
    fun testRecurrenceNextOccurrence() {
        val base = PersianCalendarUtils.jalaliToMillis(JalaliDate(1405, 8, 14), 8, 0)
        val nextDaily = PersianCalendarUtils.calculateNextOccurrence(base, RecurrenceType.DAILY)
        assertNotNull(nextDaily)
        assertEquals(JalaliDate(1405, 8, 15), PersianCalendarUtils.millisToJalali(nextDaily!!))

        val endBeforeNext = base + 3600_000L
        val expired = PersianCalendarUtils.calculateNextOccurrence(
            base,
            RecurrenceType.DAILY,
            recurrenceEndMillis = endBeforeNext
        )
        assertNull(expired)
    }

    @Test
    fun testBackupValidationRejectsCorruptedJson() {
        val invalid = BackupManager.validateBackup("{invalid_json}")
        assertFalse(invalid.isValid)
    }
}
