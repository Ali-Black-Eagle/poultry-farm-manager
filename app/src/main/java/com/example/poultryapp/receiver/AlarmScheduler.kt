package com.example.poultryapp.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.poultryapp.data.local.CalendarSchedule
import com.example.poultryapp.engine.ChargeItemType
import com.example.poultryapp.engine.JalaliDateHelper
import java.util.Calendar

object AlarmScheduler {

    const val ACTION_ALARM_TRIGGER = "com.example.poultryapp.ACTION_ALARM_TRIGGER"
    const val ACTION_DISMISS = "com.example.poultryapp.ACTION_DISMISS"
    const val ACTION_SNOOZE = "com.example.poultryapp.ACTION_SNOOZE"
    const val ACTION_CHARGE_WARNING = "com.example.poultryapp.ACTION_CHARGE_WARNING"
    const val ACTION_UPDATE_LIVE_NOTIFICATION = "com.example.poultryapp.ACTION_UPDATE_LIVE_NOTIFICATION"
    const val ACTION_DISMISS_LIVE_NOTIFICATION = "com.example.poultryapp.ACTION_DISMISS_LIVE_NOTIFICATION"

    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_FLOCK_ID = "extra_flock_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_MESSAGE = "extra_message"
    const val EXTRA_TYPE = "extra_type"
    const val EXTRA_ITEM_TYPE = "extra_item_type"

    /**
     * بررسی دسترسی به زمان‌بندی هشدارهای دقیق (Android 12+)
     */
    fun canScheduleExact(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * متد امن و مقاوم زمان‌بندی هشدار همراه با فال‌بک خودکار در صورت نداشتن مجوز Exact Alarm
     */
    private fun scheduleInternal(
        alarmManager: AlarmManager,
        triggerTime: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    // فال‌بک امن برای جلوگیری از سوختن یا کرش هشدار در اندروید ۱۲ به بالا
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    /**
     * Schedules exact alarm for a calendar schedule (weekly or monthly).
     */
    fun scheduleWeeklyAlarm(context: Context, schedule: CalendarSchedule) {
        if (!schedule.isEnabled) {
            cancelAlarm(context, (schedule.id * 10).toInt())
            for (dayIndex in 0..6) {
                cancelAlarm(context, (schedule.id * 10 + dayIndex).toInt())
            }
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        if (schedule.scheduleMode == "MONTHLY") {
            val dom = schedule.dayOfMonth ?: return
            val requestCode = (schedule.id * 10).toInt()
            val triggerTime = computeNextMonthlyTriggerTime(dom, schedule.hour, schedule.minute)

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM_TRIGGER
                putExtra(EXTRA_ALARM_ID, requestCode)
                putExtra(EXTRA_TITLE, "${schedule.scheduleType}: ${schedule.title}")
                putExtra(EXTRA_MESSAGE, schedule.notes ?: (schedule.flockName ?: "مدیریت طیور"))
                putExtra(EXTRA_TYPE, "CALENDAR")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleInternal(alarmManager, triggerTime, pendingIntent)
            return
        }

        if (schedule.daysOfWeek.isEmpty()) {
            cancelAlarm(context, schedule.id.toInt())
            return
        }

        for (dayIndex in schedule.daysOfWeek) {
            val requestCode = (schedule.id * 10 + dayIndex).toInt()
            val triggerTime = computeNextTriggerTime(dayIndex, schedule.hour, schedule.minute)

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM_TRIGGER
                putExtra(EXTRA_ALARM_ID, requestCode)
                putExtra(EXTRA_TITLE, "${schedule.scheduleType}: ${schedule.title}")
                putExtra(EXTRA_MESSAGE, schedule.notes ?: (schedule.flockName ?: "مدیریت طیور"))
                putExtra(EXTRA_TYPE, "CALENDAR")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleInternal(alarmManager, triggerTime, pendingIntent)
        }
    }

    /**
     * Snooze alarm for 15 minutes.
     */
    fun snoozeAlarm(context: Context, alarmId: Int, title: String, message: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTime = System.currentTimeMillis() + (15 * 60 * 1000L) // 15 minutes

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_TITLE, "یادآوری مجدد: $title")
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_TYPE, "SNOOZE")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleInternal(alarmManager, triggerTime, pendingIntent)
    }

    /**
     * Schedules warning alarm for feed/water running low or empty.
     */
    fun scheduleChargeWarningAlarm(
        context: Context,
        flockId: Long,
        itemType: ChargeItemType,
        flockName: String,
        triggerTimeMillis: Long,
        isLowWarning: Boolean
    ) {
        if (triggerTimeMillis <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val typeOffset = if (itemType == ChargeItemType.FEED) 1 else 2
        val stateOffset = if (isLowWarning) 10 else 20
        val reqCode = (flockId * 100 + stateOffset + typeOffset).toInt()

        val title = if (isLowWarning) "هشدار کمبود ${itemType.titleFa} گله $flockName"
                    else "هشدار اتمام ${itemType.titleFa} گله $flockName"
        val message = if (isLowWarning) "میزان ${itemType.titleFa} گله $flockName به حد کمبود رسیده است. لطفاً شارژ مجدد انجام دهید."
                      else "میزان ${itemType.titleFa} گله $flockName طبق برآورد به اتمام رسیده است."

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_CHARGE_WARNING
            putExtra(EXTRA_ALARM_ID, reqCode)
            putExtra(EXTRA_FLOCK_ID, flockId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_TYPE, "CHARGE_WARNING")
            putExtra(EXTRA_ITEM_TYPE, itemType.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleInternal(alarmManager, triggerTimeMillis, pendingIntent)
    }

    /**
     * Schedules warning alarm for feed/water running empty (backwards compatibility).
     */
    fun scheduleChargeEmptyWarning(
        context: Context,
        flockId: Long,
        itemTypeTitleFa: String,
        flockName: String,
        triggerTimeMillis: Long
    ) {
        val itemType = if (itemTypeTitleFa == "دان") ChargeItemType.FEED else ChargeItemType.WATER
        scheduleChargeWarningAlarm(
            context = context,
            flockId = flockId,
            itemType = itemType,
            flockName = flockName,
            triggerTimeMillis = triggerTimeMillis,
            isLowWarning = false
        )
    }

    /**
     * Schedules morning charge reminder at specified hour and minute.
     */
    fun scheduleMorningReminder(context: Context, hour: Int = 7, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, 9999)
            putExtra(EXTRA_TITLE, "یادآور صبحگاهی مدیریت طیور")
            putExtra(EXTRA_MESSAGE, "زمان ثبت و شارژ دان و آب تازه صبحگاهی گله‌ها فرا رسیده است.")
            putExtra(EXTRA_TYPE, "MORNING_REMINDER")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleInternal(alarmManager, cal.timeInMillis, pendingIntent)
    }

    fun cancelAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun cancelScheduleAlarms(context: Context, scheduleId: Long) {
        cancelAlarm(context, scheduleId.toInt())
        cancelAlarm(context, (scheduleId * 10).toInt())
        for (dayIndex in 0..6) {
            cancelAlarm(context, (scheduleId * 10 + dayIndex).toInt())
        }
    }

    /**
     * Cancels scheduled feed/water low & empty warning alarms for a flock.
     */
    fun cancelChargeWarningAlarms(
        context: Context,
        flockId: Long,
        itemType: ChargeItemType? = null
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val typeOffsets = when (itemType) {
            ChargeItemType.FEED -> listOf(1)
            ChargeItemType.WATER -> listOf(2)
            null -> listOf(1, 2)
        }
        val stateOffsets = listOf(10, 20)
        for (typeOffset in typeOffsets) {
            for (stateOffset in stateOffsets) {
                val reqCode = (flockId * 100 + stateOffset + typeOffset).toInt()
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_CHARGE_WARNING
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reqCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager?.cancel(pendingIntent)
                }
                notificationManager?.cancel(reqCode)
            }
        }
    }

    /**
     * Computes the next epoch trigger time for a given Shamsi day of week:
     * 0: شنبه (Saturday), 1: یکشنبه (Sunday), 2: دوشنبه (Monday), 3: سه‌شنبه (Tuesday),
     * 4: چهارشنبه (Wednesday), 5: پنجشنبه (Thursday), 6: جمعه (Friday)
     */
    private fun computeNextTriggerTime(shamsiDayIndex: Int, hour: Int, minute: Int): Long {
        // Map Shamsi day index (0: Sat) to java.util.Calendar.DAY_OF_WEEK:
        // 0 (Sat) -> Calendar.SATURDAY (7)
        // 1 (Sun) -> Calendar.SUNDAY (1)
        // 2 (Mon) -> Calendar.MONDAY (2)
        // 3 (Tue) -> Calendar.TUESDAY (3)
        // 4 (Wed) -> Calendar.WEDNESDAY (4)
        // 5 (Thu) -> Calendar.THURSDAY (5)
        // 6 (Fri) -> Calendar.FRIDAY (6)
        val targetDayOfWeek = when (shamsiDayIndex) {
            0 -> Calendar.SATURDAY
            1 -> Calendar.SUNDAY
            2 -> Calendar.MONDAY
            3 -> Calendar.TUESDAY
            4 -> Calendar.WEDNESDAY
            5 -> Calendar.THURSDAY
            else -> Calendar.FRIDAY
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        while (cal.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek || cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return cal.timeInMillis
    }

    /**
     * Computes the next epoch trigger time for a specific day of the month (Shamsi):
     */
    private fun computeNextMonthlyTriggerTime(dayOfMonth: Int, hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        val currentJalali = JalaliDateHelper.getJalaliDate(now)
        var targetYear = currentJalali.year
        var targetMonth = currentJalali.month

        val maxDaysThisMonth = JalaliDateHelper.getDaysInMonth(targetYear, targetMonth)
        val clampedDay = dayOfMonth.coerceIn(1, maxDaysThisMonth)
        val thisMonthTrigger = JalaliDateHelper.jalaliToEpochMillis(targetYear, targetMonth, clampedDay) +
                (hour * 3600_000L) + (minute * 60_000L)

        if (thisMonthTrigger > now) {
            return thisMonthTrigger
        }

        targetMonth++
        if (targetMonth > 12) {
            targetMonth = 1
            targetYear++
        }
        val maxDaysNextMonth = JalaliDateHelper.getDaysInMonth(targetYear, targetMonth)
        val clampedDayNext = dayOfMonth.coerceIn(1, maxDaysNextMonth)
        return JalaliDateHelper.jalaliToEpochMillis(targetYear, targetMonth, clampedDayNext) +
                (hour * 3600_000L) + (minute * 60_000L)
    }
}
