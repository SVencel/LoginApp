package com.example.login.hardcore

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.login.MainActivity
import com.example.login.R

class HardcoreModeService : Service() {

    override fun onCreate() {
        super.onCreate()
        startHardcoreNotification()
    }

    private fun startHardcoreNotification() {
        val channelId = "hardcore_mode_channel"

        // Optional: add a tap action
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_skull) // this icon shows in the status bar
            .setContentTitle("☠️ HARDCORE MODE IS ON")
            .setContentText("Only this app, calls, and SMS are allowed.")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true) // this prevents swipe to remove
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
