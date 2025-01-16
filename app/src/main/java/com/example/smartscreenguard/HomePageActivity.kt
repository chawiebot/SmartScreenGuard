package com.example.smartscreenguard

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomePageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage) // Replace with your actual layout file name

        // Initialize views
        val appTitle: TextView = findViewById(R.id.app_title)
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Set up BottomNavigationView listener
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Navigate to Home
                    navigateToHome()
                    true
                }
                R.id.nav_focus -> {
                    // Navigate to Focus
                    navigateToFocus()
                    true
                }
                R.id.nav_settings -> {
                    // Navigate to Settings
                    navigateToSettings()
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateToHome() {
        // Logic to navigate to Home
        // Example: startActivity(Intent(this, HomeActivity::class.java))
    }

    private fun navigateToFocus() {
        // Logic to navigate to Focus
        // Example: startActivity(Intent(this, FocusActivity::class.java))
    }

    private fun navigateToSettings() {
        // Logic to navigate to Settings
        // Example: startActivity(Intent(this, SettingsActivity::class.java))
    }
}
