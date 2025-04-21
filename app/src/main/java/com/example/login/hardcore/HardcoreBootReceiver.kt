package com.example.login.hardcore

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class HardcoreBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val wasHardcoreOn = prefs.getBoolean("hardcoreMode", false)

            Log.d("BOOT_RECEIVER", "Phone rebooted. Hardcore mode was on? $wasHardcoreOn")

            if (wasHardcoreOn) {
                // Start service again
                context.startService(Intent(context, HardcoreModeService::class.java))
                HardcoreUtils.applyHardcorePolicy(context)
            }
        }
    }
}
