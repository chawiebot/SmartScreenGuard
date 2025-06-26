package com.example.smartscreenguard

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ScreenTimeService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval: Long = 60000 // 1 minute
    private lateinit var updateRunnable: Runnable

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()

        // Create a notification to show that the service is running in the foreground
        val notification = NotificationCompat.Builder(this, "screen_time_channel")
            .setContentTitle("Screen Time Tracker")
            .setContentText("Tracking your screen time in the background")
            .setSmallIcon(R.drawable.ic_notification)
            .build()

        startForeground(1, notification) // Start the service in the foreground

        // Periodically update screen time data every minute
        updateRunnable = object : Runnable {
            override fun run() {
                updateScreenTimeData()
                handler.postDelayed(this, updateInterval) // Schedule the next update
            }
        }

        handler.post(updateRunnable) // Start the periodic updates
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // You can handle any intent data here if necessary
        return START_STICKY // Keep the service running until explicitly stopped
    }

    private fun updateScreenTimeData() {
        // This method will call the same method to update screen time data as you have in your fragment
        // If you need to access SharedPreferences, pass it from the fragment or store it in a singleton

        // For example:
        // val weeklyScreenTime = getStableWeeklyScreenTime()
        // val usageStatsByDay = getWeeklyAppUsageData(weeklyScreenTime, includeSystemApps = false)

        // Make sure the screen time update logic works here
        // Toast.makeText(this, "Updating screen time data", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable) // Stop the periodic updates when the service is destroyed
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Return null as this is a started service, not bound
        return null
    }
}
