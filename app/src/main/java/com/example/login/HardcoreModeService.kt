package com.example.login

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class HardcoreModeService : Service() {

    override fun onCreate() {
        super.onCreate()
        showPlaceholderNotification()
    }

    private fun showPlaceholderNotification() {
        val channelId = "hardcore_mode_channel"

        // Notification tap returns to app (optional)
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_skull) // Placeholder icon for UI
            .setContentTitle("☠️ HARDCORE MODE IS ON")
            .setContentText("Only this app, calls, and SMS are allowed.")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
