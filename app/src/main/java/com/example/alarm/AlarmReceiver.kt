package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.util.PersianCalendarUtils

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "yadet_reminders_alarm_channel_v2"
        const val CHANNEL_NAME = "زنگ و اعلان یادآورهای یادت"

        @Volatile
        private var activeRingtone: Ringtone? = null

        fun stopAlarmSound() {
            runCatching {
                activeRingtone?.stop()
                activeRingtone = null
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderAlarmManager.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val title = intent.getStringExtra(ReminderAlarmManager.EXTRA_REMINDER_TITLE) ?: "یادآور"
        val desc = intent.getStringExtra(ReminderAlarmManager.EXTRA_REMINDER_DESC) ?: ""
        val timeMillis = intent.getLongExtra(
            ReminderAlarmManager.EXTRA_REMINDER_TIME,
            System.currentTimeMillis()
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اعلان‌ها و زنگ هشدار زمان‌بندی‌شده یادت"
                enableVibration(true)
                setSound(alarmUri, audioAttrs)
            }
            notificationManager.createNotificationChannel(channel)
        }

        playAlarmSound(context, alarmUri)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderAlarmManager.EXTRA_REMINDER_ID, reminderId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedTime = PersianCalendarUtils.formatDateTime(
            millis = timeMillis,
            isJalali = true,
            is24Hour = true
        )
        val contentText = if (desc.isNotBlank()) "$desc\n$formattedTime" else formattedTime

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                0,
                "انجام شد",
                createActionPendingIntent(
                    context,
                    reminderId,
                    NotificationActionReceiver.ACTION_COMPLETE,
                    title,
                    desc,
                    100
                )
            )
            .addAction(
                0,
                "تعویق ۵ دقیقه",
                createActionPendingIntent(
                    context,
                    reminderId,
                    NotificationActionReceiver.ACTION_SNOOZE_5,
                    title,
                    desc,
                    105
                )
            )
            .addAction(
                0,
                "تعویق ۱۰ دقیقه",
                createActionPendingIntent(
                    context,
                    reminderId,
                    NotificationActionReceiver.ACTION_SNOOZE_10,
                    title,
                    desc,
                    110
                )
            )
            .addAction(
                0,
                "تعویق ۱۵ دقیقه",
                createActionPendingIntent(
                    context,
                    reminderId,
                    NotificationActionReceiver.ACTION_SNOOZE_15,
                    title,
                    desc,
                    115
                )
            )
            .addAction(
                0,
                "تعویق ۳۰ دقیقه",
                createActionPendingIntent(
                    context,
                    reminderId,
                    NotificationActionReceiver.ACTION_SNOOZE_30,
                    title,
                    desc,
                    130
                )
            )
            .addAction(
                0,
                "تعویق یک ساعت",
                createActionPendingIntent(
                    context,
                    reminderId,
                    NotificationActionReceiver.ACTION_SNOOZE_60,
                    title,
                    desc,
                    160
                )
            )

        notificationManager.notify(reminderId.toInt(), builder.build())
    }

    private fun createActionPendingIntent(
        context: Context,
        reminderId: Long,
        action: String,
        title: String,
        desc: String,
        requestOffset: Int
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderAlarmManager.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderAlarmManager.EXTRA_REMINDER_TITLE, title)
            putExtra(ReminderAlarmManager.EXTRA_REMINDER_DESC, desc)
        }
        return PendingIntent.getBroadcast(
            context,
            (reminderId.toInt() * 1000) + requestOffset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun playAlarmSound(context: Context, alarmUri: android.net.Uri?) {
        if (alarmUri == null) return
        runCatching {
            stopAlarmSound()
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .build()
                audioManager.requestAudioFocus(focusReq)
            }
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, alarmUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone?.isLooping = false
            }
            activeRingtone = ringtone
            ringtone?.play()
        }
    }
}
