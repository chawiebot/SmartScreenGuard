package com.example.smartscreenguard

data class OnboardingItem(
    val imageResId: Int,
    val text: String,
    val showButton: Boolean = false
)