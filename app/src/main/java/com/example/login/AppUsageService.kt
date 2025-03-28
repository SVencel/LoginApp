package com.example.login

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AppUsageService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var scrollCount = 0
    private val scrollThreshold = 5
    private var lastScrollTime = 0L
    private val minScrollInterval = 1000L
    private val resetDelay = 5000L

    private val usageLimit = 2 * 60 * 60 * 1000 // 2 hours in milliseconds
    private val doomscrollLimit = 3

    private val resetScrollCountRunnable = Runnable {
        Log.d("AppUsageService", "Resetting scroll count due to inactivity.")
        scrollCount = 0
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // ✅ **Check if the opened app is locked**
        if (isAppLocked(packageName)) {
            Log.w("AppUsageService", "🚫 $packageName is locked! Resetting streak.")
            resetStreakInFirebase()
            showNotification("$packageName is locked! Your streak has been reset.")

            handler.postDelayed({
                performGlobalAction(GLOBAL_ACTION_HOME)
            }, 500)
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (isMonitoredForDoomscrolling(packageName)) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastScrollTime >= minScrollInterval) {
                        scrollCount++
                        lastScrollTime = currentTime

                        Log.d("AppUsageService", "$packageName scroll detected. Recent scrolls: $scrollCount")

                        if (scrollCount >= scrollThreshold) {
                            Log.w("AppUsageService", "🚨 Possible doomscrolling detected!")
                            showNotification("You might be doomscrolling on $packageName! Take a break.")
                            incrementDoomscrollCount()
                        }

                        handler.removeCallbacks(resetScrollCountRunnable)
                        handler.postDelayed(resetScrollCountRunnable, resetDelay)
                    }
                }
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                Log.d("AppUsageService", "Page changed in: $packageName")
                scrollCount = 0
                handler.removeCallbacks(resetScrollCountRunnable)
            }
        }

        // ✅ Track total usage time & update streak
        trackDailyUsage()
    }

    override fun onInterrupt() {}

    // ✅ **Track Total Usage Time**
    private fun trackDailyUsage() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user.uid)

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, System.currentTimeMillis() - usageLimit, System.currentTimeMillis()
        )

        var totalTime = 0L
        for (stat in stats) {
            totalTime += stat.totalTimeInForeground
        }

        userRef.get().addOnSuccessListener { document ->
            val lastCheckedDate = document.getString("lastCheckedDate") ?: ""
            val doomscrollAlerts = document.getLong("doomscrollAlerts")?.toInt() ?: 0
            val currentStreak = document.getLong("streakCount")?.toInt() ?: 0

            if (lastCheckedDate != currentDate) {
                if (totalTime < usageLimit && doomscrollAlerts <= doomscrollLimit) {
                    userRef.update(
                        mapOf(
                            "streakCount" to (currentStreak + 1),
                            "lastCheckedDate" to currentDate,
                            "doomscrollAlerts" to 0 // Reset daily doomscroll alerts
                        )
                    )
                    Log.d("StreakFeature", "Streak increased! Current streak: ${currentStreak + 1}")
                } else {
                    resetStreakInFirebase()
                }
            }
        }
    }

    // ✅ **Increment Doomscroll Count in Firebase**
    private fun incrementDoomscrollCount() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user.uid)

        userRef.get().addOnSuccessListener { document ->
            val doomscrollAlerts = document.getLong("doomscrollAlerts")?.toInt() ?: 0
            userRef.update("doomscrollAlerts", doomscrollAlerts + 1)
        }
    }

    // ✅ **Reset Streak in Firebase**
    private fun resetStreakInFirebase() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid)
            .update(
                mapOf(
                    "streakCount" to 0,
                    "doomscrollAlerts" to 0
                )
            )
            .addOnSuccessListener {
                Log.d("StreakFeature", "Streak reset to 0.")
            }
    }

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

        val startHour = sharedPref.getInt("startHour", 20)
        val startMinute = sharedPref.getInt("startMinute", 0)
        val endHour = sharedPref.getInt("endHour", 8)
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

    private fun isMonitoredForDoomscrolling(packageName: String): Boolean {
        val monitoredPackages = listOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.tiktok.android",
            "com.reddit.frontpage",
            "com.snapchat.android"
            // ✅ Add any others you want to monitor here
        )
        return packageName in monitoredPackages
    }

}
