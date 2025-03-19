package com.example.login

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class LockScheduleActivity : AppCompatActivity() {

    private lateinit var startTimeButton: Button
    private lateinit var endTimeButton: Button
    private lateinit var saveButton: Button
    private lateinit var appListView: ListView
    private val selectedApps = mutableSetOf<String>()

    private var startHour = 20
    private var startMinute = 0
    private var endHour = 8
    private var endMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_schedule)

        startTimeButton = findViewById(R.id.btnStartTime)
        endTimeButton = findViewById(R.id.btnEndTime)
        saveButton = findViewById(R.id.btnSaveSchedule)
        appListView = findViewById(R.id.appListView)

        loadStoredData()
        loadInstalledApps()

        startTimeButton.setOnClickListener { pickTime(true) }
        endTimeButton.setOnClickListener { pickTime(false) }

        saveButton.setOnClickListener {
            saveSchedule()
        }
        requestUsageAccessPermission()
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

        val appsList = mutableListOf<Pair<String, String>>() // Pair<AppName, PackageName>

        for (app in installedApps) {
            val appName = packageManager.getApplicationLabel(app).toString()
            val packageName = app.packageName

            if (packageManager.getLaunchIntentForPackage(packageName) != null) {
                appsList.add(Pair(appName, packageName))
            }
        }

        appListView.adapter = AppListAdapter(this, appsList, selectedApps)
    }


    private fun requestUsageAccessPermission() {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), packageName
        )

        if (mode != android.app.AppOpsManager.MODE_ALLOWED) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Please grant Usage Access permission", Toast.LENGTH_LONG).show()
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
}
