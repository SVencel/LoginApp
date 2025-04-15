package com.example.login

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AddFriendActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var resultText: TextView
    private lateinit var addFriendButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)

        searchInput = findViewById(R.id.etSearchUsername)
        searchButton = findViewById(R.id.btnSearchUser)
        resultText = findViewById(R.id.tvSearchResult)
        addFriendButton = findViewById(R.id.btnAddFriend)

        searchButton.setOnClickListener {
            val username = searchInput.text.toString().trim()
            if (username.isEmpty()) {
                resultText.text = "❌ Please enter a username"
                addFriendButton.isEnabled = false
            } else {
                resultText.text = "✅ Found: $username"
                addFriendButton.isEnabled = true
            }
        }

        addFriendButton.setOnClickListener {
            Toast.makeText(this, "🎯 Friend request sent!", Toast.LENGTH_SHORT).show()
        }
    }
}
