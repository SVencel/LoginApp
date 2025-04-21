package com.example.login

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent
import android.util.Log
import android.content.Context

class NotificationTrackerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName
        Log.d("NotifTracker", "📩 Notification from: $packageName")

        val prefs = getSharedPreferences("notifStats", Context.MODE_PRIVATE)
        val currentCount = prefs.getInt("totalNotifsToday", 0)

        prefs.edit().putInt("totalNotifsToday", currentCount + 1).apply()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotifTracker", "✅ Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("NotifTracker", "❌ Notification listener disconnected")
    }

    // Optional: If you want to track specific apps only:
    private fun isMonitoredApp(pkg: String): Boolean {
        val monitored = listOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.tiktok.android",
            "com.reddit.frontpage"
        )
        return pkg in monitored
    }
}
