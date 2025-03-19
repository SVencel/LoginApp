package com.example.login

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import java.util.*

class AppUsageService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())  // Handler for UI interactions
    private var scrollCount = 0  // Track scroll events
    private val scrollThreshold = 5  // Number of fast scrolls before alert
    private var lastScrollTime = 0L  // Track last scroll event timestamp
    private val minScrollInterval = 1000L  // 1-second cooldown between scroll counts
    private val resetDelay = 5000L  // 5 seconds before resetting scroll count

    private val resetScrollCountRunnable = Runnable {
        Log.d("AppUsageService", "Resetting scroll count due to inactivity.")
        scrollCount = 0
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // ✅ **Check if the opened app is locked**
        if (isAppLocked(packageName)) {
            Log.w("AppUsageService", "🚫 $packageName is locked! Redirecting user...")
            showNotification("$packageName is locked during this time!")

            // 🚀 **Send user to home screen**
            handler.postDelayed({
                performGlobalAction(GLOBAL_ACTION_HOME)
            }, 500)
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (packageName in getBlockedApps()) {
                    val currentTime = System.currentTimeMillis()

                    // ✅ Only count if at least 1 second has passed since last scroll
                    if (currentTime - lastScrollTime >= minScrollInterval) {
                        scrollCount++
                        lastScrollTime = currentTime  // Update timestamp

                        Log.d("AppUsageService", "$packageName scroll detected. Recent scrolls: $scrollCount")

                        if (scrollCount >= scrollThreshold) {
                            Log.w("AppUsageService", "🚨 Possible doomscrolling detected!")
                            showNotification("You might be doomscrolling on $packageName! Take a break.")
                        }

                        // Reset scroll count after inactivity (5 seconds)
                        handler.removeCallbacks(resetScrollCountRunnable)  // Cancel any previous reset
                        handler.postDelayed(resetScrollCountRunnable, resetDelay)
                    } else {
                        Log.d("AppUsageService", "Ignored duplicate scroll (too fast)")
                    }
                }
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                Log.d("AppUsageService", "Page changed in: $packageName")
                scrollCount = 0  // Reset scroll counter if page changes
                handler.removeCallbacks(resetScrollCountRunnable)  // Stop any pending reset
            }

            else -> {
                Log.d("AppUsageService", "Unhandled event type: ${event.eventType}")
            }
        }
    }

    override fun onInterrupt() {}

    // ✅ **Show Notifications**
    private fun showNotification(message: String) {
        val channelId = "app_alerts"
        val channelName = "App Alerts"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("App Lock Alert")
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, notification)
    }

    // ✅ **Check if an app is currently locked**
    private fun isAppLocked(packageName: String): Boolean {
        val sharedPref = getSharedPreferences("LockSchedulePrefs", Context.MODE_PRIVATE)
        val blockedApps = getBlockedApps()

        if (packageName !in blockedApps) return false

        val startHour = sharedPref.getInt("startHour", 20) // Default 20:00
        val startMinute = sharedPref.getInt("startMinute", 0)
        val endHour = sharedPref.getInt("endHour", 8) // Default 08:00
        val endMinute = sharedPref.getInt("endMinute", 0)

        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)

        val currentTime = currentHour * 60 + currentMinute
        val startTime = startHour * 60 + startMinute
        val endTime = endHour * 60 + endMinute

        return if (startTime < endTime) {
            currentTime in startTime until endTime
        } else {
            currentTime >= startTime || currentTime < endTime
        }
    }

    // ✅ **Retrieve the list of blocked apps**
    private fun getBlockedApps(): Set<String> {
        val sharedPref = getSharedPreferences("LockSchedulePrefs", Context.MODE_PRIVATE)
        return sharedPref.getStringSet("blockedApps", setOf()) ?: setOf()
    }
}
