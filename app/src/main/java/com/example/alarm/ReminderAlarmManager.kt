package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.ReminderEntity
import com.example.data.local.ReminderStatus

object ReminderAlarmManager {

    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
    const val EXTRA_REMINDER_DESC = "extra_reminder_desc"
    const val EXTRA_REMINDER_TIME = "extra_reminder_time"

    fun scheduleReminder(
        context: Context,
        reminder: ReminderEntity,
        leadMinutes: Int = 0
    ) {
        if (reminder.status != ReminderStatus.ACTIVE.name) {
            cancelReminder(context, reminder.id)
            return
        }
        val now = System.currentTimeMillis()
        val leadMillis = leadMinutes.coerceAtLeast(0) * 60_000L
        val targetMillis = if (reminder.triggerAtMillis - leadMillis > now) {
            reminder.triggerAtMillis - leadMillis
        } else {
            reminder.triggerAtMillis
        }
        if (targetMillis <= now) return

        scheduleExactAt(
            context = context,
            reminderId = reminder.id,
            title = reminder.title,
            description = reminder.description,
            originalTriggerMillis = reminder.triggerAtMillis,
            alarmAtMillis = targetMillis
        )
    }

    fun scheduleSnooze(
        context: Context,
        reminderId: Long,
        title: String,
        description: String,
        snoozeMinutes: Int
    ) {
        val alarmAtMillis = System.currentTimeMillis() + snoozeMinutes * 60_000L
        scheduleExactAt(
            context = context,
            reminderId = reminderId,
            title = title,
            description = description,
            originalTriggerMillis = alarmAtMillis,
            alarmAtMillis = alarmAtMillis
        )
    }

    private fun scheduleExactAt(
        context: Context,
        reminderId: Long,
        title: String,
        description: String,
        originalTriggerMillis: Long,
        alarmAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_REMINDER_TITLE, title)
            putExtra(EXTRA_REMINDER_DESC, description)
            putExtra(EXTRA_REMINDER_TIME, originalTriggerMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
