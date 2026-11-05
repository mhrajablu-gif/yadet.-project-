package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.SettingsDataStore
import com.example.data.local.YadetDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = YadetDatabase.getInstance(context)
                    val settings = SettingsDataStore(context).settingsFlow.first()
                    val activeFuture = db.reminderDao().getAllActiveFuture(System.currentTimeMillis())
                    activeFuture.forEach { reminder ->
                        ReminderAlarmManager.scheduleReminder(
                            context = context,
                            reminder = reminder,
                            leadMinutes = settings.defaultLeadMinutes
                        )
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
