package com.example.smartscreenguard

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.*

class AppBlockerAccessibilityService : AccessibilityService() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var usageStatsManager: UsageStatsManager

    private var blockedApps: Set<String> = emptySet()
    private var blockedKeywords: Set<String> = emptySet()
    private var lastBlockedPackage: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 2000L // every 2 seconds

    private val myPackageName = "com.example.smartscreenguard"

    override fun onServiceConnected() {
        super.onServiceConnected()

        sharedPreferences = getSharedPreferences("BlockedApps", MODE_PRIVATE)
        usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager

        blockedApps = sharedPreferences.getStringSet("blocked_packages", emptySet()) ?: emptySet()

        startForegroundAppMonitor()
    }

    override fun onInterrupt() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.source == null) return

        // ✅ Always fetch latest keywords
        val keywordPrefs = getSharedPreferences("BlockedKeywords", MODE_PRIVATE)
        blockedKeywords = keywordPrefs.getStringSet("keywords", emptySet()) ?: emptySet()

        if (blockedKeywords.isEmpty()) return

        val currentApp = getForegroundAppPackage() ?: return

        // ✅ Skip keyword checking for own app
        if (currentApp == myPackageName) return

        val rootNode = rootInActiveWindow ?: return
        scanNodeForKeywords(rootNode)
    }

    private fun scanNodeForKeywords(node: AccessibilityNodeInfo) {
        val text = node.text?.toString()?.lowercase(Locale.getDefault())
        if (!text.isNullOrEmpty()) {
            for (keyword in blockedKeywords) {
                if (text.contains(keyword)) {
                    if (Settings.canDrawOverlays(this)) {
                        launchOverlayBlockScreen()
                    }
                    return
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                scanNodeForKeywords(child)
                child.recycle()
            }
        }
    }

    private fun startForegroundAppMonitor() {
        handler.post(object : Runnable {
            override fun run() {
                val foregroundApp = getForegroundAppPackage()

                if (foregroundApp != null && foregroundApp != lastBlockedPackage) {
                    // ✅ Skip blocking if it's your own app
                    if (foregroundApp == myPackageName) {
                        lastBlockedPackage = null
                    } else {
                        blockedApps = sharedPreferences.getStringSet("blocked_packages", emptySet()) ?: emptySet()
                        if (blockedApps.contains(foregroundApp) && !isHomeScreen(foregroundApp)) {
                            lastBlockedPackage = foregroundApp
                            if (Settings.canDrawOverlays(this@AppBlockerAccessibilityService)) {
                                launchOverlayBlockScreen()
                            }
                        } else {
                            lastBlockedPackage = null
                        }
                    }
                }

                handler.postDelayed(this, checkInterval)
            }
        })
    }

    private fun getForegroundAppPackage(): String? {
        val end = System.currentTimeMillis()
        val begin = end - 5000
        val events = usageStatsManager.queryEvents(begin, end)

        var lastApp: String? = null
        val usageEvent = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(usageEvent)
            if (usageEvent.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastApp = usageEvent.packageName
            }
        }
        return lastApp
    }

    private fun isHomeScreen(packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, 0)
        val homePackage = resolveInfo?.activityInfo?.packageName
        return packageName == homePackage
    }

    private fun launchOverlayBlockScreen() {
        val intent = Intent(this, OverlayBlockService::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startService(intent)
    }
}
