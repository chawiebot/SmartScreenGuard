package com.example.smartscreenguard

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BlockedAppActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter
    private lateinit var blockedApps: MutableSet<String>

    companion object {
        private const val PREF_NAME = "BlockedApps"
        private const val BLOCKED_KEY = "blocked_packages"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_app)

        // Handle insets (status/navigation bar padding)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Load saved blocked apps
        blockedApps = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            .getStringSet(BLOCKED_KEY, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        val pm = packageManager

        // Get user-installed apps with launcher intent
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter {
                (it.flags and ApplicationInfo.FLAG_SYSTEM == 0) &&
                        (pm.getLaunchIntentForPackage(it.packageName) != null)
            }
            .map {
                AppInfo(
                    name = it.loadLabel(pm).toString(),
                    packageName = it.packageName,
                    icon = it.loadIcon(pm)
                )
            }
            // Sort: blocked apps first, then alphabetical within each group
            .sortedWith(
                compareBy<AppInfo> { !blockedApps.contains(it.packageName) }
                    .thenBy { it.name.lowercase() }
            )

        // Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Setup Adapter with toggle listener
        adapter = AppListAdapter(installedApps, blockedApps) { packageName, isBlocked ->
            if (isBlocked) {
                blockedApps.add(packageName)
                Toast.makeText(this, "Blocked: $packageName", Toast.LENGTH_SHORT).show()
            } else {
                blockedApps.remove(packageName)
                Toast.makeText(this, "Unblocked: $packageName", Toast.LENGTH_SHORT).show()
            }
            saveBlockedApps()
        }

        recyclerView.adapter = adapter
    }

    private fun saveBlockedApps() {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit()
            .putStringSet(BLOCKED_KEY, blockedApps)
            .apply()
    }
}
