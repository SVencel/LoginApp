package com.example.login

import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
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
    }

    private fun pickTime(isStart: Boolean) {
        val calendar = Calendar.getInstance()
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

        val appNames = mutableListOf<String>()
        val appPackageNames = mutableListOf<String>()

        for (app in installedApps) {
            val appName = packageManager.getApplicationLabel(app).toString()
            val packageName = app.packageName

            if (packageManager.getLaunchIntentForPackage(packageName) != null) { // Exclude system apps
                appNames.add(appName)
                appPackageNames.add(packageName)
            }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, appNames)
        appListView.adapter = adapter
        appListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        // Restore previously selected apps
        for (i in appPackageNames.indices) {
            if (selectedApps.contains(appPackageNames[i])) {
                appListView.setItemChecked(i, true)
            }
        }

        appListView.setOnItemClickListener { _, _, position, _ ->
            val packageName = appPackageNames[position]
            if (selectedApps.contains(packageName)) {
                selectedApps.remove(packageName)
            } else {
                selectedApps.add(packageName)
            }
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
