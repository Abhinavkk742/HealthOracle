package com.healthoracle.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import androidx.room.Room
import com.healthoracle.HealthOracleApp
import com.healthoracle.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TodoWidgetToggleAction : ActionCallback {

    companion object {
        val KEY_TODO_ID = ActionParameters.Key<Int>("todo_id")
        val KEY_IS_DONE = ActionParameters.Key<Boolean>("is_done")
        private const val TAG = "TodoWidgetToggle"
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val todoId = parameters[KEY_TODO_ID]
        val isDone = parameters[KEY_IS_DONE]

        Log.d(TAG, "onAction: todoId=$todoId isDone=$isDone")

        if (todoId == null || isDone == null) {
            Log.e(TAG, "Missing parameters!")
            return
        }

        try {
            withContext(Dispatchers.IO) {
                val db = HealthOracleApp.getDatabase()

                if (db == null) {
                    Log.w(TAG, "App DB null — using fallback Room instance")
                    val fallbackDb = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "healthoracle_db"
                    )
                        .addMigrations(AppDatabase.MIGRATION_2_3)
                        .build()
                    fallbackDb.todoDao().setDone(todoId, !isDone)
                    fallbackDb.close()
                } else {
                    Log.d(TAG, "Using app DB singleton")
                    db.todoDao().setDone(todoId, !isDone)
                }

                Log.d(TAG, "setDone($todoId, ${!isDone}) complete")
            }

            // Use the specific glanceId to update the widget instance immediately
            Log.d(TAG, "Refreshing specific widget instance...")
            TodoWidget().update(context, glanceId)
            
            // Also update all others just in case
            TodoWidget().updateAll(context)
            Log.d(TAG, "Widget refreshed")

        } catch (e: Exception) {
            Log.e(TAG, "Error toggling todo", e)
        }
    }
}