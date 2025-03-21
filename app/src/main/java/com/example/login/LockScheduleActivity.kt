package com.example.login

import android.app.AppOpsManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager

class LockScheduleActivity : AppCompatActivity() {

    private lateinit var startTimeButton: Button
    private lateinit var endTimeButton: Button
    private lateinit var saveButton: Button
    private lateinit var searchAppInput: EditText
    private lateinit var appRecyclerView: RecyclerView
    private lateinit var appAdapter: AppListAdapter

    private val selectedApps = mutableSetOf<String>()
    private var startHour = 20
    private var startMinute = 0
    private var endHour = 8
    private var endMinute = 0
    private val appList = mutableListOf<AppInfo>()
    private val filteredAppList = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_schedule)

        // Initialize Views
        startTimeButton = findViewById(R.id.btnStartTime)
        endTimeButton = findViewById(R.id.btnEndTime)
        saveButton = findViewById(R.id.btnSaveSchedule)
        searchAppInput = findViewById(R.id.searchAppInput)
        appRecyclerView = findViewById(R.id.appRecyclerView)

        appRecyclerView.layoutManager = LinearLayoutManager(this)
        appAdapter = AppListAdapter(filteredAppList, selectedApps)
        appRecyclerView.adapter = appAdapter

        // Request permission only if it's not granted
        if (!hasUsageStatsPermission()) {
            requestUsageAccessPermission()
        } else {
            initializeUI()
        }

        // Implement search functionality
        searchAppInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterApps(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        searchAppInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                hideKeyboard()
                searchAppInput.clearFocus()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun initializeUI() {
        loadStoredData()
        loadInstalledApps()

        startTimeButton.setOnClickListener { pickTime(true) }
        endTimeButton.setOnClickListener { pickTime(false) }
        saveButton.setOnClickListener { saveSchedule() }
    }

    private fun pickTime(isStart: Boolean) {
        val hour = if (isStart) startHour else endHour
        val minute = if (isStart) startMinute else endMinute

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            if (isStart) {
                startHour = selectedHour
                startMinute = selectedMinute
                startTimeButton.text = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute)
            } else {
                endHour = selectedHour
                endMinute = selectedMinute
                endTimeButton.text = String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute)
            }
        }, hour, minute, true)
        timePickerDialog.show()
    }

    private fun loadStoredData() {
        val sharedPref = getSharedPreferences("LockSchedulePrefs", Context.MODE_PRIVATE)
        startHour = sharedPref.getInt("startHour", 20)
        startMinute = sharedPref.getInt("startMinute", 0)
        endHour = sharedPref.getInt("endHour", 8)
        endMinute = sharedPref.getInt("endMinute", 0)
        selectedApps.addAll(sharedPref.getStringSet("blockedApps", setOf()) ?: setOf())

        startTimeButton.text = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute)
        endTimeButton.text = String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute)
    }

    private fun loadInstalledApps() {
        val packageManager = packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        appList.clear()
        filteredAppList.clear()

        for (app in installedApps) {
            val appName = packageManager.getApplicationLabel(app).toString()
            val packageName = app.packageName

            if (packageManager.getLaunchIntentForPackage(packageName) != null) {
                val appInfo = AppInfo(appName, packageName)
                appList.add(appInfo)
                filteredAppList.add(appInfo) // Initially, filtered list is the same as full list
                Log.d("AppListDebug", "App Added: $appName ($packageName)")
            }
        }

        Log.d("AppListDebug", "Final Displayed Apps: ${appList.size}")
        appAdapter.notifyDataSetChanged()
    }

    private fun filterApps(query: String) {
        filteredAppList.clear()
        if (query.isEmpty()) {
            filteredAppList.addAll(appList)
        } else {
            val lowerQuery = query.lowercase(Locale.getDefault())
            filteredAppList.addAll(appList.filter { it.name.lowercase(Locale.getDefault()).contains(lowerQuery) })
        }
        appAdapter.notifyDataSetChanged()
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageAccessPermission() {
        if (!hasUsageStatsPermission()) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Please grant Usage Access permission", Toast.LENGTH_LONG).show()
        } else {
            initializeUI() // Load UI if permission is already granted
        }
    }

    private fun saveSchedule() {
        val sharedPref = getSharedPreferences("LockSchedulePrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("startHour", startHour)
            putInt("startMinute", startMinute)
            putInt("endHour", endHour)
            putInt("endMinute", endMinute)
            putStringSet("blockedApps", selectedApps)
            apply()
        }
        Toast.makeText(this, "Schedule and apps saved!", Toast.LENGTH_SHORT).show()
        finish()
    }
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchAppInput.windowToken, 0)
    }
}

// Data model for AppInfo
data class AppInfo(val name: String, val packageName: String)
