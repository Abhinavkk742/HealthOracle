package com.healthoracle.presentation.calendar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.healthoracle.MainActivity

class AppointmentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Health Appointment"
        val time = intent.getStringExtra("EXTRA_TIME") ?: ""

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "appointment_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Appointments",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled appointments"
                enableLights(true)
                lightColor = Color.BLUE
            }
            manager.createNotificationChannel(channel)
        }

        // Intent to open the app when the notification is tapped
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingLaunchIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Upcoming Appointment: $title")
            .setContentText("You have an appointment scheduled for $time.")
            .setColor(Color.parseColor("#006688")) // Sleek health-blue accent color
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingLaunchIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}