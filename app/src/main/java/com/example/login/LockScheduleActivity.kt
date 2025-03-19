package com.example.login

import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class LockScheduleActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var btnSetStartTime: Button
    private lateinit var btnSetEndTime: Button
    private lateinit var btnSaveSchedule: Button
    private lateinit var tvStartTime: TextView
    private lateinit var tvEndTime: TextView

    private var startHour = 20
    private var startMinute = 0
    private var endHour = 8
    private var endMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_schedule)

        sharedPref = getSharedPreferences("LockSchedulePrefs", Context.MODE_PRIVATE)

        btnSetStartTime = findViewById(R.id.btnSetStartTime)
        btnSetEndTime = findViewById(R.id.btnSetEndTime)
        btnSaveSchedule = findViewById(R.id.btnSaveSchedule)
        tvStartTime = findViewById(R.id.tvStartTime)
        tvEndTime = findViewById(R.id.tvEndTime)

        loadSavedSchedule()

        btnSetStartTime.setOnClickListener {
            showTimePicker(true)
        }

        btnSetEndTime.setOnClickListener {
            showTimePicker(false)
        }

        btnSaveSchedule.setOnClickListener {
            saveSchedule()
        }
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val cal = Calendar.getInstance()
        val hour = if (isStartTime) startHour else endHour
        val minute = if (isStartTime) startMinute else endMinute

        val timePicker = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            if (isStartTime) {
                startHour = selectedHour
                startMinute = selectedMinute
                tvStartTime.text = "Start Time: $startHour:$startMinute"
            } else {
                endHour = selectedHour
                endMinute = selectedMinute
                tvEndTime.text = "End Time: $endHour:$endMinute"
            }
        }, hour, minute, true)

        timePicker.show()
    }

    private fun saveSchedule() {
        val editor = sharedPref.edit()
        editor.putInt("startHour", startHour)
        editor.putInt("startMinute", startMinute)
        editor.putInt("endHour", endHour)
        editor.putInt("endMinute", endMinute)
        editor.apply()
        Toast.makeText(this, "Lock schedule saved!", Toast.LENGTH_SHORT).show()
        finish() // Close the activity
    }

    private fun loadSavedSchedule() {
        startHour = sharedPref.getInt("startHour", 20)
        startMinute = sharedPref.getInt("startMinute", 0)
        endHour = sharedPref.getInt("endHour", 8)
        endMinute = sharedPref.getInt("endMinute", 0)

        tvStartTime.text = "Start Time: $startHour:$startMinute"
        tvEndTime.text = "End Time: $endHour:$endMinute"
    }
}
