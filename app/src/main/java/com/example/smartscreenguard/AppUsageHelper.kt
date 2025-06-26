package com.example.smartscreenguard

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.*

class AppUsageHelper {

    /**
     * Get app usage statistics for a given time range.
     *
     * @param context The application context.
     * @param startTime The start time of the range in milliseconds.
     * @param endTime The end time of the range in milliseconds.
     * @return A list of UsageStats for the given time range.
     */
    fun getAppUsageStats(context: Context, startTime: Long, endTime: Long): List<UsageStats> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()
    }

    /**
     * Filter out usage stats for excluded apps.
     *
     * @param usageStatsList The list of UsageStats.
     * @param excludedApps The list of package names to exclude.
     * @return A filtered list of UsageStats.
     */
    fun filterUsageStats(
        usageStatsList: List<UsageStats>,
        excludedApps: List<String>
    ): List<UsageStats> {
        return usageStatsList.filter { it.packageName !in excludedApps }
    }
}
