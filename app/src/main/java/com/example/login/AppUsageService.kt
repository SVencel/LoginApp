package com.example.login

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import android.os.Handler
import android.os.Looper

class AppUsageService : AccessibilityService() {

    private var scrollCount = 0  // Track scroll events
    private val scrollThreshold = 5  // Number of allowed scrolls before alert
    private var lastScrollTime = 0L  // Track last scroll event timestamp
    private val minScrollInterval = 1000L  // 1 second cooldown between scroll counts
    private val resetDelay = 5000L  // **5 seconds** before resetting scroll count
    private val handler = Handler(Looper.getMainLooper())  // Handler for delayed reset

    private val resetScrollCountRunnable = Runnable {
        Log.d("AppUsageService", "Resetting scroll count due to inactivity.")
        scrollCount = 0
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName != "com.instagram.android") return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val currentTime = System.currentTimeMillis()

                // ✅ Only count if at least 1 second has passed since last scroll
                if (currentTime - lastScrollTime >= minScrollInterval) {
                    scrollCount++
                    lastScrollTime = currentTime  // Update timestamp

                    Log.d("AppUsageService", "Instagram scroll detected. Recent scrolls: $scrollCount")

                    if (scrollCount >= scrollThreshold) {
                        Log.w("AppUsageService", "🚨 Possible doomscrolling detected!")
                        showNotification("You might be doomscrolling on Instagram! Take a break.")
                    }

                    // Reset scroll count after inactivity (5 seconds)
                    handler.removeCallbacks(resetScrollCountRunnable)  // Cancel any previous reset
                    handler.postDelayed(resetScrollCountRunnable, resetDelay)
                } else {
                    Log.d("AppUsageService", "Ignored duplicate scroll (too fast)")
                }
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                Log.d("AppUsageService", "Instagram page changed: ${event.className}")
                scrollCount = 0  // Reset scroll counter if page changes
                handler.removeCallbacks(resetScrollCountRunnable)  // Stop any pending reset
            }

            else -> {
                Log.d("AppUsageService", "Unhandled event type: ${event.eventType}")
            }
        }
    }

    override fun onInterrupt() {}

    private fun showNotification(message: String) {
        val channelId = "doomscroll_alerts"
        val channelName = "Doomscroll Alerts"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Doomscroll Alert")
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
