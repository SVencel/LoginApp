package com.example.login

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CreateSectionActivity : AppCompatActivity() {

    private lateinit var sectionNameInput: EditText
    private lateinit var startTimeButton: Button
    private lateinit var endTimeButton: Button
    private lateinit var saveButton: Button
    private lateinit var appRecyclerView: RecyclerView

    private val selectedApps = mutableSetOf<String>()
    private var startHour = 19
    private var startMinute = 0
    private var endHour = 8
    private var endMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_section)

        sectionNameInput = findViewById(R.id.etSectionName)
        startTimeButton = findViewById(R.id.btnStartTime)
        endTimeButton = findViewById(R.id.btnEndTime)
        saveButton = findViewById(R.id.btnSaveSection)
        appRecyclerView = findViewById(R.id.recyclerViewApps)

        appRecyclerView.layoutManager = LinearLayoutManager(this)

        loadInstalledApps()

        startTimeButton.setOnClickListener { pickTime(true) }
        endTimeButton.setOnClickListener { pickTime(false) }

        saveButton.setOnClickListener { saveSectionToFirebase() }
    }

    private fun loadInstalledApps() {
        val packageManager = packageManager
        val installedApps = packageManager.getInstalledApplications(0)
        val appList = installedApps
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .map {
                AppInfo(
                    it.loadLabel(packageManager).toString(),
                    it.packageName
                )
            }

        appRecyclerView.adapter = AppListAdapter(appList, selectedApps)
    }

    private fun pickTime(isStart: Boolean) {
        val initialHour = if (isStart) startHour else endHour
        val initialMinute = if (isStart) startMinute else endMinute

        val picker = TimePickerDialog(this, { _, hourOfDay, minute ->
            if (isStart) {
                startHour = hourOfDay
                startMinute = minute
                startTimeButton.text = String.format("Start: %02d:%02d", hourOfDay, minute)
            } else {
                endHour = hourOfDay
                endMinute = minute
                endTimeButton.text = String.format("End: %02d:%02d", hourOfDay, minute)
            }
        }, initialHour, initialMinute, true)

        picker.show()
    }

    private fun saveSectionToFirebase() {
        val name = sectionNameInput.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter a name for the section", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedApps.isEmpty()) {
            Toast.makeText(this, "Select at least one app", Toast.LENGTH_SHORT).show()
            return
        }

        val section = Section(
            name,
            selectedApps.toList(),
            startHour,
            startMinute,
            endHour,
            endMinute
        )

        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid)
            .collection("sections")
            .add(section)
            .addOnSuccessListener {
                Toast.makeText(this, "Section saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save section", Toast.LENGTH_SHORT).show()
            }
    }
}
