package com.example.smartscreenguard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private val appList: List<AppInfo>,
    private val blockedApps: Set<String>,
    private val onToggle: (String, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.app_icon)
        val name: TextView = view.findViewById(R.id.app_name)
        val blockSwitch: Switch = view.findViewById(R.id.block_switch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = appList[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.name
        holder.blockSwitch.setOnCheckedChangeListener(null) // Avoid unwanted triggers
        holder.blockSwitch.isChecked = blockedApps.contains(app.packageName)

        holder.blockSwitch.setOnCheckedChangeListener { _, isChecked ->
            onToggle(app.packageName, isChecked)
        }
    }

    override fun getItemCount(): Int = appList.size
}
