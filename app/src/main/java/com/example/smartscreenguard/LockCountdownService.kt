package com.example.smartscreenguard

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import androidx.core.app.NotificationCompat
import android.app.admin.DevicePolicyManager
import android.content.ComponentName

class LockCountdownService : Service() {

    private val channelId = "lock_timer_channel"
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private var countdownTimer: CountDownTimer? = null

    override fun onCreate() {
        super.onCreate()
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, MyDeviceAdminReceiver::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val delayMillis = intent?.getLongExtra("delayMillis", 0L) ?: 0L
        if (delayMillis <= 0) stopSelf()

        createNotificationChannel()

        countdownTimer = object : CountDownTimer(delayMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60

                val notification = NotificationCompat.Builder(this@LockCountdownService, channelId)
                    .setContentTitle("Focus Mode Active")
                    .setContentText("Locking in %02d:%02d".format(minutes, seconds))
                    .setSmallIcon(R.drawable.ic_notification) // Ensure you have this icon
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .build()

                startForeground(1, notification)
            }

            override fun onFinish() {
                stopForeground(true)
                stopSelf()
                if (devicePolicyManager.isAdminActive(compName)) {
                    devicePolicyManager.lockNow()
                }
            }
        }.start()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        countdownTimer?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Lock Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Countdown for locking the screen"
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
