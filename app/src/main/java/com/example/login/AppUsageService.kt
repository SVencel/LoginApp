package com.example.login

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
    private var lastScrollTime = 0L
    private val minScrollInterval = 4000L
    private val resetDelay = 30000L
    private var hasWarned = false
    private var lastAppOpenedTime = 0L
    private val scrollCooldownAfterAppOpen = 3000L // 3 seconds cooldown
    private var extraMinuteTimerStarted = false
    private val extraMinuteIdleTimeout = 30_000L // 30 seconds
    private val autoCloseRunnable = Runnable {
        Log.d("EXTRA_MINUTE", "User idle during 1 more minute. Going home.")
        performGlobalAction(GLOBAL_ACTION_HOME)
        extraMinuteTimerStarted = false

        val prefs = getSharedPreferences("doomPrefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("allowUntil", 0L).apply()
    }



    private val usageLimit = 2 * 60 * 60 * 1000 // 2 hours in milliseconds
    private val doomscrollLimit = 3

    private val resetScrollCountRunnable = Runnable {
        scrollCount = 0
        hasWarned = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getStringExtra("action") == "start_auto_close_timer") {
            forceCloseAfterGracePeriod()
        }
        return super.onStartCommand(intent, flags, startId)
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        val pkg = event.packageName?.toString()
        val type = event.eventType
        Log.d("AC_EVENT", "pkg=$pkg, type=$type, class=${event.className}")

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

    private fun getScrollLimitFromSensitivity(): Int {
        val prefs = getSharedPreferences("doomPrefs", Context.MODE_PRIVATE)
        return when (prefs.getInt("doomSensitivity", 1)) {
            0 -> 15 // Soft
            1 -> 9  // Medium
            2 -> 6  // Hardcore
            else -> 9
        }
    }

    fun forceCloseAfterGracePeriod() {
        handler.postDelayed({
            val allowUntil = getSharedPreferences("doomPrefs", Context.MODE_PRIVATE)
                .getLong("allowUntil", 0L)
            if (System.currentTimeMillis() >= allowUntil) {
                Log.d("EXTRA_MINUTE", "⏰ Grace period ended. Closing app.")
                performGlobalAction(GLOBAL_ACTION_HOME)
                val prefs = getSharedPreferences("doomPrefs", Context.MODE_PRIVATE)
                prefs.edit().putLong("allowUntil", 0L).apply()
                extraMinuteTimerStarted = false
            }
        }, 2 * 60_000)
    }


    class DoomscrollActionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "ALLOW_EXTRA_SCROLLING") {
                val prefs = context.getSharedPreferences("doomPrefs", Context.MODE_PRIVATE)
                prefs.edit().putLong("allowUntil", System.currentTimeMillis() + 2 * 60_000).apply()

                // Trigger the service to schedule the auto-close
                val intent = Intent(context, AppUsageService::class.java)
                intent.putExtra("action", "start_auto_close_timer")
                context.startService(intent)
            }
        }
    }

    private fun handleEventAfterBlockingCheck(event: AccessibilityEvent, packageName: String) {
        val allowUntil = getSharedPreferences("doomPrefs", Context.MODE_PRIVATE)
            .getLong("allowUntil", 0L)

        val currentTime = System.currentTimeMillis()

        if (isDoomscrollingEnabled() && isMonitoredForDoomscrolling(packageName)) {

            if (System.currentTimeMillis() - lastAppOpenedTime < scrollCooldownAfterAppOpen) return

            val isYoutube = packageName == "com.google.android.youtube"
            val isScrollEvent = event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
            val isContentChanged = event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && isYoutube

            if (isScrollEvent || isContentChanged) {
                if (currentTime - lastScrollTime >= minScrollInterval) {
                    scrollCount++
                    lastScrollTime = currentTime
                    Log.d("SCROLL_DEBUG", "📜 Scroll detected in $packageName — count: $scrollCount")

                    val scrollLimit = getScrollLimitFromSensitivity()

                    if (currentTime < allowUntil) {
                        if (!extraMinuteTimerStarted) {
                            handler.postDelayed(autoCloseRunnable, extraMinuteIdleTimeout)
                            extraMinuteTimerStarted = true
                        }
                        return
                    }

                    if (!hasWarned && scrollCount == scrollLimit - 2) {
                        hasWarned = true
                        showNotification("⚠️ Heads up! You're 2 scrolls away from a doomscroll alert.")
                    }

                    if (scrollCount >= scrollLimit) {
                        hasWarned = false
                        showDoomscrollAlert(packageName)
                        incrementDoomscrollCount()
                    }

                    handler.removeCallbacks(resetScrollCountRunnable)
                    handler.postDelayed(resetScrollCountRunnable, resetDelay)
                } else {
                    Log.d("SCROLL_DEBUG", "🕒 Ignored scroll: too soon after last (${currentTime - lastScrollTime} ms)")
                }
            }
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            scrollCount = 0
            lastAppOpenedTime = currentTime
            handler.removeCallbacks(resetScrollCountRunnable)
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
        val channelId = "hardcore_mode_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("App Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        manager.notify(Random().nextInt(), notification)
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

    private fun showDoomscrollAlert(packageName: String) {
        val channelId = "hardcore_mode_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val allowIntent = Intent(this, DoomscrollActionReceiver::class.java).apply {
            action = "ALLOW_EXTRA_SCROLLING"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, allowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("😵 You might be doomscrolling on $packageName")
            .setContentText("Take a break or allow 2 more minutes.")
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                "Allow 2 More Minutes",
                pendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        manager.notify(Random().nextInt(), notification)
    }



    private fun isMonitoredForDoomscrolling(packageName: String): Boolean {
        val monitored = listOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.tiktok.android",
            "com.reddit.frontpage",
            "com.snapchat.android",
            "com.google.android.youtube"
        )
        return packageName in monitored
    }
}
