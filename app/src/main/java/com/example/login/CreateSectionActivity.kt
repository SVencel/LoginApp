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
    private val selectedDays = mutableSetOf<Int>()

    private var startHour = 8
    private var startMinute = 0
    private var endHour = 19
    private var endMinute = 0

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

        loadDummyApps()
        setupSearchBar()

        startTimeButton.setOnClickListener { pickTime(true) }
        endTimeButton.setOnClickListener { pickTime(false) }
        saveButton.setOnClickListener {
            Toast.makeText(this, "✅ Save clicked (no backend in design mode)", Toast.LENGTH_SHORT).show()
        }
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

    private fun loadDummyApps() {
        appList.clear()
        appList.addAll(
            listOf(
                AppInfo("Instagram", "com.instagram.android", 5000, isTopUsed = true),
                AppInfo("YouTube", "com.google.android.youtube", 4000, isTopUsed = true),
                AppInfo("TikTok", "com.tiktok.android", 3000),
                AppInfo("Gmail", "com.google.android.gm", 2000),
                AppInfo("Reddit", "com.reddit.frontpage", 1000)
            )
        )
        filteredAppList.clear()
        filteredAppList.addAll(appList)
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
}
