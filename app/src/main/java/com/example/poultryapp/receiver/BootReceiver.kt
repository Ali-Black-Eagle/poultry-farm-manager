package com.example.poultryapp.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.poultryapp.data.local.AppDatabaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val isExactAlarmPermissionChanged = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED

        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.MY_PACKAGE_REPLACED" ||
            isExactAlarmPermissionChanged
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabaseHelper.getInstance(context)
                    val schedules = db.getAllSchedules()
                    for (sched in schedules) {
                        if (sched.isEnabled) {
                            AlarmScheduler.scheduleWeeklyAlarm(context, sched)
                        }
                    }

                    // Schedule morning charge reminder
                    val morningAlertEnabled = db.getSetting("alert_morning", "true").toBoolean()
                    if (morningAlertEnabled) {
                        val h = db.getSetting("morning_hour", "7").toIntOrNull() ?: 7
                        val m = db.getSetting("morning_minute", "0").toIntOrNull() ?: 0
                        AlarmScheduler.scheduleMorningReminder(context, h, m)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
