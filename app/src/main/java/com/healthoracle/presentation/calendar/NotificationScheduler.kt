package com.healthoracle.presentation.calendar

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class NotificationScheduler(private val context: Context) {

    fun schedule(appointmentId: Int, title: String, timeStr: String, date: LocalDate) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AppointmentReceiver::class.java).apply {
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_TIME", timeStr)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointmentId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmTimeMillis = try {
            val cleanStr = timeStr.trim().uppercase()
            val time = if (cleanStr.contains("AM") || cleanStr.contains("PM")) {
                LocalTime.parse(cleanStr, DateTimeFormatter.ofPattern("h:mm a"))
            } else {
                LocalTime.parse(cleanStr, DateTimeFormatter.ofPattern("H:mm"))
            }

            val localDateTime = LocalDateTime.of(date, time)
            localDateTime.minusMinutes(15).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000

        } catch (e: Exception) {
            val fallbackDateTime = LocalDateTime.of(date, LocalTime.of(9, 0))
            fallbackDateTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000
        }

        if (alarmTimeMillis > System.currentTimeMillis()) {
            try {
                // setExact ensures the notification fires exactly on time, not batched by the OS
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    alarmTimeMillis,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    fun cancel(appointmentId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AppointmentReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointmentId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // NEW: Function to test the notification immediately
    fun scheduleTestNotification() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AppointmentReceiver::class.java).apply {
            putExtra("EXTRA_TITLE", "Test Checkup")
            putExtra("EXTRA_TIME", "Right Now")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            99999, // Dummy ID for the test
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Schedule the alarm for exactly 5 seconds from the moment the button is pressed
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 5000,
                pendingIntent
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}