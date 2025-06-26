package com.example.smartscreenguard

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment

class FocusFragment : Fragment() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName

    companion object {
        private const val OVERLAY_PERMISSION_REQ_CODE = 5678
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_focus, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        devicePolicyManager = requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(requireContext(), MyDeviceAdminReceiver::class.java)

        setupFocusModeCards(view)
        setupOverlayFeatureCards(view)
    }

    private fun setupFocusModeCards(view: View) {
        view.findViewById<CardView>(R.id.card_auto_lock).setOnClickListener {
            showDurationInputDialog()
        }

        setupPresetTimer(view.findViewById(R.id.card_study_mode), hours = 2)
        setupPresetTimer(view.findViewById(R.id.card_watch_mode), minutes = 40)
        setupPresetTimer(view.findViewById(R.id.card_playtime), hours = 2)
        setupPresetTimer(view.findViewById(R.id.card_good_sleep), minutes = 35)
    }

    private fun setupOverlayFeatureCards(view: View) {
        view.findViewById<CardView>(R.id.card_app_blocking).setOnClickListener {
            requestOverlayPermission()
        }

        view.findViewById<CardView>(R.id.card_block_keywords).setOnClickListener {
            startActivity(Intent(requireContext(), BlockedKeywordManagerActivity::class.java))
        }
    }

    private fun showDurationInputDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_duration_input, null)
        val inputTime = dialogView.findViewById<EditText>(R.id.input_time)

        AlertDialog.Builder(requireContext())
            .setTitle("Set Focus Timer (HHMM)")
            .setView(dialogView)
            .setPositiveButton("Start") { _, _ ->
                val input = inputTime.text.toString().padStart(4, '0').take(4)
                val hours = input.substring(0, 2).toIntOrNull() ?: 0
                val minutes = input.substring(2, 4).toIntOrNull() ?: 0
                val millis = (hours * 60 + minutes) * 60 * 1000L

                if (millis > 0) startFocusTimer(millis)
                else Toast.makeText(requireContext(), "Please enter a valid time.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupPresetTimer(cardView: CardView, hours: Int = 0, minutes: Int = 0) {
        val millis = (hours * 60 + minutes) * 60 * 1000L
        cardView.setOnClickListener { startFocusTimer(millis) }
    }

    private fun startFocusTimer(delayMillis: Long) {
        if (!devicePolicyManager.isAdminActive(compName)) {
            Toast.makeText(requireContext(), "Device Admin permission required", Toast.LENGTH_LONG).show()
            requestDeviceAdminPermission()
            return
        }

        val prefs = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val endTime = System.currentTimeMillis() + delayMillis
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", false)

        prefs.edit()
            .putBoolean("focus_timer_active", true)
            .putLong("focus_timer_end_time", endTime)
            .apply()

        FocusNotificationUtil.cancelLockTimerNotification(requireContext())
        if (notificationsEnabled) {
            FocusNotificationUtil.showLockTimerNotification(requireContext())
        }

        Toast.makeText(requireContext(), "Focus timer started.", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({
            if (devicePolicyManager.isAdminActive(compName)) {
                devicePolicyManager.lockNow()
            }
            prefs.edit().putBoolean("focus_timer_active", false).apply()
            FocusNotificationUtil.cancelLockTimerNotification(requireContext())
        }, delayMillis)
    }

    private fun requestDeviceAdminPermission() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "SmartScreenGuard needs this permission to auto-lock the screen after timer ends."
            )
        }
        startActivity(intent)
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(requireContext())) {
            Toast.makeText(requireContext(), "Overlay permission required", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${requireContext().packageName}")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        } else {
            launchBlockedAppActivity()
        }
    }

    private fun launchBlockedAppActivity() {
        startActivity(Intent(requireContext(), BlockedAppActivity::class.java))
    }

    private fun isBlockedKeywordPresent(inputText: String): Boolean {
        val prefs = requireContext().getSharedPreferences("BlockedKeywords", Context.MODE_PRIVATE)
        val blockedKeywords = prefs.getStringSet("keywords", emptySet()) ?: emptySet()
        return blockedKeywords.any { inputText.lowercase().contains(it) }
    }

    private fun simulateTextDetection(text: String) {
        if (isBlockedKeywordPresent(text)) {
            Toast.makeText(requireContext(), "Blocked keyword detected!", Toast.LENGTH_LONG).show()
            if (devicePolicyManager.isAdminActive(compName)) {
                devicePolicyManager.lockNow()
            }
        }
    }

    private fun getForegroundAppPackageName(): String? {
        val usageStatsManager =
            requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY,
            time - 10_000,
            time
        )
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Settings.canDrawOverlays(requireContext())) {
                Toast.makeText(requireContext(), "Overlay permission granted.", Toast.LENGTH_SHORT).show()
                launchBlockedAppActivity()
            } else {
                Toast.makeText(requireContext(), "Overlay permission is required to block apps.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
