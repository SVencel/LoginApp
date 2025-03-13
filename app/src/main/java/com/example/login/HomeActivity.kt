package com.example.login

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.google.firebase.auth.FirebaseAuth
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var usageTextView: TextView
    private lateinit var logoutButton: Button
    private lateinit var switchViewMode: Switch
    private lateinit var barChart: BarChart
    private lateinit var enableMonitoringButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize UI Elements
        auth = FirebaseAuth.getInstance()
        usageTextView = findViewById(R.id.tvUsageStats)
        logoutButton = findViewById(R.id.btnLogout)
        switchViewMode = findViewById(R.id.switchViewMode)
        barChart = findViewById(R.id.barChart)
        enableMonitoringButton = findViewById(R.id.btnEnableAccessibility)

        // Request Notification Permission (Android 13+)
        requestNotificationPermission()

        // Check and Request Permissions
        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission()
        } else {
            displayUsageStats()
        }

        // Handle Button Clicks
        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        enableMonitoringButton.setOnClickListener {
            requestAccessibilityPermission()
        }

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
    }

    /** ✅ REQUEST NOTIFICATION PERMISSION (Android 13+) **/
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

    /** ✅ HANDLE PERMISSION RESULT **/
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** ✅ CHECKING PERMISSIONS **/
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        Toast.makeText(this, "Please enable usage access permission", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Please enable the Accessibility Service for app tracking.", Toast.LENGTH_LONG).show()
    }

    /** ✅ DISPLAYING APP USAGE **/
    private fun displayUsageStats() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1) // Get last 24 hours
        val startTime = calendar.timeInMillis

        val stats: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        if (stats.isEmpty()) {
            usageTextView.text = "No usage data available. Please grant permission."
            return
        }

        val usageMap = mutableMapOf<String, Long>()

        for (usageStat in stats) {
            val packageName = usageStat.packageName
            val totalTime = usageStat.totalTimeInForeground
            if (totalTime > 0) {
                usageMap[packageName] = (usageMap[packageName] ?: 0) + totalTime
            }
        }

        val sortedUsage = usageMap.toList().sortedByDescending { (_, time) -> time }.toMap()
        val usageText = StringBuilder("App Usage in Last 24 Hours:\n\n")

        for ((app, time) in sortedUsage) {
            usageText.append("${app}: ${formatTime(time)}\n")
        }

        usageTextView.text = usageText.toString()
    }

    /** ✅ POPULATING BAR CHART **/
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
        val labels = mutableListOf<String>()
        var index = 0

        // Sort and filter apps to exclude system apps
        for ((app, time) in usageMap.toList()
            .sortedByDescending { it.second }
            .filterNot { (packageName, _) -> packageName.contains("launcher") || packageName.contains("systemui") }
            .take(10)) {

            entries.add(BarEntry(index.toFloat(), (time / 60000).toFloat()))
            labels.add(app)
            index++
        }

        val dataSet = BarDataSet(entries, "App Usage (Minutes)")
        dataSet.color = resources.getColor(R.color.primaryColor, null) // Customize Bar Color
        dataSet.valueTextSize = 12f // Increase text size

        val barData = BarData(dataSet)
        barChart.data = barData

        // Configure X-Axis
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f // Ensure every label is displayed
        xAxis.textSize = 12f // Make text larger
        xAxis.labelRotationAngle = -45f // 🔹 Rotate labels to prevent overlap

        // Configure Y-Axis
        barChart.axisLeft.axisMinimum = 0f // Start Y-axis at zero
        barChart.axisRight.isEnabled = false // Disable right Y-axis

        // Bar chart appearance
        barChart.description.isEnabled = false // Remove description text
        barChart.setFitBars(true) // Make bars fit properly
        barChart.invalidate() // Refresh chart
    }

    private fun formatTime(milliseconds: Long): String {
        val minutes = (milliseconds / 60000)
        return "$minutes min"
    }
}
