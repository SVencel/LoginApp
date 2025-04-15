package com.example.login.utils

import android.content.Context

class ProductivitySessionManager(
    private val context: Context,
    private val thresholdMinutes: Int = 60,
    private val onThresholdReached: () -> Unit
) {
    fun start() {
        // Placeholder - design mode only
    }

    fun stop() {
        // Placeholder - design mode only
    }

    fun resetSession() {
        // Placeholder - design mode only
    }
}
