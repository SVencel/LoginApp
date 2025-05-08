package com.example.login

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.KeyguardManager
import android.util.Log
class PhoneUnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val isUnlocked = !km.isKeyguardLocked

        if ((action == Intent.ACTION_SCREEN_ON || action == Intent.ACTION_USER_PRESENT) && isUnlocked) {
            Log.d("UNLOCK_TRACK", "✅ Phone unlocked")

            val prefs = context.getSharedPreferences("monitorPrefs", Context.MODE_PRIVATE)

            // 🔢 Total counter
            val totalCount = prefs.getInt("unlockCount", 0)
            prefs.edit().putInt("unlockCount", totalCount + 1).apply()

            // 📅 Daily counter
            val today = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
            val key = "phoneOpens_$today"
            val todayCount = prefs.getInt(key, 0)
            prefs.edit().putInt(key, todayCount + 1).apply()

            Log.d("UNLOCK_TRACK", "📱 Daily: $key = ${todayCount + 1}")
        }
    }
}
