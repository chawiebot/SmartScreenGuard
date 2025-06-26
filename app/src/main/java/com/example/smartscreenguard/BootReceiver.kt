package com.example.smartscreenguard

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.os.Build
import java.util.Calendar
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted - restoring focus timer and scheduling reset.")

            restoreFocusTimer(context)
            scheduleDailyReset(context)
        }
    }

    private fun restoreFocusTimer(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val endTime = prefs.getLong("focus_timer_end_time", 0L)
        val isActive = prefs.getBoolean("focus_timer_active", false)
        val currentTime = System.currentTimeMillis()
        val remainingTime = endTime - currentTime

        if (isActive && remainingTime > 0) {
            val serviceIntent = Intent(context, FocusLockService::class.java).apply {
                putExtra("duration", remainingTime)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.d("BootReceiver", "FocusLockService restarted with remaining time: $remainingTime ms")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to restart FocusLockService: ${e.message}", e)
            }
        } else {
            Log.d("BootReceiver", "No active focus timer to restore.")
        }
    }

    private fun scheduleDailyReset(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val resetIntent = Intent(context, ResetDataReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            resetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, 1)
        }

        val triggerAt = calendar.timeInMillis

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }

        Log.d("BootReceiver", "Daily reset alarm scheduled for: ${calendar.time}")
    }
}
