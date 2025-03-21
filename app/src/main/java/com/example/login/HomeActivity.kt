package com.example.login

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.jinatonic.confetti.CommonConfetti
import com.github.jinatonic.confetti.ConfettiView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var usageTextView: TextView
    private lateinit var switchViewMode: Switch
    private lateinit var barChart: BarChart
    private lateinit var streakChart: BarChart
    private lateinit var setLockScheduleButton: Button
    private lateinit var infoBox: LinearLayout
    private lateinit var goToSettingsButton: Button
    private lateinit var streakTextView: TextView
    private lateinit var rootLayout: ViewGroup

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        usageTextView = findViewById(R.id.tvUsageStats)
        switchViewMode = findViewById(R.id.switchViewMode)
        barChart = findViewById(R.id.barChart)
        streakChart = findViewById(R.id.streakChart)
        setLockScheduleButton = findViewById(R.id.btnSetLockSchedule)
        infoBox = findViewById(R.id.infoBox)
        goToSettingsButton = findViewById(R.id.btnGoToSettings)
        streakTextView = findViewById(R.id.tvStreakCount)
        rootLayout = findViewById(R.id.rootLayout)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val user: FirebaseUser? = auth.currentUser
        val headerView = navigationView.getHeaderView(0)
        val navUsername = headerView.findViewById<TextView>(R.id.tvUsername)
        navUsername.text = user?.displayName ?: "Guest"

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> Toast.makeText(this, "Home Clicked", Toast.LENGTH_SHORT).show()
                R.id.nav_profile -> Toast.makeText(this, "Profile Clicked", Toast.LENGTH_SHORT).show()
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                R.id.nav_logout -> {
                    auth.signOut()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        setLockScheduleButton.setOnClickListener {
            startActivity(Intent(this, LockScheduleActivity::class.java))
        }

        goToSettingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        requestNotificationPermission()
        checkMonitoringStatus()

        switchViewMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                usageTextView.visibility = TextView.GONE
                barChart.visibility = BarChart.VISIBLE
                populateBarChart()
            } else {
                barChart.visibility = BarChart.GONE
                usageTextView.visibility = TextView.VISIBLE
            }
        }

        fetchStreakFromFirebase()
        fetchStreakHistory()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (toggle.onOptionsItemSelected(item)) true else super.onOptionsItemSelected(item)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private fun checkMonitoringStatus() {
        if (!isAccessibilityServiceEnabled()) {
            infoBox.visibility = LinearLayout.VISIBLE
        } else {
            infoBox.visibility = LinearLayout.GONE
        }
    }

    private fun populateBarChart() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = calendar.timeInMillis

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        val usageMap = mutableMapOf<String, Long>()
        for (usageStat in stats) {
            val totalTime = usageStat.totalTimeInForeground
            if (totalTime > 0) {
                usageMap[usageStat.packageName] = totalTime
            }
        }

        val entries = mutableListOf<BarEntry>()
        var index = 0

        for ((_, time) in usageMap.toList().sortedByDescending { it.second }.take(10)) {
            entries.add(BarEntry(index.toFloat(), (time / 60000).toFloat())) // in minutes
            index++
        }

        val dataSet = BarDataSet(entries, "App Usage (Minutes)")
        val barData = BarData(dataSet)
        barChart.data = barData
        barChart.invalidate()
    }


    private fun isAccessibilityServiceEnabled(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun fetchStreakFromFirebase() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val currentStreak = document.getLong("streakCount")?.toInt() ?: 0
                streakTextView.text = "Current Streak: $currentStreak Days"
                checkStreakRewards(currentStreak)
            }
    }

    private fun fetchStreakHistory() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid).collection("streakHistory")
            .get()
            .addOnSuccessListener { documents ->
                val entries = ArrayList<BarEntry>()
                var index = 0

                for (document in documents) {
                    val streakValue = document.getLong("streak")?.toInt() ?: 0
                    entries.add(BarEntry(index.toFloat(), streakValue.toFloat()))
                    index++
                }

                val dataSet = BarDataSet(entries, "Streak Progress")
                val barData = BarData(dataSet)
                streakChart.data = barData
                streakChart.invalidate()
            }
    }

    private fun checkStreakRewards(streak: Int) {
        when (streak) {
            7 -> {
                showToast("🔥 7-day streak! Keep going!")
                triggerConfetti()
            }
            14 -> {
                showToast("🏆 2-week streak! Amazing!")
                triggerConfetti()
            }
            30 -> {
                showToast("🌟 1-month streak! You're unstoppable!")
                triggerConfetti()
            }
        }
    }

    private fun triggerConfetti() {
        CommonConfetti.rainingConfetti(rootLayout, intArrayOf(Color.YELLOW, Color.RED, Color.BLUE))
            .stream(3000) // 3 seconds confetti
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
