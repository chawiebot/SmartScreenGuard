package com.example.smartscreenguard

import android.app.*
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var barChart: BarChart
    private lateinit var totalScreenTimeText: TextView

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var packageManager: PackageManager

    private val handler = Handler(Looper.getMainLooper())
    private val updateIntervalMillis = TimeUnit.MINUTES.toMillis(1)
    private var isUpdateRunning = false
    private lateinit var updateRunnable: Runnable

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()

        if (hasUsageStatsPermission()) {
            startPeriodicUpdates()
        } else {
            promptUsagePermission()
        }

        scheduleDailyReset()
    }

    override fun onResume() {
        super.onResume()
        if (!isUpdateRunning && hasUsageStatsPermission()) {
            startPeriodicUpdates()
        }
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        barChart = view.findViewById(R.id.barChart)
        totalScreenTimeText = view.findViewById(R.id.totalScreenTimeText)
        sharedPreferences = requireContext().getSharedPreferences("ScreenTimeData", Context.MODE_PRIVATE)
        packageManager = requireContext().packageManager
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)
    }

    private fun startPeriodicUpdates() {
        if (isUpdateRunning) return
        isUpdateRunning = true

        updateRunnable = object : Runnable {
            override fun run() {
                if (!hasUsageStatsPermission()) {
                    stopPeriodicUpdates()
                    promptUsagePermission()
                    return
                }
                updateScreenTimeData()
                handler.postDelayed(this, updateIntervalMillis)
            }
        }
        handler.post(updateRunnable)
    }

    private fun stopPeriodicUpdates() {
        if (::updateRunnable.isInitialized) {
            handler.removeCallbacks(updateRunnable)
        }
        isUpdateRunning = false
    }

    private fun updateScreenTimeData() {
        lifecycleScope.launch {
            val usageStatsByDay = withContext(Dispatchers.IO) {
                getFreshWeeklyAppUsage(includeSystemApps = false)
            }

            val todayIndex = calculateCurrentDayIndex()
            val todayStats = usageStatsByDay.getOrNull(todayIndex).orEmpty()

            val newWeeklyScreenTime = usageStatsByDay.map { day ->
                day.sumOf { it.usageTime } / (1000f * 60f * 60f)
            }

            saveScreenTimeForWeek(newWeeklyScreenTime)
            updateUI(newWeeklyScreenTime, todayStats, todayIndex)
        }
    }

    private fun updateUI(weeklyScreenTime: List<Float>, todayStats: List<AppUsageAdapter.AppUsageData>, todayIndex: Int) {
        if (recyclerView.adapter == null) {
            recyclerView.adapter = AppUsageAdapter(requireContext(), todayStats)
        } else {
            (recyclerView.adapter as AppUsageAdapter).updateData(todayStats)
        }

        setupBarChart(weeklyScreenTime)
        totalScreenTimeText.text = "Total Screen Time: ${formatTime(weeklyScreenTime.getOrElse(todayIndex) { 0f })}"
    }

    private fun getFreshWeeklyAppUsage(includeSystemApps: Boolean): List<List<AppUsageAdapter.AppUsageData>> {
        val usageStatsManager = requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val systemApps = getSystemApps()
        val weeklyData = mutableListOf<List<AppUsageAdapter.AppUsageData>>()

        repeat(7) {
            val startTime = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endTime = calendar.timeInMillis

            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            val seenPackages = mutableSetOf<String>()

            val dailyData = stats?.mapNotNull { stat ->
                if (seenPackages.add(stat.packageName)) {
                    createAppUsageData(stat, includeSystemApps, systemApps)
                } else null
            }?.sortedByDescending { it.usageTime } ?: emptyList()

            weeklyData.add(dailyData)
        }

        return weeklyData
    }

    private fun createAppUsageData(stat: UsageStats, includeSystemApps: Boolean, systemApps: Set<String>): AppUsageAdapter.AppUsageData? {
        val appName = getAppName(stat.packageName)
        return if (appName != null && stat.totalTimeInForeground > 0 &&
            (includeSystemApps || stat.packageName !in systemApps)
        ) {
            AppUsageAdapter.AppUsageData(appName, stat.totalTimeInForeground, stat.packageName)
        } else null
    }

    private fun getAppName(packageName: String): String? {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun getSystemApps(): Set<String> {
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0 }
            .map { it.packageName }
            .toSet()
    }

    private fun saveScreenTimeForWeek(weeklyScreenTime: List<Float>) {
        val data = weeklyScreenTime.joinToString(",")
        sharedPreferences.edit().putString("weeklyScreenTime", data).apply()
    }

    private fun getStableWeeklyScreenTime(): MutableList<Float> {
        val raw = sharedPreferences.getString("weeklyScreenTime", "") ?: return MutableList(7) { 0f }
        return raw.split(",").mapNotNull { it.toFloatOrNull() }.toMutableList().apply {
            while (size < 7) add(0f)
        }
    }

    private fun setupBarChart(weeklyScreenTime: List<Float>) {
        val entries = weeklyScreenTime.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }
        val dataSet = BarDataSet(entries, "Weekly Screen Time (hrs)").apply {
            color = requireContext().getColor(R.color.custom_amber)
            valueTextColor = requireContext().getColor(R.color.black)
            valueTextSize = 12f
        }

        barChart.run {
            clear()
            data = BarData(dataSet).apply { barWidth = 0.8f }
            description.isEnabled = false
            setFitBars(true)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelCount = 7
                valueFormatter = DayOfWeekAxisFormatter()
            }
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 24f
            axisRight.isEnabled = false
            invalidate()
        }
    }

    private fun scheduleDailyReset() {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), ResetDataReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(), 0, intent,
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
    }

    private fun promptUsagePermission() {
        Toast.makeText(requireContext(), "Please grant usage access permission", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            requireContext().packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    private fun calculateCurrentDayIndex(): Int {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return if (day == Calendar.SUNDAY) 6 else day - Calendar.MONDAY
    }

    private fun formatTime(hours: Float): String {
        val totalMinutes = (hours * 60).toInt()
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return if (h > 0) "$h hrs and $m mins" else "$m mins"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPeriodicUpdates()
        barChart.clear()
        recyclerView.adapter = null
    }

    class DayOfWeekAxisFormatter : ValueFormatter() {
        private val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        override fun getFormattedValue(value: Float): String {
            return days.getOrElse(value.toInt()) { "" }
        }
    }
}
