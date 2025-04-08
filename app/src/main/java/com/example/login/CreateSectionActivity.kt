package com.example.login

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CreateSectionActivity : AppCompatActivity() {

    private lateinit var sectionNameInput: EditText
    private lateinit var startTimeButton: Button
    private lateinit var endTimeButton: Button
    private lateinit var saveButton: Button
    private lateinit var searchAppInput: EditText
    private lateinit var appRecyclerView: RecyclerView
    private lateinit var appAdapter: AppListAdapter

    private val selectedApps = mutableSetOf<String>()
    private val appList = mutableListOf<AppInfo>()
    private val filteredAppList = mutableListOf<AppInfo>()

    private lateinit var dayToggles: List<ToggleButton>
    private val selectedDays = mutableSetOf<Int>() // 1 = Mon ... 7 = Sun

    private var startHour = 8
    private var startMinute = 0
    private var endHour = 19
    private var endMinute = 0

    private var isEditMode = false
    private var originalSectionName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_section)

        sectionNameInput = findViewById(R.id.etSectionName)
        startTimeButton = findViewById(R.id.btnStartTime)
        endTimeButton = findViewById(R.id.btnEndTime)
        saveButton = findViewById(R.id.btnSaveSection)
        searchAppInput = findViewById(R.id.searchAppInput)
        appRecyclerView = findViewById(R.id.recyclerViewApps)

        appRecyclerView.layoutManager = LinearLayoutManager(this)
        appAdapter = AppListAdapter(filteredAppList, selectedApps)
        appRecyclerView.adapter = appAdapter

        dayToggles = listOf(
            findViewById(R.id.toggleMon),
            findViewById(R.id.toggleTue),
            findViewById(R.id.toggleWed),
            findViewById(R.id.toggleThu),
            findViewById(R.id.toggleFri),
            findViewById(R.id.toggleSat),
            findViewById(R.id.toggleSun)
        )

        dayToggles.forEachIndexed { index, toggle ->
            toggle.setOnCheckedChangeListener { _, isChecked ->
                val day = index + 1
                if (isChecked) selectedDays.add(day) else selectedDays.remove(day)
            }
        }

        loadInstalledApps()

        if (intent.hasExtra("sectionName")) {
            isEditMode = true
            originalSectionName = intent.getStringExtra("sectionName")
            sectionNameInput.setText(originalSectionName)
            saveButton.text = "Update Section"

            sectionNameInput.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    hideKeyboard(sectionNameInput)
                    sectionNameInput.clearFocus()
                    return@setOnEditorActionListener true
                }
                false
            }

            val fromTime = intent.getStringExtra("fromTime") ?: "00:00"
            val toTime = intent.getStringExtra("toTime") ?: "00:00"
            val fromParts = fromTime.split(":")
            val toParts = toTime.split(":")

            startHour = fromParts[0].toInt()
            startMinute = fromParts[1].toInt()
            endHour = toParts[0].toInt()
            endMinute = toParts[1].toInt()

            startTimeButton.text = "Start: $fromTime"
            endTimeButton.text = "End: $toTime"

            selectedApps.addAll(intent.getStringArrayExtra("apps")?.toList() ?: emptyList())

            val selectedDayInts = intent.getIntegerArrayListExtra("days") ?: arrayListOf()
            selectedDays.clear()
            selectedDays.addAll(selectedDayInts)

            selectedDayInts.forEach { day ->
                if (day in 1..7) {
                    dayToggles[day - 1].isChecked = true
                }
            }

        }

        startTimeButton.setOnClickListener { pickTime(true) }
        endTimeButton.setOnClickListener { pickTime(false) }
        saveButton.setOnClickListener { saveSectionToFirebase() }

        setupSearchBar()
    }

    private fun setupSearchBar() {
        searchAppInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterApps(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        searchAppInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                hideKeyboard(searchAppInput)
                searchAppInput.clearFocus()
                true
            } else false
        }
    }

    private fun hideKeyboard(view: EditText) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun loadInstalledApps() {
        val packageManager = packageManager
        val installedApps = packageManager.getInstalledApplications(0)

        // Step 1: Gather usage data
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 7 * 24 * 60 * 60 * 1000L // last 7 days

        val usageStats = usageStatsManager.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val usageMap = usageStats
            .filter { it.totalTimeInForeground > 0 }
            .associateBy({ it.packageName }, { it.totalTimeInForeground })

        // Step 2: Build the list with usage info
        appList.clear()
        installedApps.forEach { app ->
            val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                val label = packageManager.getApplicationLabel(app).toString()
                val usage = usageMap[app.packageName] ?: 0L
                appList.add(AppInfo(label, app.packageName, usage))
            }
        }

        // Step 3: Sort by usage (desc) then name (asc)
        val topUsed = appList.sortedByDescending { it.usageTime }.take(5).map { it.copy(isTopUsed = true) }
        val rest = appList.minus(topUsed.toSet()).sortedBy { it.name.lowercase(Locale.getDefault()) }

        filteredAppList.clear()
        filteredAppList.addAll(topUsed + rest)

        appAdapter.notifyDataSetChanged()
    }


    private fun filterApps(query: String) {
        filteredAppList.clear()
        if (query.isEmpty()) {
            filteredAppList.addAll(appList)
        } else {
            val lowerQuery = query.lowercase(Locale.getDefault())
            filteredAppList.addAll(appList.filter {
                it.name.lowercase(Locale.getDefault()).contains(lowerQuery)
            })
        }
        appAdapter.notifyDataSetChanged()
    }

    private fun pickTime(isStart: Boolean) {
        val hour = if (isStart) startHour else endHour
        val minute = if (isStart) startMinute else endMinute

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            if (isStart) {
                startHour = selectedHour
                startMinute = selectedMinute
                startTimeButton.text = "Start: %02d:%02d".format(startHour, startMinute)
            } else {
                endHour = selectedHour
                endMinute = selectedMinute
                endTimeButton.text = "End: %02d:%02d".format(endHour, endMinute)
            }
        }, hour, minute, true).show()
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
        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show()
            return
        }

        // ⏱ Time validation
        val startTotal = startHour * 60 + startMinute
        val endTotal = endHour * 60 + endMinute
        if (endTotal <= startTotal) {
            Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show()
            return
        }

        val section = mapOf(
            "name" to name,
            "apps" to selectedApps.toList(),
            "startHour" to startHour,
            "startMinute" to startMinute,
            "endHour" to endHour,
            "endMinute" to endMinute,
            "days" to selectedDays.toList()
        )

        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("users").document(user.uid).collection("sections")

        if (isEditMode) {
            saveOrUpdateSection(ref, name, section, true)
        } else {
            ref.document(name).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    Toast.makeText(this, "A section with this name already exists", Toast.LENGTH_SHORT).show()
                } else {
                    saveOrUpdateSection(ref, name, section, false)
                }
            }
        }
    }

    private fun saveOrUpdateSection(
        ref: CollectionReference,
        name: String,
        section: Map<String, Any>,
        isEditMode: Boolean
    ) {
        if (isEditMode && originalSectionName != null) {
            ref.document(originalSectionName!!).delete().addOnSuccessListener {
                ref.document(name).set(section).addOnSuccessListener {
                    Toast.makeText(this, "Section updated", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to update section", Toast.LENGTH_SHORT).show()
            }
        } else {
            ref.document(name).set(section).addOnSuccessListener {
                Toast.makeText(this, "Section saved!", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to save section", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
