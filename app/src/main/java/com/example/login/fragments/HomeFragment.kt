package com.example.login.fragments

import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.login.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import android.provider.Settings
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

        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )

        val usageAccessGranted = mode == AppOpsManager.MODE_ALLOWED

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val accessibilityGranted = enabledServices?.contains(context.packageName) == true

        val shouldPrompt = !usageAccessGranted || !accessibilityGranted

        if (shouldPrompt) {
            AlertDialog.Builder(context)
                .setTitle("Enable Monitoring")
                .setMessage("To track app usage and limit distractions, please enable Usage Access and Accessibility permissions.")
                .setPositiveButton("Go to Settings") { _, _ ->
                    val intent = if (!usageAccessGranted) {
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    } else {
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    }
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

}
