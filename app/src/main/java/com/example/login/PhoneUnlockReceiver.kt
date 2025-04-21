package com.example.login

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PhoneUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_USER_PRESENT) {
            val prefs = context.getSharedPreferences("monitorPrefs", Context.MODE_PRIVATE)
            val today = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
            val key = "phoneOpens_$today"

            val count = prefs.getInt(key, 0) + 1
            prefs.edit().putInt(key, count).apply()

            Log.d("PhoneUnlock", "📱 Phone opened. Count = $count")
        }
    }
}
