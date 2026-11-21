package com.example.poultryapp.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.poultryapp.MainActivity
import com.example.poultryapp.R
import com.example.poultryapp.data.local.AppDatabaseHelper
import com.example.poultryapp.engine.ChargeItemType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, 1)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "یادآور مدیریت طیور"
        val message = intent.getStringExtra(AlarmScheduler.EXTRA_MESSAGE) ?: "زمان انجام برنامه گله فرا رسیده است."

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            AlarmScheduler.ACTION_DISMISS -> {
                notificationManager.cancel(alarmId)
            }

            AlarmScheduler.ACTION_SNOOZE -> {
                notificationManager.cancel(alarmId)
                AlarmScheduler.snoozeAlarm(context, alarmId, title, message)
            }

            AlarmScheduler.ACTION_ALARM_TRIGGER -> {
                val db = AppDatabaseHelper.getInstance(context)
                if (alarmId == 9999) {
                    val morningEnabled = db.getSetting("alert_morning", "true").toBoolean()
                    if (morningEnabled) {
                        showAlarmNotification(context, notificationManager, alarmId, title, message)
                        val h = db.getSetting("morning_hour", "7").toIntOrNull() ?: 7
                        val m = db.getSetting("morning_minute", "0").toIntOrNull() ?: 0
                        AlarmScheduler.scheduleMorningReminder(context, h, m)
                    }
                } else {
                    val calendarAlertsEnabled = db.getSetting("alert_calendar", "true").toBoolean()
                    if (calendarAlertsEnabled) {
                        showAlarmNotification(context, notificationManager, alarmId, title, message)
                    }
                    if (alarmId >= 10) {
                        val schedId = (alarmId / 10).toLong()
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val repo = com.example.poultryapp.data.repository.ScheduleRepository(context)
                                val sched = repo.getAllSchedules().find { it.id == schedId }
                                if (sched != null && sched.isEnabled && calendarAlertsEnabled) {
                                    AlarmScheduler.scheduleWeeklyAlarm(context, sched)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }

            AlarmScheduler.ACTION_CHARGE_WARNING -> {
                val db = AppDatabaseHelper.getInstance(context)
                val feedWaterAlertsEnabled = db.getSetting("alert_feed_water", "true").toBoolean()
                val flockId = intent.getLongExtra(AlarmScheduler.EXTRA_FLOCK_ID, -1L)
                val itemTypeName = intent.getStringExtra(AlarmScheduler.EXTRA_ITEM_TYPE)
                val itemType = when (itemTypeName) {
                    ChargeItemType.FEED.name -> ChargeItemType.FEED
                    ChargeItemType.WATER.name -> ChargeItemType.WATER
                    else -> if (alarmId % 10 == 1) ChargeItemType.FEED else ChargeItemType.WATER
                }
                val flock = if (flockId != -1L) db.getAllFlocks().find { it.id == flockId } else null
                val isTrackingEnabled = flock != null && if (itemType == ChargeItemType.FEED) flock.isFeedEnabled else flock.isWaterEnabled

                if (feedWaterAlertsEnabled && isTrackingEnabled) {
                    showAlarmNotification(context, notificationManager, alarmId, title, message)
                } else {
                    notificationManager.cancel(alarmId)
                }
                LiveFeedWaterNotificationManager.updateAllLowNotifications(context)
            }

            AlarmScheduler.ACTION_UPDATE_LIVE_NOTIFICATION -> {
                LiveFeedWaterNotificationManager.updateAllLowNotifications(context)
            }

            AlarmScheduler.ACTION_DISMISS_LIVE_NOTIFICATION -> {
                val flockId = intent.getLongExtra(AlarmScheduler.EXTRA_FLOCK_ID, -1L)
                if (flockId != -1L) {
                    LiveFeedWaterNotificationManager.dismissLowNotificationForFlock(context, flockId)
                }
            }
        }
    }

    private fun showAlarmNotification(
        context: Context,
        notificationManager: NotificationManager,
        alarmId: Int,
        title: String,
        message: String
    ) {
        val channelId = "poultry_alarm_channel_v1"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                channelId,
                "هشدارهای مدیریت طیور",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "آلارم‌ها و هشدارهای دقیق خوراک، آب و تقویم گله"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap intent -> Open MainActivity
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            alarmId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Done / Dismiss
        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_DISMISS
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId + 100000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Snooze 15 minutes
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_MESSAGE, message)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId + 200000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setContentIntent(contentPendingIntent)
            .addAction(0, "انجام شد", dismissPendingIntent)
            .addAction(0, "۱۵ دقیقه بعد یادآوری کن", snoozePendingIntent)
            .build()

        notificationManager.notify(alarmId, notification)
    }
}
