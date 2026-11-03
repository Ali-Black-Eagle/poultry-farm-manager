package com.example.poultryapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.poultryapp.data.local.AppDatabaseHelper
import com.example.poultryapp.receiver.AlarmScheduler
import com.example.poultryapp.receiver.LiveFeedWaterNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PoultryApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupGlobalCrashHandler()
        createNotificationChannels()
        LiveFeedWaterNotificationManager.createNotificationChannel(this)
        initDefaultAlarms()
    }

    private fun setupGlobalCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val logFile = java.io.File(filesDir, "crash_log.txt")
                val stackTrace = android.util.Log.getStackTraceString(throwable)
                val crashReport = buildString {
                    appendLine("=== POULTRY APP CRASH REPORT ===")
                    appendLine("Time: ${java.util.Date()}")
                    appendLine("Thread: ${thread.name} (id: ${thread.id})")
                    appendLine("Exception: ${throwable::class.java.name}: ${throwable.message}")
                    appendLine("Stacktrace:")
                    appendLine(stackTrace)
                }
                logFile.writeText(crashReport)
                android.util.Log.e("PoultryApplication", "CRASH CAUGHT:\n$crashReport")
            } catch (_: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                "poultry_alarm_channel_v1",
                "هشدارهای مدیریت طیور",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "آلارم‌ها و هشدارهای دقیق خوراک، آب و تقویم گله"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun initDefaultAlarms() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabaseHelper.getInstance(this@PoultryApplication)
                db.seedDatabaseFromAssetsIfEmpty(this@PoultryApplication)
                val morningAlertEnabled = db.getSetting("alert_morning", "true").toBoolean()
                if (morningAlertEnabled) {
                    val h = db.getSetting("morning_hour", "7").toIntOrNull() ?: 7
                    val m = db.getSetting("morning_minute", "0").toIntOrNull() ?: 0
                    AlarmScheduler.scheduleMorningReminder(this@PoultryApplication, h, m)
                }

                // Check and update live low feed/water notifications
                LiveFeedWaterNotificationManager.updateAllLowNotifications(this@PoultryApplication)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
