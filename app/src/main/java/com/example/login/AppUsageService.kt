package com.example.login

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

    // Timing and limits
    private val minScrollInterval = 4000L
    private val resetDelay = 30000L
    private val scrollCooldownAfterAppOpen = 3000L
    private val extraMinuteIdleTimeout = 30_000L
    private val usageLimit = 2 * 60 * 60 * 1000L
    private val doomscrollLimit = 3

    // Runtime tracking
    private var scrollCount = 0
    private var lastScrollTime = 0L
    private var lastAppOpenedTime = 0L
    private var hasWarned = false
    private var extraMinuteTimerStarted = false
    private var isYoutubeFullscreen = false
    private var youtubeIdleRunnable: Runnable? = null

    private val resetScrollCountRunnable = Runnable {
        scrollCount = 0
        hasWarned = false
    }

    private val autoCloseRunnable = Runnable {
        performGlobalAction(GLOBAL_ACTION_HOME)
        handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, 200)
        extraMinuteTimerStarted = false
        getSharedPreferences("doomPrefs", Context.MODE_PRIVATE)
            .edit().putLong("allowUntil", 0L).apply()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        if (packageName == "com.tiktok.android" || packageName == "com.zhiliaoapp.musically") {
            handleTiktokDoomscroll(event)
        }

        if (packageName == "com.google.android.youtube") {
            checkYoutubeIdleDoomscroll(event)
        }

        isAppBlockedBySectionAsync(packageName) { isBlocked ->
            if (isBlocked) {
                resetStreakInFirebase()
                showNotification("$packageName is restricted during this time!")
                handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, 100)
                return@isAppBlockedBySectionAsync
            }
            handleEventAfterBlockingCheck(event, packageName)
        }
    }

    private fun handleEventAfterBlockingCheck(event: AccessibilityEvent, packageName: String) {
        val allowUntil = getSharedPreferences("doomPrefs", Context.MODE_PRIVATE)
            .getLong("allowUntil", 0L)
        val currentTime = System.currentTimeMillis()

        val isScrollEvent = event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
        val isContentChanged = event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED

        if (isDoomscrollingEnabled() && isMonitoredForDoomscrolling(packageName)) {
            if (currentTime - lastAppOpenedTime < scrollCooldownAfterAppOpen) return

            if (packageName == "com.google.android.youtube") {
                checkYoutubeIdleDoomscroll(event)
            }

            if (isScrollEvent || isContentChanged) {
                if (currentTime - lastScrollTime >= minScrollInterval) {
                    scrollCount++
                    lastScrollTime = currentTime
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
                        showNotification("⚠️ You're 2 scrolls away from a doomscroll alert.")
                    }

                    if (scrollCount >= scrollLimit) {
                        hasWarned = false
                        showDoomscrollAlert(packageName)
                        incrementDoomscrollCount()
                    }

                    handler.removeCallbacks(resetScrollCountRunnable)
                    handler.postDelayed(resetScrollCountRunnable, resetDelay)
                }
            }
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            scrollCount = 0
            lastAppOpenedTime = currentTime
            handler.removeCallbacks(resetScrollCountRunnable)
        }

        trackDailyUsage()

        if (packageName != "com.google.android.youtube") {
            youtubeIdleRunnable?.let {
                handler.removeCallbacks(it)
                youtubeIdleRunnable = null
                isYoutubeFullscreen = false
            }
        }
    }

    private fun handleTiktokDoomscroll(event: AccessibilityEvent) {
        val className = event.className?.toString() ?: ""
        val textContent = event.text?.joinToString()?.lowercase() ?: ""
        Log.d("TT_DEBUG", "📱 TikTok: class=$className, text=$textContent, type=${event.eventType}")
    }

    private fun checkYoutubeIdleDoomscroll(event: AccessibilityEvent) {
        val className = event.className?.toString() ?: ""
        val textContent = event.text?.joinToString()?.lowercase() ?: ""

        val isNonFullscreen = textContent.contains("shorts") ||
                textContent.contains("explore") ||
                textContent.contains("home") ||
                textContent.contains("subscriptions") ||
                textContent.contains("browse")

        val isFullscreen = className.contains("DrawerLayout") ||
                className.contains("FrameLayout") ||
                className.contains("SurfaceView")

        if (isNonFullscreen && !isFullscreen && youtubeIdleRunnable == null) {
            youtubeIdleRunnable = Runnable {
                if (!isYoutubeFullscreen) {
                    showDoomscrollAlert("YouTube")
                    incrementDoomscrollCount()
                }
            }
            handler.postDelayed(youtubeIdleRunnable!!, 60_000)
        }

        if (isFullscreen) {
            isYoutubeFullscreen = true
            youtubeIdleRunnable?.let { handler.removeCallbacks(it) }
            youtubeIdleRunnable = null
        }
    }

    private fun getScrollLimitFromSensitivity(): Int {
        return when (getSharedPreferences("doomPrefs", Context.MODE_PRIVATE)
            .getInt("doomSensitivity", 1)) {
            0 -> 15
            1 -> 9
            2 -> 6
            else -> 9
        }
    }

    fun forceCloseAfterGracePeriod() {
        handler.postDelayed({
            val allowUntil = getSharedPreferences("doomPrefs", Context.MODE_PRIVATE)
                .getLong("allowUntil", 0L)
            if (System.currentTimeMillis() >= allowUntil) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                getSharedPreferences("doomPrefs", Context.MODE_PRIVATE)
                    .edit().putLong("allowUntil", 0L).apply()
                extraMinuteTimerStarted = false
            }
        }, 2 * 60_000)
    }

    private fun isDoomscrollingEnabled(): Boolean {
        return getSharedPreferences("doomPrefs", Context.MODE_PRIVATE)
            .getBoolean("doomEnabled", true)
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
                    val newStreak = currentStreak + 1
                    userRef.update(
                        mapOf(
                            "streakCount" to newStreak,
                            "lastCheckedDate" to currentDate,
                            "doomscrollAlerts" to 0
                        )
                    )

                    val historyData = mapOf(
                        "streak" to newStreak,
                        "date" to currentDate,
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("users").document(user.uid)
                        .collection("streakHistory")
                        .document(currentDate)
                        .set(historyData)
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
            .update(mapOf("streakCount" to 0, "doomscrollAlerts" to 0))
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

    private fun showDoomscrollAlert(packageName: String) {
        val channelId = "hardcore_mode_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid)
            .collection("onboardingAnswers")
            .document("initialSurvey")
            .get()
            .addOnSuccessListener { doc ->
                val allGoals = mutableListOf<String>()
                val listGoals = doc.get("goal_30_days") as? List<*> ?: emptyList<Any>()
                allGoals.addAll(listGoals.mapNotNull { it?.toString()?.takeIf { it.isNotBlank() } })
                listOf("goal_1", "goal_2", "goal_3").forEach { key ->
                    doc.getString(key)?.takeIf { it.isNotBlank() }?.let { allGoals.add(it) }
                }

                val randomGoal = allGoals.randomOrNull() ?: "Stay focused and in control 💪"
                val bigText = "You've been scrolling in $packageName for a while.\nTake a break or allow 2 more minutes.\n\n🎯 Goal: \"$randomGoal\""

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
                    .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
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
                performGlobalAction(GLOBAL_ACTION_HOME)
                handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, 200)
            }
            .addOnFailureListener {
                Log.w("NOTIF_GOAL", "❌ Failed to fetch goals: ${it.message}")
            }
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
                    if (!apps.any { it.toString() == packageName }) continue
                    if (!(doc.getBoolean("enabled") ?: true)) continue

                    val start = (doc.getLong("startHour") ?: 0L).toInt() * 60 +
                            (doc.getLong("startMinute") ?: 0L).toInt()
                    val end = (doc.getLong("endHour") ?: 0L).toInt() * 60 +
                            (doc.getLong("endMinute") ?: 0L).toInt()
                    val days = (doc.get("days") as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList()

                    val inTimeRange = if (start < end) {
                        currentMinutes in start until end
                    } else {
                        currentMinutes >= start || currentMinutes < end
                    }

                    if (adjustedDay in days && inTimeRange) {
                        onResult(true)
                        return@addOnSuccessListener
                    }
                }

                onResult(false)
            }
            .addOnFailureListener { onResult(false) }
    }

    private fun isMonitoredForDoomscrolling(packageName: String): Boolean {
        val monitored = listOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.tiktok.android",
            "com.zhiliaoapp.musically",
            "com.reddit.frontpage",
            "com.snapchat.android",
            "com.google.android.youtube"
        )
        return packageName in monitored
    }

    class DoomscrollActionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "ALLOW_EXTRA_SCROLLING") {
                val prefs = context.getSharedPreferences("doomPrefs", Context.MODE_PRIVATE)
                prefs.edit().putLong("allowUntil", System.currentTimeMillis() + 2 * 60_000).apply()

                val intent = Intent(context, AppUsageService::class.java).apply {
                    putExtra("action", "start_auto_close_timer")
                }
                context.startService(intent)
            }
        }
    }
}
