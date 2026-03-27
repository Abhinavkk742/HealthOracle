package com.healthoracle.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object TodoWidgetUpdater {
    fun enqueue(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            TodoWidget().updateAll(context)
        }
    }
}