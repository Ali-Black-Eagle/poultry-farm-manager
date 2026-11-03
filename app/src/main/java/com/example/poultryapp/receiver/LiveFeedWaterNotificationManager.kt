package com.example.poultryapp.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.poultryapp.MainActivity
import com.example.poultryapp.R
import com.example.poultryapp.data.local.AppDatabaseHelper
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.engine.ChargeItemType
import com.example.poultryapp.engine.DailyNutritionEngine
import com.example.poultryapp.engine.LiveConsumptionTracker
import com.example.poultryapp.engine.toPersianDigits

object LiveFeedWaterNotificationManager {

    const val CHANNEL_ID = "poultry_feed_water_live_channel_v1"
    const val NOTIFICATION_ID_BASE = 60000
    const val REQUEST_CODE_PERIODIC_UPDATE = 88888

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "هشدارهای زنده دان و آب",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "نمایش زنده میزان باقیمانده دان و آب و هشدارهای کمبود در نوار نوتیفیکیشن"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getNotificationId(flockId: Long): Int = (NOTIFICATION_ID_BASE + flockId).toInt()

    /**
     * Updates live feed and water remaining amounts for all flocks in notification bar.
     * If any flock has low feed or water, an ongoing/live notification is maintained
     * and a repeating 60-second update alarm is scheduled.
     */
    fun updateAllLowNotifications(context: Context): Boolean {
        createNotificationChannel(context)
        val db = AppDatabaseHelper.getInstance(context)
        val flocks = db.getAllFlocks()
        var anyLow = false

        for (flock in flocks) {
            val isFlockLow = updateNotificationForFlock(context, flock, db)
            if (isFlockLow) {
                anyLow = true
            }
        }

        if (anyLow) {
            scheduleNextLiveUpdate(context)
        } else {
            cancelNextLiveUpdate(context)
        }

        // Check warehouse feed stocks as well
        checkAndNotifyWarehouseLowStock(context)

        return anyLow
    }

    /**
     * Calculates live remaining feed and water for a single flock and updates the notification.
     * Returns true if either feed or water is currently low or depleted.
     */
    fun updateNotificationForFlock(
        context: Context,
        flock: Flock,
        db: AppDatabaseHelper = AppDatabaseHelper.getInstance(context)
    ): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = getNotificationId(flock.id)

        val latestFeedCharge = db.getLatestChargeForFlock(flock.id, ChargeItemType.FEED)
        val latestWaterCharge = db.getLatestChargeForFlock(flock.id, ChargeItemType.WATER)

        // Check if tracking is disabled for both
        if ((!flock.isFeedEnabled && !flock.isWaterEnabled) || (latestFeedCharge == null && latestWaterCharge == null)) {
            notificationManager.cancel(notifId)
            return false
        }

        val lowThreshold = db.getSetting("low_threshold", "15.0").toDoubleOrNull() ?: 15.0
        val customBreeds = db.getAllCustomBreeds()
        val currentSeason = DailyNutritionEngine.getCurrentSeason()
        val dailyReq = DailyNutritionEngine.calculateFlockRequirement(flock, season = currentSeason, customBreeds = customBreeds)

        val now = System.currentTimeMillis()

        // Calculate live status for feed
        val feedStatus = LiveConsumptionTracker.calculateLiveStatus(
            itemType = ChargeItemType.FEED,
            chargedAmount = if (flock.isFeedEnabled) latestFeedCharge?.chargedAmount ?: 0.0 else 0.0,
            chargeTimestampMillis = if (flock.isFeedEnabled) latestFeedCharge?.chargeTimestampMillis ?: 0L else 0L,
            dailyRequirementTotal = dailyReq.totalFeedGrams,
            lowThresholdPercent = lowThreshold,
            currentTimestampMillis = now
        )

        // Calculate live status for water
        val waterStatus = LiveConsumptionTracker.calculateLiveStatus(
            itemType = ChargeItemType.WATER,
            chargedAmount = if (flock.isWaterEnabled) latestWaterCharge?.chargedAmount ?: 0.0 else 0.0,
            chargeTimestampMillis = if (flock.isWaterEnabled) latestWaterCharge?.chargeTimestampMillis ?: 0L else 0L,
            dailyRequirementTotal = dailyReq.totalWaterMl,
            lowThresholdPercent = lowThreshold,
            currentTimestampMillis = now
        )

        val hasFeedCharge = flock.isFeedEnabled && latestFeedCharge != null && latestFeedCharge.chargedAmount > 0
        val hasWaterCharge = flock.isWaterEnabled && latestWaterCharge != null && latestWaterCharge.chargedAmount > 0

        // Dismissal persistence check:
        // When user dismissed, we recorded the latest charge timestamp.
        // Alert stays muted until a new charge is made and later drops low again.
        val dismissedFeedTime = db.getSetting("dismissed_feed_${flock.id}", "0").toLongOrNull() ?: 0L
        val dismissedWaterTime = db.getSetting("dismissed_water_${flock.id}", "0").toLongOrNull() ?: 0L

        val isFeedDismissed = (latestFeedCharge?.chargeTimestampMillis ?: 0L) <= dismissedFeedTime
        val isWaterDismissed = (latestWaterCharge?.chargeTimestampMillis ?: 0L) <= dismissedWaterTime

        val isFeedLow = hasFeedCharge && !isFeedDismissed && (feedStatus.isLowWarning || feedStatus.isFinished)
        val isWaterLow = hasWaterCharge && !isWaterDismissed && (waterStatus.isLowWarning || waterStatus.isFinished)

        val isLow = isFeedLow || isWaterLow

        if (!isLow) {
            // Neither is low (or user dismissed until next charge) -> dismiss notification
            notificationManager.cancel(notifId)
            return false
        }

        // Title according to status
        val title = when {
            feedStatus.isFinished && waterStatus.isFinished -> "🚨 اتمام کامل دان و آب — گله ${flock.name}"
            feedStatus.isFinished && flock.isFeedEnabled -> "🚨 اتمام دان — گله ${flock.name}"
            waterStatus.isFinished && flock.isWaterEnabled -> "🚨 اتمام آب — گله ${flock.name}"
            isFeedLow && isWaterLow -> "⚠️ هشدار کمبود همزمان دان و آب — گله ${flock.name}"
            isFeedLow -> "⚠️ هشدار کمبود دان — گله ${flock.name}"
            else -> "⚠️ هشدار کمبود آب — گله ${flock.name}"
        }

        // Single line live remaining text for the status bar
        val feedPercent = (feedStatus.progress * 100).toInt()
        val waterPercent = (waterStatus.progress * 100).toInt()

        val contentText = buildString {
            if (hasFeedCharge) {
                append("🌾 دان مانده: ${feedStatus.remainingAmountFa} (${toPersianDigits(feedPercent)}٪)")
            }
            if (hasFeedCharge && hasWaterCharge) {
                append(" | ")
            }
            if (hasWaterCharge) {
                append("💧 آب مانده: ${waterStatus.remainingAmountFa} (${toPersianDigits(waterPercent)}٪)")
            }
        }

        // Expanded multi-line BigTextStyle
        val bigText = buildString {
            appendLine("گله ${flock.name} (${flock.breed} — ${toPersianDigits(flock.effectiveBirdCount)} پرنده)")
            if (hasFeedCharge) {
                appendLine("🌾 دان باقیمانده: ${feedStatus.remainingAmountFa} (${toPersianDigits(feedPercent)}٪) — ${feedStatus.statusTextFa}")
            }
            if (hasWaterCharge) {
                appendLine("💧 آب باقیمانده: ${waterStatus.remainingAmountFa} (${toPersianDigits(waterPercent)}٪) — ${waterStatus.statusTextFa}")
            }
            append("⚠️ اقدام: لطفاً شارژ مجدد را انجام دهید تا گله دچار افت راندمان یا تشنگی نشود.")
        }

        // Open App Intent
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("TARGET_TAB", "FEED_WATER")
            putExtra("SELECTED_FLOCK_ID", flock.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss Intent
        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_DISMISS_LIVE_NOTIFICATION
            putExtra(AlarmScheduler.EXTRA_FLOCK_ID, flock.id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 1000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val lowestProgress = listOfNotNull(
            if (hasFeedCharge) feedPercent else null,
            if (hasWaterCharge) waterPercent else null
        ).minOrNull() ?: 0

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSubText("گله ${flock.name}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setProgress(100, lowestProgress, false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSound(soundUri)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "ثبت شارژ مجدد", contentPendingIntent)
            .addAction(0, "بستن اعلان", dismissPendingIntent)

        notificationManager.notify(notifId, builder.build())
        return true
    }

    /**
     * Called when the user taps 'بستن اعلان' on a low feed/water notification.
     * Mutes this flock's low notification until a new charge is logged and drops low again.
     */
    fun dismissLowNotificationForFlock(context: Context, flockId: Long) {
        val db = AppDatabaseHelper.getInstance(context)
        val latestFeed = db.getLatestChargeForFlock(flockId, ChargeItemType.FEED)
        val latestWater = db.getLatestChargeForFlock(flockId, ChargeItemType.WATER)
        val now = System.currentTimeMillis()

        db.saveSetting("dismissed_feed_${flockId}", (latestFeed?.chargeTimestampMillis ?: now).toString())
        db.saveSetting("dismissed_water_${flockId}", (latestWater?.chargeTimestampMillis ?: now).toString())
        cancelNotification(context, flockId)
    }

    fun cancelNotification(context: Context, flockId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(getNotificationId(flockId))
    }

    /**
     * Checks warehouse feed stocks and notifies if stock will deplete in <= 3 days.
     */
    fun checkAndNotifyWarehouseLowStock(context: Context) {
        val db = AppDatabaseHelper.getInstance(context)
        val warehouseItems = db.getAllWarehouseItems()
        val allFlocks = db.getAllFlocks()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        for (item in warehouseItems) {
            if (item.category != com.example.poultryapp.data.local.WarehouseCategory.FEED) continue

            val assignedFlocks = allFlocks.filter { it.id in item.assignedFlockIds }
            val customBreeds = db.getAllCustomBreeds()
            val currentSeason = DailyNutritionEngine.getCurrentSeason()
            val dailyKg = assignedFlocks.sumOf { DailyNutritionEngine.calculateFlockRequirement(it, season = currentSeason, customBreeds = customBreeds).totalFeedGrams / 1000.0 }

            val daysRemaining = if (dailyKg > 0) item.currentStock / dailyKg else Double.MAX_VALUE
            val isLow = daysRemaining <= 3.0 || (item.minAlertThreshold > 0.0 && item.currentStock <= item.minAlertThreshold)

            val notifId = 70000 + item.id.toInt()
            if (isLow) {
                val title = "⚠️ هشدار کمبود دان در انبار: ${item.name}"
                val daysStr = if (daysRemaining < Double.MAX_VALUE) "${toPersianDigits(String.format(java.util.Locale.US, "%.1f", daysRemaining))} روز" else "نامشخص"
                val text = "موجودی ${item.name} (${toPersianDigits(item.currentStock.toInt())} ${item.unit}) برای حدود $daysStr کافی است. لطفاً اقدام به شارژ انبار نمایید."

                val contentIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("TARGET_TAB", "WAREHOUSE")
                }
                val contentPendingIntent = PendingIntent.getActivity(
                    context,
                    notifId,
                    contentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(contentPendingIntent)

                notificationManager.notify(notifId, builder.build())
            } else {
                notificationManager.cancel(notifId)
            }
        }
    }

    private fun scheduleNextLiveUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_UPDATE_LIVE_NOTIFICATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_PERIODIC_UPDATE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Refresh live remaining values in notification bar periodically (every 5 minutes to be battery-friendly and Doze-compliant)
        val triggerAt = System.currentTimeMillis() + 5 * 60_000L
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun cancelNextLiveUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_UPDATE_LIVE_NOTIFICATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_PERIODIC_UPDATE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    /**
     * Schedules warning alarms for both low threshold (e.g. 15%) and depleted (0%)
     */
    fun scheduleChargeLowAndEmptyWarnings(
        context: Context,
        flockId: Long,
        itemType: ChargeItemType,
        chargedAmount: Double
    ) {
        val db = AppDatabaseHelper.getInstance(context)
        val flock = db.getAllFlocks().find { it.id == flockId } ?: return

        // Do not schedule if tracking disabled for this item type
        if (itemType == ChargeItemType.FEED && !flock.isFeedEnabled) return
        if (itemType == ChargeItemType.WATER && !flock.isWaterEnabled) return

        val lowThreshold = db.getSetting("low_threshold", "15.0").toDoubleOrNull() ?: 15.0
        val customBreeds = db.getAllCustomBreeds()
        val dailyReq = DailyNutritionEngine.calculateFlockRequirement(flock, customBreeds = customBreeds)

        val totalDaily = if (itemType == ChargeItemType.FEED) dailyReq.totalFeedGrams else dailyReq.totalWaterMl
        val ratePerMilli = totalDaily / (24L * 60 * 60 * 1000)

        if (ratePerMilli <= 0) return

        val totalDurationMillis = (chargedAmount / ratePerMilli).toLong()
        val now = System.currentTimeMillis()

        // 1. Low warning trigger time (e.g. at 15% remaining -> 85% elapsed)
        val lowElapsedRatio = (1.0 - (lowThreshold / 100.0)).coerceIn(0.0, 0.99)
        val lowTriggerTime = now + (totalDurationMillis * lowElapsedRatio).toLong()

        // 2. Empty trigger time (0% remaining)
        val emptyTriggerTime = now + totalDurationMillis

        // Schedule warning alarms
        AlarmScheduler.scheduleChargeWarningAlarm(
            context = context,
            flockId = flockId,
            itemType = itemType,
            flockName = flock.name,
            triggerTimeMillis = lowTriggerTime,
            isLowWarning = true
        )

        AlarmScheduler.scheduleChargeWarningAlarm(
            context = context,
            flockId = flockId,
            itemType = itemType,
            flockName = flock.name,
            triggerTimeMillis = emptyTriggerTime,
            isLowWarning = false
        )
    }
}
