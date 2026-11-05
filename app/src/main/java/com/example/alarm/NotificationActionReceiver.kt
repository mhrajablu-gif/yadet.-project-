package com.example.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.ReminderStatus
import com.example.data.local.SettingsDataStore
import com.example.data.local.YadetDatabase
import com.example.util.PersianCalendarUtils
import com.example.util.RecurrenceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.TimeZone

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE = "com.example.yadet.ACTION_COMPLETE"
        const val ACTION_SNOOZE_5 = "com.example.yadet.ACTION_SNOOZE_5"
        const val ACTION_SNOOZE_10 = "com.example.yadet.ACTION_SNOOZE_10"
        const val ACTION_SNOOZE_15 = "com.example.yadet.ACTION_SNOOZE_15"
        const val ACTION_SNOOZE_30 = "com.example.yadet.ACTION_SNOOZE_30"
        const val ACTION_SNOOZE_60 = "com.example.yadet.ACTION_SNOOZE_60"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderAlarmManager.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val title = intent.getStringExtra(ReminderAlarmManager.EXTRA_REMINDER_TITLE) ?: "یادآور"
        val desc = intent.getStringExtra(ReminderAlarmManager.EXTRA_REMINDER_DESC) ?: ""

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId.toInt())

        val snoozeMinutes = when (intent.action) {
            ACTION_SNOOZE_5 -> 5
            ACTION_SNOOZE_10 -> 10
            ACTION_SNOOZE_15 -> 15
            ACTION_SNOOZE_30 -> 30
            ACTION_SNOOZE_60 -> 60
            else -> null
        }

        if (snoozeMinutes != null) {
            ReminderAlarmManager.scheduleSnooze(
                context = context,
                reminderId = reminderId,
                title = title,
                description = desc,
                snoozeMinutes = snoozeMinutes
            )
            return
        }

        if (intent.action == ACTION_COMPLETE) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = YadetDatabase.getInstance(context)
                    val dao = db.reminderDao()
                    val reminder = dao.getById(reminderId)
                    if (reminder != null) {
                        val recType = runCatching {
                            RecurrenceType.valueOf(reminder.recurrenceType)
                        }.getOrDefault(RecurrenceType.NONE)

                        val nextMillis = PersianCalendarUtils.calculateNextOccurrence(
                            currentTriggerMillis = reminder.triggerAtMillis,
                            recurrenceType = recType,
                            interval = reminder.recurrenceInterval,
                            daysMask = reminder.recurrenceDaysMask,
                            recurrenceEndMillis = reminder.recurrenceEndMillis,
                            timeZone = TimeZone.getTimeZone(reminder.timezoneId)
                        )

                        if (nextMillis != null) {
                            val updated = reminder.copy(
                                triggerAtMillis = nextMillis,
                                status = ReminderStatus.ACTIVE.name,
                                updatedAtMillis = System.currentTimeMillis()
                            )
                            dao.update(updated)
                            val settings = SettingsDataStore(context).settingsFlow.first()
                            ReminderAlarmManager.scheduleReminder(
                                context,
                                updated,
                                settings.defaultLeadMinutes
                            )
                        } else {
                            dao.update(
                                reminder.copy(
                                    status = ReminderStatus.COMPLETED.name,
                                    updatedAtMillis = System.currentTimeMillis()
                                )
                            )
                            ReminderAlarmManager.cancelReminder(context, reminderId)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
