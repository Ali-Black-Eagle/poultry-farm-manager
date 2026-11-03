package com.example.poultryapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.poultryapp.MainActivity
import com.example.poultryapp.R

class FarmQuickActionWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_farm_quick_action)

            // 1. Egg Recording Intent
            val eggIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("TARGET_TAB", "EGG_PRODUCTION")
            }
            val eggPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 1,
                eggIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_egg, eggPendingIntent)

            // 2. Feed Charging Intent
            val feedIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("TARGET_TAB", "FEED_WATER")
            }
            val feedPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 2,
                feedIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_feed, feedPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
