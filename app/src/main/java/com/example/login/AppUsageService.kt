package com.example.login

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat

class AppUsageService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var scrollCount = 0
    private val scrollThreshold = 5
    private var lastScrollTime = 0L
    private val minScrollInterval = 1000L
    private val resetDelay = 5000L

    private val resetScrollCountRunnable = Runnable {
        scrollCount = 0
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        // Simulate a hardcoded block list
        val blockedApps = listOf("com.instagram.android", "com.tiktok.android")

        if (packageName in blockedApps) {
            showNotification("🚫 $packageName is restricted right now!")
            handler.postDelayed({
                performGlobalAction(GLOBAL_ACTION_HOME)
            }, 100)
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastScrollTime >= minScrollInterval) {
                    scrollCount++
                    lastScrollTime = currentTime

                    if (scrollCount >= scrollThreshold) {
                        showNotification("⚠️ Doomscrolling alert! Take a break.")
                    }

                    handler.removeCallbacks(resetScrollCountRunnable)
                    handler.postDelayed(resetScrollCountRunnable, resetDelay)
                }
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                scrollCount = 0
                handler.removeCallbacks(resetScrollCountRunnable)
            }
        }
    }

    private fun showNotification(message: String) {
        val channelId = "design_mode_channel"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Design Mode Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Preview Mode")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onInterrupt() {}
}
