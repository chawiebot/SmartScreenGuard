package com.example.smartscreenguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import java.util.*

class ResetDataReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("ResetDataReceiver", "⏰ Daily reset triggered at midnight.")

        try {
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences("ScreenTimeData", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            val weeklyDataRaw = sharedPreferences.getString("weeklyScreenTime", "") ?: ""
            val weeklyData = weeklyDataRaw.split(",").mapNotNull { it.toFloatOrNull() }.toMutableList()

            while (weeklyData.size < 7) weeklyData.add(0f) // Fill in missing days if needed

            val calendar = Calendar.getInstance()
            val todayIndex = if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 6
            else calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY

            // Reset only today's screen time
            weeklyData[todayIndex] = 0f

            // Save updated weekly data
            editor.putString("weeklyScreenTime", weeklyData.joinToString(","))

            // Optionally reset daily data
            editor.putLong("todayScreenTime", 0L)
            editor.remove("dailyAppUsage")

            // Store the last reset day
            editor.putInt("lastResetDay", calendar.get(Calendar.DAY_OF_YEAR))

            editor.apply()
            Log.i("ResetDataReceiver", "✅ Today's screen time reset. Index: $todayIndex")

            // Optional: Notify UI
            val refreshIntent = Intent("com.example.smartscreenguard.ACTION_DATA_RESET")
            context.sendBroadcast(refreshIntent)

        } catch (e: Exception) {
            Log.e("ResetDataReceiver", "❌ Exception during reset: ${e.message}", e)
        }
    }
}
