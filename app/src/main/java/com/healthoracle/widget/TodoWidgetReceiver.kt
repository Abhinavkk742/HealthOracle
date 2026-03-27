package com.healthoracle.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Widget added to home screen — trigger first load
        TodoWidgetUpdater.enqueue(context)
    }
}