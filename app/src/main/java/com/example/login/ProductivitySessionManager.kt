package com.example.login.utils

import android.content.Context
import android.os.Handler
import android.os.Looper

class ProductivitySessionManager(
    private val context: Context,
    private val thresholdMinutes: Int = 60,
    private val onThresholdReached: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var sessionMinutes = 0
    private var lastPromptTime = 0L

    private val sessionRunnable = object : Runnable {
        override fun run() {
            sessionMinutes++
            if (sessionMinutes >= thresholdMinutes) {
                val now = System.currentTimeMillis()
                if (now - lastPromptTime > thresholdMinutes * 60 * 1000) {
                    lastPromptTime = now
                    onThresholdReached()
                    sessionMinutes = 0
                }
            }
            handler.postDelayed(this, 60_000)
        }
    }

    fun start() {
        handler.postDelayed(sessionRunnable, 60_000)
    }

    fun stop() {
        handler.removeCallbacks(sessionRunnable)
    }

    fun resetSession() {
        sessionMinutes = 0
    }
}
