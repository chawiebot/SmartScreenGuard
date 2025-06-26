package com.example.smartscreenguard

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.app.Service
import android.os.IBinder

class FocusLockService : Service() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName

    override fun onCreate() {
        super.onCreate()
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, MyDeviceAdminReceiver::class.java)
        startForegroundWithNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val durationMillis = intent?.getLongExtra("duration", 0L) ?: 0L
        if (durationMillis <= 0L) stopSelf()

        val endTime = System.currentTimeMillis() + durationMillis
        getSharedPreferences("AppSettings", MODE_PRIVATE).edit()
            .putBoolean("focus_timer_active", true)
            .putLong("focus_timer_end_time", endTime)
            .apply()

        Handler(Looper.getMainLooper()).postDelayed({
            if (devicePolicyManager.isAdminActive(compName)) {
                devicePolicyManager.lockNow()
            }
            stopSelf()
        }, durationMillis)

        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val channelId = "focus_service_channel"
        val channelName = "Focus Lock Service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Focus Timer Running")
            .setContentText("Device will lock after the timer ends.")
            .setSmallIcon(R.drawable.ic_notification)
            .build()

        startForeground(999, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
