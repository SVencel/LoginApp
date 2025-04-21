package com.example.login

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.login.hardcore.HardcoreModeService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val hardcoreEnabled = prefs.getBoolean("hardcoreMode", false)

            Log.d("BootReceiver", "Boot completed. Hardcore mode enabled: $hardcoreEnabled")

            if (hardcoreEnabled) {
                context.startService(Intent(context, HardcoreModeService::class.java))
            }
        }
    }
}
