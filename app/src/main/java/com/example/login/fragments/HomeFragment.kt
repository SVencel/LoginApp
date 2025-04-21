    package com.example.login.fragments

    import android.app.AlertDialog
    import android.app.AppOpsManager
    import android.content.Context
    import android.content.Intent
    import android.os.Bundle
    import android.Manifest
    import android.content.pm.PackageManager
    import android.os.Build
    import androidx.core.app.NotificationManagerCompat
    import androidx.core.content.ContextCompat
    import android.view.*
    import android.widget.TextView
    import androidx.fragment.app.Fragment
    import com.example.login.R
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore
    import java.text.SimpleDateFormat
    import android.provider.Settings
    import android.widget.Toast
    import java.util.*

    class HomeFragment : Fragment() {

        private lateinit var tvStreakCount: TextView
        private lateinit var tvSummary: TextView
        private lateinit var tvQuote: TextView

        private val quotes = listOf(
            "“Small steps every day.”",
            "“Progress over perfection.”",
            "“Stay focused, stay sharp.”",
            "“One day at a time.”",
            "“Your future self will thank you.”"
        )

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
        ): View {
            val view = inflater.inflate(R.layout.fragment_home, container, false)

            tvStreakCount = view.findViewById(R.id.tvStreak)
            tvSummary = view.findViewById(R.id.tvSummary)
            tvQuote = view.findViewById(R.id.tvQuote)

            tvQuote.text = quotes.random()
            fetchStreak()
            checkMonitoringStatus()

            return view
        }

        override fun onResume() {
            super.onResume()
            checkMonitoringStatus()
        }

        private fun fetchStreak() {
            val user = FirebaseAuth.getInstance().currentUser ?: return
            val db = FirebaseFirestore.getInstance()

            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener {
                    val streak = it.getLong("streakCount")?.toInt() ?: 0
                    tvStreakCount.text = "🔥 $streak-day streak"
                    tvSummary.text = when {
                        streak == 0 -> "Let’s get started today!"
                        streak < 3 -> "Nice work! Keep it up!"
                        streak < 7 -> "Great focus this week!"
                        else -> "You're on fire! 🔥"
                    }
                }
        }

        private fun checkMonitoringStatus() {
            val context = requireContext()

            // Check Usage Access
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val usageGranted = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            ) == AppOpsManager.MODE_ALLOWED

            // Check Accessibility
            val accessibilityGranted = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )?.contains(context.packageName) == true

            // Check POST_NOTIFICATIONS
            val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }

            // 🔔 Check Notification Listener Access
            val notificationListenerGranted = isNotificationAccessEnabled(context)

            // If any of them is missing
            if (!usageGranted || !accessibilityGranted || !notificationGranted || !notificationListenerGranted) {
                val missing = buildList {
                    if (!usageGranted) add("Usage Access")
                    if (!accessibilityGranted) add("Accessibility")
                    if (!notificationGranted) add("Notifications")
                    if (!notificationListenerGranted) add("Notification Access")
                }.joinToString(", ")

                AlertDialog.Builder(context)
                    .setTitle("Enable Permissions")
                    .setMessage("To monitor distractions and provide insights, please enable:\n\n$missing.")
                    .setPositiveButton("Go to Settings") { _, _ ->
                        when {
                            !usageGranted -> startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            !accessibilityGranted -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            !notificationGranted -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    requestPermissions(
                                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                        1002
                                    )
                                } else {
                                    startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    })
                                }
                            }
                            !notificationListenerGranted -> {
                                startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }



        private fun isNotificationAccessEnabled(context: Context): Boolean {
            val packageName = context.packageName
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return enabledListeners?.contains(packageName) == true
        }


        @Deprecated("Use registerForActivityResult instead in the future")
        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == 1002) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireContext(), "✅ Notifications permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "❌ Notifications permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }
