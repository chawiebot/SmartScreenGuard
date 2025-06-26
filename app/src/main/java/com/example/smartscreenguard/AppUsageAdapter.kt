package com.example.smartscreenguard

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class AppUsageAdapter(
    private val context: Context,
    usageStatsList: List<AppUsageData>
) : RecyclerView.Adapter<AppUsageAdapter.ViewHolder>() {

    private val iconCache = mutableMapOf<String, Drawable>()
    private var appUsageList: List<AppUsageData> = filterAndSort(usageStatsList)

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_usage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(appUsageList[position], context, iconCache)
    }

    override fun getItemCount(): Int = appUsageList.size

    override fun getItemId(position: Int): Long {
        return appUsageList[position].packageName.hashCode().toLong()
    }

    fun updateData(newList: List<AppUsageData>) {
        appUsageList = filterAndSort(newList)
        notifyDataSetChanged()
    }

    private fun filterAndSort(data: List<AppUsageData>): List<AppUsageData> {
        return data.filter {
            it.usageTime >= 60_000L && // At least 1 minute
                    it.appName.isNotBlank() &&
                    it.appName.lowercase() != "n/a" &&
                    it.appName.lowercase() != "unknown"
        }.sortedByDescending { it.usageTime }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appName: TextView = itemView.findViewById(R.id.appName)
        private val usageTime: TextView = itemView.findViewById(R.id.usageTime)
        private val icon: ImageView = itemView.findViewById(R.id.appIcon)

        fun bind(appUsage: AppUsageData, context: Context, iconCache: MutableMap<String, Drawable>) {
            appName.text = appUsage.appName
            usageTime.text = formatTime(appUsage.usageTime)
            icon.setImageDrawable(getAppIcon(appUsage.packageName, context, iconCache))
        }

        private fun formatTime(timeInMillis: Long): String {
            val hours = timeInMillis / (1000 * 60 * 60)
            val minutes = (timeInMillis / (1000 * 60)) % 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "<1m"
            }
        }

        private fun getAppIcon(
            packageName: String,
            context: Context,
            cache: MutableMap<String, Drawable>
        ): Drawable {
            return cache.getOrPut(packageName) {
                try {
                    context.packageManager.getApplicationIcon(packageName)
                } catch (e: PackageManager.NameNotFoundException) {
                    ContextCompat.getDrawable(context, R.drawable.ic_default_app_icon)
                        ?: context.getDrawable(android.R.drawable.sym_def_app_icon)
                        ?: throw RuntimeException("Missing default icon")
                }
            }
        }
    }

    data class AppUsageData(
        val appName: String,
        val usageTime: Long,  // Time in milliseconds
        val packageName: String
    )
}
