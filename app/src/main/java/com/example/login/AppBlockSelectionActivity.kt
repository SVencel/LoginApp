package com.example.login

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AppBlockSelectionActivity : AppCompatActivity() {

    private lateinit var appListView: ListView
    private lateinit var saveButton: Button
    private val selectedApps = mutableSetOf<String>()  // Track selected apps

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_block_selection)

        appListView = findViewById(R.id.appListView)
        saveButton = findViewById(R.id.btnSaveBlockedApps)

        loadInstalledApps()
        loadPreviouslySelectedApps()

        saveButton.setOnClickListener {
            saveBlockedApps()
        }
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

    private fun loadPreviouslySelectedApps() {
        val sharedPref = getSharedPreferences("LockSchedulePrefs", Context.MODE_PRIVATE)
        selectedApps.addAll(sharedPref.getStringSet("blockedApps", setOf()) ?: setOf())
    }

    private fun saveBlockedApps() {
        val sharedPref = getSharedPreferences("LockSchedulePrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putStringSet("blockedApps", selectedApps)
            apply()
        }
        Toast.makeText(this, "Blocked apps updated!", Toast.LENGTH_SHORT).show()
        finish() // Close activity after saving
    }
}
