package com.example.login.hardcore

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast

object HardcoreUtils {
    fun applyHardcorePolicy(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (manager.isNotificationPolicyAccessGranted) {
                val policy = NotificationManager.Policy(
                    NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or
                            NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES or
                            NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS,
                    NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or
                            NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES or
                            NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS,
                    NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                    0 // No visual suppression
                )

                manager.notificationPolicy = policy
                manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } else {
                Toast.makeText(context, "⚠️ Grant DND access to apply Hardcore Mode", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }
}
