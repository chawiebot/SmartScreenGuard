package com.example.smartscreenguard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsLayout: LinearLayout
    private lateinit var dots: Array<ImageView?>
    private lateinit var getStartedButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen and immersive UI
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        actionBar?.hide()
        supportActionBar?.hide()

        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        if (prefs.getBoolean("onboarding_complete", false)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        dotsLayout = findViewById(R.id.dotsLayout)
        getStartedButton = findViewById(R.id.buttonGetStarted)

        val onboardingItems = listOf(
            OnboardingItem(R.drawable.onboarding1, ""),
            OnboardingItem(R.drawable.onboarding2, ""),
            OnboardingItem(R.drawable.onboarding3, ""),
            OnboardingItem(R.drawable.onboarding4, "")
        )

        // Pass a lambda to handle "Get Started"
        viewPager.adapter = OnboardingAdapter(onboardingItems) {
            completeOnboarding()
        }

        getStartedButton.setOnClickListener {
            completeOnboarding()
        }

        setupDots(onboardingItems.size)
        setCurrentDot(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentDot(position)

                // Show "Get Started" only on the last page
                getStartedButton.visibility =
                    if (position == onboardingItems.lastIndex) View.VISIBLE else View.GONE
            }
        })
    }

    private fun completeOnboarding() {
        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
            .putBoolean("onboarding_complete", true).apply()

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun setupDots(count: Int) {
        dots = arrayOfNulls(count)
        dotsLayout.removeAllViews()

        for (i in 0 until count) {
            dots[i] = ImageView(this).apply {
                setImageResource(R.drawable.dot_inactive)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 0, 8, 0)
                }
            }
            dotsLayout.addView(dots[i])
        }
    }

    private fun setCurrentDot(index: Int) {
        for (i in dots.indices) {
            dots[i]?.setImageResource(
                if (i == index) R.drawable.dot_active else R.drawable.dot_inactive
            )
        }
    }
}
