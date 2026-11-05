package com.example.ai

import com.example.data.local.AppThemeMode
import com.example.data.local.CalendarSystem
import com.example.data.local.MemoryItemEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.ReminderPriority
import com.example.data.local.ReminderStatus
import com.example.data.repository.YadetRepository
import com.example.util.JalaliDate
import com.example.util.PersianCalendarUtils
import com.example.util.RecurrenceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class ToolExecutionResult(
    val success: Boolean,
    val resultJson: String,
    val humanSummary: String
)

class ToolExecutor(private val repository: YadetRepository) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun executeTool(name: String, argsJson: String): ToolExecutionResult {
        val args = runCatching { JSONObject(argsJson) }.getOrElse { JSONObject() }
        val result = when (name) {
            "create_reminder" -> handleCreateReminder(args)
            "update_reminder" -> handleUpdateReminder(args)
            "delete_reminder" -> handleDeleteReminder(args)
            "search_reminders" -> handleSearchReminders(args)
            "list_reminders" -> handleListReminders(args)
            "get_reminder_details" -> handleGetReminderDetails(args)
            "complete_reminder" -> handleCompleteReminder(args)
            "restore_reminder" -> handleRestoreReminder(args)
            "change_recurrence" -> handleChangeRecurrence(args)
            "change_notification_settings" -> handleChangeNotificationSettings(args)
            "update_app_settings" -> handleUpdateAppSettings(args)
            "get_current_datetime" -> handleGetCurrentDatetime()
            "get_calendar_information" -> handleGetCalendarInfo(args)
            "get_user_preferences" -> handleGetUserPreferences()
            "save_memory" -> handleSaveMemory(args)
            "web_search" -> handleWebSearch(args)
            else -> ToolExecutionResult(
                false,
                JSONObject().put("error", "ابزار ناشناخته: $name").toString(),
                "ابزار ناشناخته"
            )
        }
        repository.logToolExecution(name, argsJson, result.resultJson, result.success)
        return result
    }

    private suspend fun handleCreateReminder(args: JSONObject): ToolExecutionResult {
        val title = args.optString("title", "").trim()
        if (title.isBlank()) {
            return ToolExecutionResult(
                false,
                JSONObject().put("error", "عنوان یادآور مشخص نشده است. لطفاً عنوان را بپرسید.").toString(),
                "عنوان یادآور مشخص نیست"
            )
        }
        val description = args.optString("description", "").trim()
        val timeExpr = args.optString("datetime", "").ifBlank { args.optString("time_expression", "") }
        val triggerMillis = parsePersianDateTimeExpression(timeExpr)
            ?: return ToolExecutionResult(
                false,
                JSONObject().put("error", "زمان یادآور نامعتبر یا نامشخص است: $timeExpr").toString(),
                "زمان یادآور نامشخص است"
            )

        val priority = when (args.optString("priority", "NORMAL").uppercase()) {
            "LOW", "کم" -> ReminderPriority.LOW.level
            "HIGH", "مهم" -> ReminderPriority.HIGH.level
            "URGENT", "فوری" -> ReminderPriority.URGENT.level
            else -> ReminderPriority.NORMAL.level
        }

        val recStr = args.optString("recurrence", "NONE").uppercase()
        val recType = runCatching { RecurrenceType.valueOf(recStr) }.getOrDefault(RecurrenceType.NONE)
        val interval = args.optInt("recurrence_interval", 1).coerceAtLeast(1)
        val endExpr = args.optString("recurrence_end", "")
        val endMillis = if (endExpr.isNotBlank()) parsePersianDateTimeExpression(endExpr) else null

        val entity = ReminderEntity(
            title = title,
            description = description,
            triggerAtMillis = triggerMillis,
            priority = priority,
            recurrenceType = recType.name,
            recurrenceInterval = interval,
            recurrenceStartMillis = if (recType != RecurrenceType.NONE) triggerMillis else null,
            recurrenceEndMillis = endMillis,
            location = args.optString("location", "")
        )
        val id = repository.createReminder(entity)
        val formatted = PersianCalendarUtils.formatDateTime(triggerMillis, isJalali = true, is24Hour = true)
        val res = JSONObject()
            .put("status", "created")
            .put("id", id)
            .put("title", title)
            .put("formatted_time", formatted)
            .put("recurrence", recType.persianTitle)
        return ToolExecutionResult(true, res.toString(), "یادآور «$title» برای $formatted ثبت شد.")
    }

    private suspend fun handleUpdateReminder(args: JSONObject): ToolExecutionResult {
        val id = args.optLong("id", -1L)
        val existing = if (id > 0) repository.getReminderById(id) else null
        if (existing == null) {
            return ToolExecutionResult(
                false,
                JSONObject().put("error", "یادآور با شناسه $id یافت نشد.").toString(),
                "یادآور یافت نشد"
            )
        }
        val newTitle = args.optString("title", existing.title).ifBlank { existing.title }
        val newDesc = if (args.has("description")) args.optString("description") else existing.description
        val timeExpr = args.optString("datetime", "")
        val newTrigger = if (timeExpr.isNotBlank()) {
            parsePersianDateTimeExpression(timeExpr, existing.triggerAtMillis) ?: existing.triggerAtMillis
        } else {
            existing.triggerAtMillis
        }
        val updated = existing.copy(
            title = newTitle,
            description = newDesc,
            triggerAtMillis = newTrigger
        )
        repository.updateReminder(updated)
        val formatted = PersianCalendarUtils.formatDateTime(newTrigger, isJalali = true, is24Hour = true)
        return ToolExecutionResult(
            true,
            JSONObject().put("status", "updated").put("id", id).put("formatted_time", formatted).toString(),
            "یادآور «$newTitle» به‌روزرسانی شد ($formatted)."
        )
    }

    private suspend fun handleDeleteReminder(args: JSONObject): ToolExecutionResult {
        val id = args.optLong("id", -1L)
        val existing = if (id > 0) repository.getReminderById(id) else null
        if (existing == null) {
            return ToolExecutionResult(
                false,
                JSONObject().put("error", "یادآور با شناسه $id یافت نشد.").toString(),
                "یادآور یافت نشد"
            )
        }
        repository.moveReminderToTrash(existing)
        return ToolExecutionResult(
            true,
            JSONObject().put("status", "trashed").put("id", id).put("title", existing.title).toString(),
            "یادآور «${existing.title}» به سطل زباله منتقل شد."
        )
    }

    private suspend fun handleSearchReminders(args: JSONObject): ToolExecutionResult {
        val query = args.optString("query", "").trim()
        val list = if (query.isBlank()) {
            repository.remindersFlow.first()
        } else {
            repository.searchReminders(query)
        }
        val arr = JSONArray()
        list.take(20).forEach { r ->
            arr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("title", r.title)
                    .put("status", r.status)
                    .put("time", PersianCalendarUtils.formatDateTime(r.triggerAtMillis, true, true))
                    .put("recurrence", r.recurrenceType)
            )
        }
        return ToolExecutionResult(
            true,
            JSONObject().put("count", list.size).put("reminders", arr).toString(),
            "${PersianCalendarUtils.toPersianDigits(list.size)} یادآور یافت شد."
        )
    }

    private suspend fun handleListReminders(args: JSONObject): ToolExecutionResult {
        val filter = args.optString("filter", "ACTIVE").uppercase()
        val all = repository.remindersFlow.first()
        val now = System.currentTimeMillis()
        val startToday = PersianCalendarUtils.startOfDayMillis(now)
        val endToday = PersianCalendarUtils.endOfDayMillis(now)

        val filtered = when (filter) {
            "TODAY" -> all.filter { it.triggerAtMillis in startToday..endToday }
            "OVERDUE" -> all.filter { it.status == ReminderStatus.ACTIVE.name && it.triggerAtMillis < now }
            "COMPLETED" -> all.filter { it.status == ReminderStatus.COMPLETED.name }
            else -> all.filter { it.status == ReminderStatus.ACTIVE.name }
        }
        val arr = JSONArray()
        filtered.take(25).forEach { r ->
            arr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("title", r.title)
                    .put("time", PersianCalendarUtils.formatDateTime(r.triggerAtMillis, true, true))
                    .put("priority", ReminderPriority.fromLevel(r.priority).persianLabel)
            )
        }
        return ToolExecutionResult(
            true,
            JSONObject().put("count", filtered.size).put("reminders", arr).toString(),
            "فهرست یادآورها (${PersianCalendarUtils.toPersianDigits(filtered.size)} مورد)"
        )
    }

    private suspend fun handleGetReminderDetails(args: JSONObject): ToolExecutionResult {
        val id = args.optLong("id", -1L)
        val r = repository.getReminderById(id)
            ?: return ToolExecutionResult(false, JSONObject().put("error", "یافت نشد").toString(), "یادآور یافت نشد")
        val obj = JSONObject()
            .put("id", r.id)
            .put("title", r.title)
            .put("description", r.description)
            .put("time", PersianCalendarUtils.formatDateTime(r.triggerAtMillis, true, true))
            .put("status", r.status)
            .put("recurrence", r.recurrenceType)
            .put("location", r.location)
        return ToolExecutionResult(true, obj.toString(), "جزئیات یادآور «${r.title}»")
    }

    private suspend fun handleCompleteReminder(args: JSONObject): ToolExecutionResult {
        val id = args.optLong("id", -1L)
        val r = repository.getReminderById(id)
            ?: return ToolExecutionResult(false, JSONObject().put("error", "یافت نشد").toString(), "یادآور یافت نشد")
        repository.completeReminder(r)
        return ToolExecutionResult(
            true,
            JSONObject().put("status", "completed").put("id", id).toString(),
            "یادآور «${r.title}» انجام شد."
        )
    }

    private suspend fun handleRestoreReminder(args: JSONObject): ToolExecutionResult {
        val id = args.optLong("id", -1L)
        val r = repository.getReminderById(id)
            ?: return ToolExecutionResult(false, JSONObject().put("error", "یافت نشد").toString(), "یادآور یافت نشد")
        repository.restoreReminderFromTrash(r)
        return ToolExecutionResult(
            true,
            JSONObject().put("status", "restored").put("id", id).toString(),
            "یادآور «${r.title}» بازیابی شد."
        )
    }

    private suspend fun handleChangeRecurrence(args: JSONObject): ToolExecutionResult {
        val id = args.optLong("id", -1L)
        val r = repository.getReminderById(id)
            ?: return ToolExecutionResult(false, JSONObject().put("error", "یافت نشد").toString(), "یادآور یافت نشد")
        val recType = runCatching {
            RecurrenceType.valueOf(args.optString("recurrence", "NONE").uppercase())
        }.getOrDefault(RecurrenceType.NONE)
        val interval = args.optInt("interval", 1).coerceAtLeast(1)
        repository.updateReminder(r.copy(recurrenceType = recType.name, recurrenceInterval = interval))
        return ToolExecutionResult(
            true,
            JSONObject().put("status", "updated").put("recurrence", recType.persianTitle).toString(),
            "تکرار یادآور «${r.title}» به «${recType.persianTitle}» تغییر یافت."
        )
    }

    private suspend fun handleChangeNotificationSettings(args: JSONObject): ToolExecutionResult {
        val lead = if (args.has("lead_minutes")) args.optInt("lead_minutes") else null
        val sound = if (args.has("sound")) args.optBoolean("sound") else null
        val vibration = if (args.has("vibration")) args.optBoolean("vibration") else null
        val snooze = if (args.has("snooze_minutes")) args.optInt("snooze_minutes") else null
        repository.settingsStore.updateNotificationSettings(lead, sound, vibration, snooze)
        repository.rescheduleAllActiveAlarms()
        return ToolExecutionResult(
            true,
            JSONObject().put("status", "updated").toString(),
            "تنظیمات اعلان‌ها با موفقیت به‌روزرسانی شد."
        )
    }

    private suspend fun handleUpdateAppSettings(args: JSONObject): ToolExecutionResult {
        if (args.has("theme")) {
            val mode = when (args.optString("theme").uppercase()) {
                "DARK", "تاریک" -> AppThemeMode.DARK
                "LIGHT", "روشن" -> AppThemeMode.LIGHT
                else -> AppThemeMode.SYSTEM
            }
            repository.settingsStore.updateThemeMode(mode)
        }
        if (args.has("calendar")) {
            val cal = when (args.optString("calendar").uppercase()) {
                "GREGORIAN", "میلادی" -> CalendarSystem.GREGORIAN
                else -> CalendarSystem.JALALI
            }
            repository.settingsStore.updateCalendarSystem(cal)
        }
        if (args.has("is_24_hour")) {
            repository.settingsStore.updateIs24Hour(args.optBoolean("is_24_hour"))
        }
        return ToolExecutionResult(
            true,
            JSONObject().put("status", "updated").toString(),
            "تنظیمات برنامه اعمال شد."
        )
    }

    private fun handleGetCurrentDatetime(): ToolExecutionResult {
        val now = System.currentTimeMillis()
        val jalali = PersianCalendarUtils.millisToJalali(now)
        val weekday = PersianCalendarUtils.getPersianDayOfWeekName(now)
        val time = PersianCalendarUtils.formatTimeOnly(now, true)
        val obj = JSONObject()
            .put("timestamp", now)
            .put("jalali_date", jalali.formatFull())
            .put("weekday", weekday)
            .put("time_24h", time)
            .put("timezone", TimeZone.getDefault().id)
        return ToolExecutionResult(true, obj.toString(), "$weekday ${jalali.formatFull()} - $time")
    }

    private fun handleGetCalendarInfo(args: JSONObject): ToolExecutionResult {
        val now = System.currentTimeMillis()
        val j = PersianCalendarUtils.millisToJalali(now)
        val daysInMonth = PersianCalendarUtils.jalaliMonthLength(j.year, j.month)
        val isLeap = PersianCalendarUtils.isJalaliLeapYear(j.year)
        val obj = JSONObject()
            .put("current_jalali", j.formatFull())
            .put("days_in_current_month", daysInMonth)
            .put("is_leap_year", isLeap)
        return ToolExecutionResult(true, obj.toString(), "اطلاعات تقویم شمسی")
    }

    private suspend fun handleGetUserPreferences(): ToolExecutionResult {
        val s = repository.settingsStore.settingsFlow.first()
        val obj = JSONObject()
            .put("theme", s.themeMode.persianLabel)
            .put("calendar", s.calendarSystem.persianLabel)
            .put("is_24_hour", s.is24Hour)
            .put("lead_minutes", s.defaultLeadMinutes)
            .put("snooze_minutes", s.defaultSnoozeMinutes)
        return ToolExecutionResult(true, obj.toString(), "ترجیحات کاربر")
    }

    private suspend fun handleSaveMemory(args: JSONObject): ToolExecutionResult {
        val key = args.optString("key", "").trim()
        val value = args.optString("value", "").trim()
        if (key.isBlank() || value.isBlank()) {
            return ToolExecutionResult(false, JSONObject().put("error", "کلید یا مقدار خالی است").toString(), "خطا در ذخیره حافظه")
        }
        val type = args.optString("type", "LONG_TERM").uppercase()
        repository.saveMemory(MemoryItemEntity(type = type, key = key, value = value))
        return ToolExecutionResult(
            true,
            JSONObject().put("status", "saved").put("key", key).toString(),
            "در حافظه دستیار ذخیره شد: $key"
        )
    }

    private suspend fun handleWebSearch(args: JSONObject): ToolExecutionResult = withContext(Dispatchers.IO) {
        val settings = repository.settingsStore.settingsFlow.first()
        if (!settings.webSearchEnabled) {
            return@withContext ToolExecutionResult(
                false,
                JSONObject().put("error", "جستجوی اینترنتی در تنظیمات غیرفعال است.").toString(),
                "جستجوی اینترنتی غیرفعال است"
            )
        }
        val query = args.optString("query", "").trim()
        if (query.isBlank()) {
            return@withContext ToolExecutionResult(false, JSONObject().put("error", "عبارت جستجو خالی است").toString(), "عبارت جستجو خالی است")
        }
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
            val req = Request.Builder().url(url).header("User-Agent", "YadetAndroid/1.0").build()
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string() ?: "{}"
            val json = JSONObject(body)
            val abstractText = json.optString("AbstractText", "")
            val abstractUrl = json.optString("AbstractURL", "")
            val heading = json.optString("Heading", "")
            val resultObj = JSONObject()
                .put("heading", heading)
                .put("summary", abstractText.ifBlank { "نتیجه مستقیمی یافت نشد." })
                .put("source_url", abstractUrl)
            ToolExecutionResult(true, resultObj.toString(), "نتیجه جستجو برای «$query»")
        } catch (e: Exception) {
            ToolExecutionResult(false, JSONObject().put("error", e.localizedMessage).toString(), "خطا در جستجوی اینترنتی")
        }
    }

    fun parsePersianDateTimeExpression(expr: String, baseMillis: Long = System.currentTimeMillis()): Long? {
        val clean = PersianCalendarUtils.toEnglishDigits(expr).trim()
        if (clean.isBlank()) return null

        // Epoch millis
        clean.toLongOrNull()?.let { if (it > 1_000_000_000_000L) return it }

        val cal = Calendar.getInstance().apply { timeInMillis = baseMillis }

        // Relative minutes/hours
        Regex("(\\d+)\\s*دقیقه\\s*(دیگر|بعد)").find(clean)?.let {
            val mins = it.groupValues[1].toIntOrNull() ?: 0
            cal.add(Calendar.MINUTE, mins)
            return cal.timeInMillis
        }
        if (clean.contains("نیم ساعت دیگر") || clean.contains("نیم ساعت بعد")) {
            cal.add(Calendar.MINUTE, 30)
            return cal.timeInMillis
        }
        Regex("(\\d+)\\s*ساعت\\s*(دیگر|بعد)").find(clean)?.let {
            val hrs = it.groupValues[1].toIntOrNull() ?: 0
            cal.add(Calendar.HOUR_OF_DAY, hrs)
            return cal.timeInMillis
        }

        // Extract explicit hour:minute if present
        var hour = 9
        var minute = 0
        var hasExplicitTime = false
        Regex("(\\d{1,2}):(\\d{2})").find(clean)?.let {
            hour = it.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: 9
            minute = it.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
            hasExplicitTime = true
        } ?: Regex("ساعت\\s*(\\d{1,2})").find(clean)?.let {
            var h = it.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: 9
            if ((clean.contains("عصر") || clean.contains("شب")) && h in 1..11) h += 12
            hour = h
            minute = 0
            hasExplicitTime = true
        }

        if (clean.contains("فردا همین ساعت")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }
        if (clean.contains("پس‌فردا") || clean.contains("پس فردا")) {
            cal.add(Calendar.DAY_OF_YEAR, 2)
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            return cal.timeInMillis
        }
        if (clean.contains("فردا")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            return cal.timeInMillis
        }
        if (clean.contains("امروز")) {
            if (!hasExplicitTime) {
                cal.add(Calendar.HOUR_OF_DAY, 1)
            } else {
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
            }
            return cal.timeInMillis
        }
        if (clean.contains("هفته آینده")) {
            cal.add(Calendar.WEEK_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            return cal.timeInMillis
        }
        if (clean.contains("ماه آینده")) {
            cal.add(Calendar.MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            return cal.timeInMillis
        }
        if (clean.contains("سال آینده")) {
            val j = PersianCalendarUtils.millisToJalali(baseMillis)
            return PersianCalendarUtils.jalaliToMillis(JalaliDate(j.year + 1, 1, 1), hour, minute)
        }

        // Jalali YYYY/MM/DD or YYYY-MM-DD
        Regex("(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})").find(clean)?.let {
            val y = it.groupValues[1].toInt()
            val m = it.groupValues[2].toInt().coerceIn(1, 12)
            val d = it.groupValues[3].toInt().coerceIn(1, 31)
            return if (y in 1300..1500) {
                PersianCalendarUtils.jalaliToMillis(JalaliDate(y, m, d), hour, minute)
            } else {
                cal.set(y, m - 1, d, hour, minute, 0)
                cal.timeInMillis
            }
        }

        if (hasExplicitTime) {
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            if (cal.timeInMillis <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis
        }

        return null
    }
}
