package com.healthoracle.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object TodoWidgetUpdater {
    fun enqueue(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            refreshNow(context)
        }
    }

    suspend fun refreshNow(context: Context) {
        TodoWidget().updateAll(context)

        val manager = AppWidgetManager.getInstance(context)
        val receiver = ComponentName(context, TodoWidgetReceiver::class.java)
        val appWidgetIds = manager.getAppWidgetIds(receiver)
        if (appWidgetIds.isNotEmpty()) {
            context.sendBroadcast(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                    component = receiver
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
            )
        }
    }
}