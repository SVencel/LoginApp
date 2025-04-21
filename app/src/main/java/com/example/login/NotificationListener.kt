package com.example.login

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.content.Intent
import android.content.Context

class NotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        val prefs = getSharedPreferences("monitorPrefs", Context.MODE_PRIVATE)
        val today = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        val key = "notifCount_$today"

        val count = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, count).apply()

        Log.d("NOTIF_TRACK", "🔔 Notification posted. Count = $count")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Optional: You could decrement here if desired
    }

    override fun onListenerConnected() {
        Log.d("NOTIF_TRACK", "✅ NotificationListener connected")
    }
}
