package com.example.smartscreenguard

import android.Manifest
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var sharedPreferences: SharedPreferences
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

    private lateinit var switchOverlay: Switch
    private lateinit var btnAccessibility: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences =
            requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        val switchNotifications = view.findViewById<Switch>(R.id.switch_notifications)
        switchOverlay = view.findViewById(R.id.switch_overlay)
        btnAccessibility = view.findViewById(R.id.btn_accessibility)
        val switchUsageAccess = view.findViewById<Switch>(R.id.switch_usage_access)
        val switchDeviceAdmin = view.findViewById<Switch>(R.id.switch_device_admin)

        // Restore states
        switchNotifications.isChecked = sharedPreferences.getBoolean("notifications_enabled", false)
        switchOverlay.isChecked = Settings.canDrawOverlays(requireContext())
        updateAccessibilityButtonText()
        switchUsageAccess.isChecked = hasUsageStatsPermission()
        switchDeviceAdmin.isChecked = isDeviceAdminActive()

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()
            if (isChecked) {
                requestNotificationPermission()
                if (sharedPreferences.getBoolean("focus_timer_active", false)) {
                    FocusNotificationUtil.showLockTimerNotification(requireContext())
                }
            } else {
                FocusNotificationUtil.cancelLockTimerNotification(requireContext())
            }
            showToast("Notifications ${if (isChecked) "Enabled" else "Disabled"}")
        }

        switchOverlay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !Settings.canDrawOverlays(requireContext())) {
                requestOverlayPermission()
            } else if (!isChecked && Settings.canDrawOverlays(requireContext())) {
                showToast("Please disable 'Display over other apps' in settings.")
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                }
                startActivity(intent)
            }
        }

        btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            showToast("Enable accessibility service for Smart Screen Guard.")
        }

        switchUsageAccess.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !hasUsageStatsPermission()) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                showToast("Grant usage access permission.")
            }
        }

        switchDeviceAdmin.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !isDeviceAdminActive()) {
                requestDeviceAdminPermission()
            } else if (!isChecked && isDeviceAdminActive()) {
                revokeDeviceAdmin()
            }
        }

        view.findViewById<View>(R.id.share_app).setOnClickListener { shareApp() }
        view.findViewById<View>(R.id.send_feedback).setOnClickListener { sendFeedback() }
    }

    override fun onResume() {
        super.onResume()
        // Refresh toggle states on resume
        switchOverlay.isChecked = Settings.canDrawOverlays(requireContext())
        updateAccessibilityButtonText()
    }

    private fun updateAccessibilityButtonText() {
        if (::btnAccessibility.isInitialized) {
            btnAccessibility.text = if (isAccessibilityServiceEnabled()) "Enabled" else "Enable"
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(requireContext())) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            }
            startActivity(intent)
        }
    }

    private fun requestDeviceAdminPermission() {
        val componentName = ComponentName(requireContext(), MyDeviceAdminReceiver::class.java)
        val dpm =
            requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        if (!dpm.isAdminActive(componentName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Smart Screen Guard requires admin rights to lock the device automatically."
                )
            }
            startActivity(intent)
        }
    }

    private fun revokeDeviceAdmin() {
        val componentName = ComponentName(requireContext(), MyDeviceAdminReceiver::class.java)
        val dpm =
            requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (dpm.isAdminActive(componentName)) {
            dpm.removeActiveAdmin(componentName)
            showToast("Device admin permission revoked")
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                "android:get_usage_stats",
                android.os.Process.myUid(),
                requireContext().packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected =
            "${requireContext().packageName}/com.example.smartscreenguard.MyAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.split(":")?.contains(expected) == true
    }

    private fun isDeviceAdminActive(): Boolean {
        val componentName = ComponentName(requireContext(), MyDeviceAdminReceiver::class.java)
        val dpm =
            requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(componentName)
    }

    private fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Check out Smart Screen Guard to improve your focus and screen time control!"
            )
        }
        startActivity(Intent.createChooser(intent, "Share via"))
    }

    private fun sendFeedback() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:laguardia.j.bsinfotech@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "Smart Screen Guard Feedback")
        }
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            val granted =
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            showToast(if (granted) "Notification permission granted" else "Notification permission denied")
        }
    }
}