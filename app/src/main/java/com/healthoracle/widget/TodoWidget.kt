package com.healthoracle.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import androidx.room.Room
import com.healthoracle.HealthOracleApp
import com.healthoracle.data.local.AppDatabase
import com.healthoracle.data.local.entity.TodoEntity
import java.time.LocalDate

class TodoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now().toString()
        Log.d("TodoWidget", "provideGlance: $today")

        val todos = try {
            val db = HealthOracleApp.getDatabase()
                ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "healthoracle_db"
                )
                    .addMigrations(AppDatabase.MIGRATION_2_3)
                    .build()
                    .also { Log.w("TodoWidget", "Using fallback DB") }

            val result = db.todoDao().getTodosForDateSync(today)
            Log.d("TodoWidget", "Loaded ${result.size} todos, done=${result.count { it.isDone }}")
            result
        } catch (e: Exception) {
            Log.e("TodoWidget", "Error loading todos", e)
            emptyList()
        }

        provideContent {
            WidgetContent(todos)
        }
    }
}

@Composable
private fun WidgetContent(todos: List<TodoEntity>) {
    val doneCount = todos.count { it.isDone }
    val total = todos.size

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(Color(0xFFF7F6F2)))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            Text(
                text = "Today's Tasks",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ColorProvider(Color(0xFF28251D))
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            if (total > 0) {
                Text(
                    text = "$doneCount/$total",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(Color(0xFF7A7974))
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (todos.isEmpty()) {
            Text(
                text = "No tasks for today",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = ColorProvider(Color(0xFF7A7974))
                ),
                modifier = GlanceModifier.padding(vertical = 8.dp)
            )
        } else {
            todos.take(5).forEach { todo ->
                TodoWidgetRow(todo)
                Spacer(modifier = GlanceModifier.height(6.dp))
            }
            if (todos.size > 5) {
                Text(
                    text = "+${todos.size - 5} more — open app",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(Color(0xFF7A7974))
                    )
                )
            }
        }
    }
}

@Composable
private fun TodoWidgetRow(todo: TodoEntity) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(
                actionRunCallback<TodoWidgetToggleAction>(
                    actionParametersOf(
                        TodoWidgetToggleAction.KEY_TODO_ID to todo.id,
                        TodoWidgetToggleAction.KEY_IS_DONE to todo.isDone
                    )
                )
            )
            .background(
                ColorProvider(
                    if (todo.isDone) Color(0x0D28251D) else Color(0xFFFFFFFF)
                )
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .size(18.dp)
                .background(
                    ColorProvider(
                        if (todo.isDone) Color(0xFF01696F) else Color(0xFFD4D1CA)
                    )
                )
        ) {
            if (todo.isDone) {
                Text(
                    text = "✓",
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = ColorProvider(Color.White),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = todo.title,
                style = TextStyle(
                    fontSize = 13.sp,
                    color = ColorProvider(
                        if (todo.isDone) Color(0x6628251D) else Color(0xFF28251D)
                    ),
                    fontWeight = if (todo.isDone) FontWeight.Normal else FontWeight.Medium,
                    textDecoration = if (todo.isDone) TextDecoration.LineThrough else TextDecoration.None
                ),
                maxLines = 1
            )
            Text(
                text = todo.time,
                style = TextStyle(
                    fontSize = 11.sp,
                    color = ColorProvider(Color(0xFF7A7974))
                )
            )
        }
    }
}