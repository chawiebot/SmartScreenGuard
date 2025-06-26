package com.example.smartscreenguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object FocusNotificationUtil {

    private const val CHANNEL_ID = "focus_timer_channel"
    private const val CHANNEL_NAME = "Focus Timer"
    private const val CHANNEL_DESC = "Notifications for screen lock countdown"
    private const val NOTIFICATION_ID = 1001

    fun showLockTimerNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel if needed (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val endTimeMillis = prefs.getLong("focus_timer_end_time", 0L)
        val remainingMinutes = ((endTimeMillis - System.currentTimeMillis()) / 60000).coerceAtLeast(0)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Make sure this drawable exists
            .setContentTitle("Focus Timer Active")
            .setContentText("Your device will lock in $remainingMinutes minute(s).")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true) // Prevent user from swiping it away
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelLockTimerNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
