package com.example.login

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AppBlockSelectionActivity : AppCompatActivity() {

    private lateinit var appListView: ListView
    private lateinit var saveButton: Button

    private val mockApps = listOf("Instagram", "YouTube", "Reddit", "Twitter", "TikTok")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_block_selection)

        appListView = findViewById(R.id.appListView)
        saveButton = findViewById(R.id.btnSaveBlockedApps)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, mockApps)
        appListView.adapter = adapter
        appListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        saveButton.setOnClickListener {
            Toast.makeText(this, "✅ Blocked apps updated!", Toast.LENGTH_SHORT).show()
        }
    }
}
