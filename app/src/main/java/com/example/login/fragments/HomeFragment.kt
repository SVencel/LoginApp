package com.example.login.fragments

import android.content.*
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.login.*
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var streakTextView: TextView
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var infoBox: LinearLayout
    private lateinit var goToSettings: Button
    private lateinit var rootLayout: ViewGroup

    private var sessionMinutes = 0
    private var lastPromptTime = 0L
    private var productivityThresholdMinutes = 60
    private val handler = android.os.Handler()

    private val sessionRunnable = object : Runnable {
        override fun run() {
            sessionMinutes++
            if (sessionMinutes >= productivityThresholdMinutes) {
                val now = System.currentTimeMillis()
                if (now - lastPromptTime > productivityThresholdMinutes * 60 * 1000) {
                    lastPromptTime = now
                    showProductivityDialog()
                    sessionMinutes = 0
                }
            }
            handler.postDelayed(this, 60_000)
        }
    }

    private lateinit var screenReceiver: BroadcastReceiver

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        streakTextView = view.findViewById(R.id.tvStreakCount)
        viewPager = view.findViewById(R.id.chartViewPager)
        tabLayout = view.findViewById(R.id.chartTabIndicator)
        infoBox = view.findViewById(R.id.infoBox)
        goToSettings = view.findViewById(R.id.btnGoToSettings)
        rootLayout = view.findViewById(R.id.rootLayout)

        viewPager.adapter = ChartPagerAdapter(requireContext())

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = listOf("Streak", "Daily", "Weekly", "Monthly").getOrNull(position) ?: ""
        }.attach()

        fetchStreak()
        fetchProductivityThreshold()
        checkMonitoringStatus()

        goToSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val recyclerView = viewPager.getChildAt(0) as? RecyclerView
                val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
                        as? ChartPagerAdapter.ChartViewHolder
                viewPager.postDelayed({ viewHolder?.clearHighlight() }, 50)
            }
        })

        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        sessionMinutes = 0
                        handler.removeCallbacks(sessionRunnable)
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        handler.postDelayed(sessionRunnable, 60_000)
                    }
                }
            }
        }

        requireContext().registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        )

        handler.postDelayed(sessionRunnable, 60_000)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(sessionRunnable)
        requireContext().unregisterReceiver(screenReceiver)
    }

    private fun fetchStreak() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener {
                val streak = it.getLong("streakCount")?.toInt() ?: 0
                streakTextView.text = "Current Streak: $streak Days"
            }
    }

    private fun fetchProductivityThreshold() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener {
                productivityThresholdMinutes = it.getLong("productivityPromptMinutes")?.toInt() ?: 60
            }
    }

    private fun showProductivityDialog() {
        if (!isAppInForeground()) return

        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("🧠 Are you being productive?")
        builder.setMessage("You've been on your device for $productivityThresholdMinutes minutes.")
        builder.setPositiveButton("✅ Yes") { _, _ -> logProductivityResponse(true) }
        builder.setNegativeButton("❌ No") { _, _ -> logProductivityResponse(false) }
        builder.show()
    }

    private fun logProductivityResponse(isProductive: Boolean) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        db.collection("users")
            .document(user.uid)
            .collection("productivityCheck")
            .add(mapOf("timestamp" to now, "isProductive" to isProductive))
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val processes = activityManager.runningAppProcesses ?: return false
        return processes.any {
            it.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    it.processName == requireContext().packageName
        }
    }

    private fun checkMonitoringStatus() {
        val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val usageEnabled = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            requireContext().packageName
        ) == android.app.AppOpsManager.MODE_ALLOWED

        val enabledServices = Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val accessibilityEnabled = enabledServices?.contains(requireContext().packageName) == true

        infoBox.visibility = if (!usageEnabled || !accessibilityEnabled) View.VISIBLE else View.GONE
    }
}
