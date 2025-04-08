package com.example.login

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
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
        scrollCount = 0
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        isAppBlockedBySectionAsync(packageName) { isBlocked ->
            if (isBlocked) {
                Log.w("AppUsageService", "🚫 $packageName is blocked by section! Resetting streak.")
                resetStreakInFirebase()
                showNotification("$packageName is restricted during this time!")

                handler.postDelayed({
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }, 100)
                return@isAppBlockedBySectionAsync
            }

            handleEventAfterBlockingCheck(event, packageName)
        }
    }

    private fun handleEventAfterBlockingCheck(event: AccessibilityEvent, packageName: String) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (isDoomscrollingEnabled() && isMonitoredForDoomscrolling(packageName)) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastScrollTime >= minScrollInterval) {
                        scrollCount++
                        lastScrollTime = currentTime

                        if (scrollCount >= scrollThreshold) {
                            showNotification("You might be doomscrolling on $packageName! Take a break.")
                            incrementDoomscrollCount()
                        }

                        handler.removeCallbacks(resetScrollCountRunnable)
                        handler.postDelayed(resetScrollCountRunnable, resetDelay)
                    }
                }
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                scrollCount = 0
                handler.removeCallbacks(resetScrollCountRunnable)
            }
        }

        trackDailyUsage()
    }

    private fun isDoomscrollingEnabled(): Boolean {
        val prefs = getSharedPreferences("doomPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("doomEnabled", true)
    }


    override fun onInterrupt() {}

    private fun trackDailyUsage() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user.uid)

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            System.currentTimeMillis() - usageLimit,
            System.currentTimeMillis()
        )

        var totalTime = 0L
        for (stat in stats) totalTime += stat.totalTimeInForeground

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
                            "doomscrollAlerts" to 0
                        )
                    )
                } else {
                    resetStreakInFirebase()
                }
            }
        }
    }

    private fun incrementDoomscrollCount() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user.uid)

        userRef.get().addOnSuccessListener {
            val current = it.getLong("doomscrollAlerts")?.toInt() ?: 0
            userRef.update("doomscrollAlerts", current + 1)
        }
    }

    private fun resetStreakInFirebase() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance().collection("users").document(user.uid)
            .update(
                mapOf(
                    "streakCount" to 0,
                    "doomscrollAlerts" to 0
                )
            )
    }

    private fun showNotification(message: String) {
        val channelId = "app_alerts"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "App Alerts", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("App Restriction")
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        manager.notify(2, notification)
    }

    private fun isAppBlockedBySectionAsync(packageName: String, onResult: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false)
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid)
            .collection("sections")
            .get()
            .addOnSuccessListener { sections ->
                val now = Calendar.getInstance()
                val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                val currentDay = now.get(Calendar.DAY_OF_WEEK)
                val adjustedDay = if (currentDay == Calendar.SUNDAY) 7 else currentDay - 1

                for (doc in sections) {
                    val apps = doc.get("apps") as? List<*> ?: continue
                    val enabled = doc.getBoolean("enabled") ?: true

                    if (!apps.any { it.toString() == packageName }) continue

                    val startH = (doc.getLong("startHour") ?: 0L).toInt()
                    val startM = (doc.getLong("startMinute") ?: 0L).toInt()
                    val endH = (doc.getLong("endHour") ?: 0L).toInt()
                    val endM = (doc.getLong("endMinute") ?: 0L).toInt()
                    val days = (doc.get("days") as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList()

                    val start = startH * 60 + startM
                    val end = endH * 60 + endM

                    val inTimeRange = if (start < end) {
                        currentMinutes in start until end
                    } else {
                        currentMinutes >= start || currentMinutes < end
                    }

                    // ✅ Check both day and time
                    if (enabled && adjustedDay in days && inTimeRange) {
                        Log.d("BLOCK_CHECK", "Blocked: $packageName by section '${doc.getString("name")}' on day $adjustedDay and time $currentMinutes")
                        onResult(true)
                        return@addOnSuccessListener
                    }
                }

                onResult(false)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }


    private fun isMonitoredForDoomscrolling(packageName: String): Boolean {
        val monitored = listOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.tiktok.android",
            "com.reddit.frontpage",
            "com.snapchat.android"
        )
        return packageName in monitored
    }
}
