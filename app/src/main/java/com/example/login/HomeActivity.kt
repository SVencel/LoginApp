package com.example.login

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.recyclerview.widget.RecyclerView
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
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.DocumentReference
import java.util.*
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat



class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var barChart: BarChart
    private lateinit var streakChart: BarChart
    private lateinit var setLockScheduleButton: Button
    private lateinit var infoBox: LinearLayout
    private lateinit var goToSettingsButton: Button
    private lateinit var streakTextView: TextView
    private lateinit var chartViewPager: ViewPager2
    private lateinit var tabIndicator: TabLayout
    private lateinit var rootLayout: ViewGroup

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    private var sessionMinutes = 0
    private var lastPromptTime = 0L
    private var productivityThresholdMinutes = 60 // default
    private val sessionHandler = android.os.Handler()
    private lateinit var screenReceiver: BroadcastReceiver


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
            sessionHandler.postDelayed(this, 60_000)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 👇 If notification was tapped
        if (intent?.getBooleanExtra("SHOW_PRODUCTIVITY_DIALOG", false) == true) {
            showProductivityDialog()
        }

        auth = FirebaseAuth.getInstance()
        streakTextView = findViewById(R.id.tvStreakCount)
        chartViewPager = findViewById(R.id.chartViewPager)
        tabIndicator = findViewById(R.id.chartTabIndicator)
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

        createNotificationChannel()

        val user: FirebaseUser? = auth.currentUser
        val headerView = navigationView.getHeaderView(0)
        val navUsername = headerView.findViewById<TextView>(R.id.tvUsername)
        navUsername.text = user?.displayName ?: "Guest"

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> Toast.makeText(this, "Home Clicked", Toast.LENGTH_SHORT).show()
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                }
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

        // Setup ViewPager Adapter for Swiping Between Charts
        val chartAdapter = ChartPagerAdapter(this)
        chartViewPager.adapter = chartAdapter
        chartViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // Try to find the currently visible chart's ViewHolder
                val recyclerView = chartViewPager.getChildAt(0) as? RecyclerView
                val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
                        as? ChartPagerAdapter.ChartViewHolder

                // Clear the marker tooltip if any is visible
                chartViewPager.postDelayed({
                    val recyclerView = chartViewPager.getChildAt(0) as? RecyclerView
                    val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
                            as? ChartPagerAdapter.ChartViewHolder

                    viewHolder?.clearHighlight()
                }, 50)

            }
        })


        // Attach Tab Indicator to ViewPager2
        TabLayoutMediator(tabIndicator, chartViewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Streak History"
                1 -> "Daily Usage"
                2 -> "Weekly Usage"
                3 -> "Monthly Usage"
                else -> ""
            }
        }.attach()

        tabIndicator.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val position = tab.position
                val recyclerView = chartViewPager.getChildAt(0) as? RecyclerView
                val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
                        as? ChartPagerAdapter.ChartViewHolder

                viewHolder?.clearHighlight()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        fetchStreakFromFirebase()
        fetchStreakHistory()

        fetchProductivityThreshold()
        sessionHandler.postDelayed(sessionRunnable, 60_000)

        val screenIntentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }

        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        println("📴 Screen off - Reset session timer")
                        sessionMinutes = 0
                        sessionHandler.removeCallbacks(sessionRunnable)
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        println("📱 Screen on - Start session timer")
                        sessionHandler.postDelayed(sessionRunnable, 60_000)
                    }
                }
            }
        }
        registerReceiver(screenReceiver, screenIntentFilter)


    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "productivity_channel_id",
                "Productivity Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to check productivity after long screen time"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
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
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("SHOW_PRODUCTIVITY_DIALOG", false)) {
            showProductivityDialog()
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

        if (entries.isEmpty()) {
            barChart.setNoDataText("No usage data available")
            barChart.setNoDataTextColor(Color.GRAY) // Make it look better
        } else {
            val dataSet = BarDataSet(entries, "App Usage (Minutes)")
            val barData = BarData(dataSet)
            barChart.data = barData
            barChart.invalidate()
        }
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
        val userDocRef = db.collection("users").document(user.uid)

        userDocRef.get().addOnSuccessListener { document ->
            val currentStreak = document.getLong("streakCount")?.toInt() ?: 0
            val lastUpdatedDate = document.getString("lastStreakUpdate") ?: ""

            // Get today's date
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // ✅ If streak was already updated today, do nothing
            if (lastUpdatedDate == today) {
                println("✅ Debug: Streak already updated today ($today), skipping update.")
                return@addOnSuccessListener
            }

            // ✅ Check if user met streak criteria
            checkUserStreakCriteria(userDocRef, currentStreak, today)
        }
    }

    private fun fetchProductivityThreshold() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                productivityThresholdMinutes = doc.getLong("productivityPromptMinutes")?.toInt() ?: 60
                println("⏱ Productivity prompt threshold: $productivityThresholdMinutes minutes")
            }
            .addOnFailureListener {
                println("⚠️ Could not fetch custom threshold, using default.")
            }
    }

    private fun showProductivityDialog() {
        // Always send a notification
        showProductivityNotification()

        // Also show dialog if app is in foreground
        if (isAppInForeground()) {
            runOnUiThread {
                val builder = androidx.appcompat.app.AlertDialog.Builder(this)
                builder.setTitle("🧠 Are you being productive?")
                builder.setMessage("You've been using your device for $productivityThresholdMinutes minutes.")

                builder.setPositiveButton("✅ Yes") { _, _ ->
                    logProductivityResponse(true)
                }

                builder.setNegativeButton("❌ No") { _, _ ->
                    logProductivityResponse(false)
                }

                builder.setCancelable(true)
                builder.show()
            }
        }
    }

    private fun showProductivityNotification() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("SHOW_PRODUCTIVITY_DIALOG", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "productivity_channel_id")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🧠 Productivity Check")
            .setContentText("You've been using your phone for $productivityThresholdMinutes minutes. Being productive?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            with(NotificationManagerCompat.from(this)) {
                notify(101, builder.build())
            }
        }
    }




    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = applicationContext.packageName

        for (appProcess in appProcesses) {
            if (appProcess.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                appProcess.processName == packageName) {
                return true
            }
        }
        return false
    }


    private fun logProductivityResponse(isProductive: Boolean) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val data = mapOf(
            "timestamp" to now,
            "isProductive" to isProductive
        )

        db.collection("users")
            .document(user.uid)
            .collection("productivityCheck")
            .add(data)
            .addOnSuccessListener {
                println("📘 Logged productivity response: $isProductive")
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionHandler.removeCallbacks(sessionRunnable)
        unregisterReceiver(screenReceiver)
    }





    /**
     * ✅ Checks if user met the streak criteria and updates accordingly.
     */
    private fun checkUserStreakCriteria(userDocRef: DocumentReference, currentStreak: Int, today: String) {
        val db = FirebaseFirestore.getInstance()

        // Retrieve stored usage stats
        userDocRef.collection("usageStats").document(today).get()
            .addOnSuccessListener { usageDoc ->
                val totalScreenTime = usageDoc.getLong("totalScreenTime") ?: 0L  // In minutes
                val doomscrollAlerts = usageDoc.getLong("doomscrollAlerts") ?: 0L

                println("📊 Debug: Screen time = $totalScreenTime mins, Doomscroll alerts = $doomscrollAlerts")

                if (totalScreenTime < 120 && doomscrollAlerts <= 3) {
                    // ✅ User met the criteria → Increase streak
                    val newStreak = currentStreak + 1
                    updateStreak(userDocRef, newStreak, today)
                } else {
                    // ❌ User failed the criteria → Reset streak
                    updateStreak(userDocRef, 0, today)
                }
            }
    }

    /**
     * 🔥 Updates the streak count and last updated date.
     */
    private fun updateStreak(userDocRef: DocumentReference, newStreak: Int, today: String) {
        val updates = mapOf(
            "streakCount" to newStreak,
            "lastStreakUpdate" to today
        )

        userDocRef.update(updates)
            .addOnSuccessListener {
                println("🔥 Debug: Streak updated to $newStreak on $today")
                streakTextView.text = "Current Streak: $newStreak Days"
                checkStreakRewards(newStreak)
            }
            .addOnFailureListener { e ->
                println("❌ Error updating streak: ${e.message}")
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
