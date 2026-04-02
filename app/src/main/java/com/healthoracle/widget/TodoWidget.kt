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
import androidx.glance.appwidget.cornerRadius
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

            val result = db.todoDao().getTodosForDateSync(today)
            Log.d("TodoWidget", "Loaded ${result.size} todos")
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
            .fillMaxSize()
            .background(ColorProvider(Color(0xFFFDFDFD)))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Tasks",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ColorProvider(Color(0xFF1A1C1E))
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            if (total > 0) {
                Text(
                    text = "$doneCount/$total",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(Color(0xFF43474E))
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        if (todos.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "All clear for today!",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(Color(0xFF74777F))
                    )
                )
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                todos.take(4).forEach { todo ->
                    TodoWidgetRow(todo)
                    Spacer(modifier = GlanceModifier.height(8.dp))
                }
                if (todos.size > 4) {
                    Text(
                        text = "+${todos.size - 4} more in app",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = ColorProvider(Color(0xFF0067FF)),
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = GlanceModifier.padding(start = 28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TodoWidgetRow(todo: TodoEntity) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(
                actionRunCallback<TodoWidgetToggleAction>(
                    actionParametersOf(
                        TodoWidgetToggleAction.KEY_TODO_ID to todo.id,
                        TodoWidgetToggleAction.KEY_IS_DONE to todo.isDone
                    )
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minimal outlined tick style for better readability on small widgets
        // Using nested Boxes as Glance does not support a direct border modifier in version 1.1.0
        Box(
            modifier = GlanceModifier
                .size(18.dp)
                .cornerRadius(9.dp)
                .background(
                    ColorProvider(if (todo.isDone) Color(0xFF00A85E) else Color(0xFFB9BCC2))
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(1.5.dp)
                    .cornerRadius(8.dp)
                    .background(ColorProvider(Color(0xFFFDFDFD))),
                contentAlignment = Alignment.Center
            ) {
                if (todo.isDone) {
                    Text(
                        text = "✓",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = ColorProvider(Color(0xFF00A85E)),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.width(10.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = todo.title,
                style = TextStyle(
                    fontSize = 13.sp,
                    color = ColorProvider(
                        if (todo.isDone) Color(0xFF909194) else Color(0xFF1A1C1E)
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
                    color = ColorProvider(Color(0xFF74777F))
                )
            )
        }
    }
}
