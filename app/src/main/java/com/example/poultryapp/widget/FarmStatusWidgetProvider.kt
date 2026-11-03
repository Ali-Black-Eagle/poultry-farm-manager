package com.example.poultryapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.poultryapp.MainActivity
import com.example.poultryapp.R
import com.example.poultryapp.data.local.AppDatabaseHelper
import com.example.poultryapp.engine.toPersianDigits
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FarmStatusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        updateAllWidgets(context, appWidgetManager, appWidgetIds, pendingResult)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_STATUS_WIDGET || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, FarmStatusWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds.isNotEmpty()) {
                val pendingResult = goAsync()
                updateAllWidgets(context, appWidgetManager, appWidgetIds, pendingResult)
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_STATUS_WIDGET = "com.example.poultryapp.ACTION_UPDATE_STATUS_WIDGET"

        fun sendUpdateBroadcast(context: Context) {
            val intent = Intent(context, FarmStatusWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_STATUS_WIDGET
            }
            context.sendBroadcast(intent)
        }

        private fun updateAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            pendingResult: BroadcastReceiver.PendingResult? = null
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dbHelper = AppDatabaseHelper.getInstance(context)
                    val flocks = dbHelper.getAllFlocks()
                    val totalBirds = flocks.sumOf { it.effectiveBirdCount }
                    val todayJalali = com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()
                    val eggRecords = dbHelper.getAllEggProductions()
                    val todayEggs = eggRecords.filter { it.dateJalaliStr == todayJalali }.sumOf { it.totalCount }
                    val manualTemp = dbHelper.getSetting("manual_temp_celsius", "")
                    val cachedTemp = dbHelper.getSetting("cached_temp_celsius", "")
                    val tempStr = if (manualTemp.isNotBlank()) {
                        "${toPersianDigits(manualTemp)}°C"
                    } else if (cachedTemp.isNotBlank()) {
                        "${toPersianDigits(cachedTemp)}°C"
                    } else {
                        "۲۴°C"
                    }

                    for (appWidgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_farm_status)

                        views.setTextViewText(R.id.widget_stat_birds, "${toPersianDigits(totalBirds)} قطعه")
                        views.setTextViewText(R.id.widget_stat_eggs, "${toPersianDigits(todayEggs)} دانه")
                        views.setTextViewText(R.id.widget_stat_temp, tempStr)
                        views.setTextViewText(R.id.widget_stat_footer, "گله‌های فعال: ${toPersianDigits(flocks.size)} سالن — لمس برای ورود")

                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("TARGET_TAB", "HOME")
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            appWidgetId,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }
}
