package com.example.login

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class AppUsageService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            Log.d("AppUsageService", "App opened: $packageName") // ✅ Debugging Log

            if (packageName == "com.instagram.android") {
                showNotification("Instagram Opened! Monitoring started...")
                Log.d("AppUsageService", "Instagram detected!") // ✅ Log for Debugging
            }
        }
    }

    override fun onInterrupt() {}

    private fun showNotification(message: String) {
        val channelId = "app_usage_channel"
        val channelName = "App Usage Alerts"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 🔹 Create Notification Channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // 🔹 Check for Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e("AppUsageService", "Notification permission not granted")
                return // ❌ Don't send notification if permission is missing
            }
        }

        // 🔹 Build and Show Notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // 🔥 Default icon (replace with your own)
            .setContentTitle("App Monitor")
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
