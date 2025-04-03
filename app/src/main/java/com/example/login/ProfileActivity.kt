package com.example.login

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent


class ProfileActivity : AppCompatActivity() {

    private lateinit var thresholdInput: EditText
    private lateinit var saveButton: Button
    private lateinit var friendCountText: TextView
    private lateinit var addFriendButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        thresholdInput = findViewById(R.id.etCustomThreshold)
        saveButton = findViewById(R.id.btnSaveThreshold)

        friendCountText = findViewById(R.id.tvFriendCount)
        addFriendButton = findViewById(R.id.btnAddFriends)

        addFriendButton.setOnClickListener {
            startActivity(Intent(this, AddFriendActivity::class.java))
        }


        saveButton.setOnClickListener {
            val inputText = thresholdInput.text.toString()
            val minutes = inputText.toIntOrNull()

            if (minutes == null || minutes < 1) {
                Toast.makeText(this, "Enter a number ≥ 5", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = FirebaseAuth.getInstance().currentUser ?: return@setOnClickListener
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid)
                .update("productivityPromptMinutes", minutes)
                .addOnSuccessListener {
                    Toast.makeText(this, "Saved! You’ll be reminded every $minutes minutes", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save threshold", Toast.LENGTH_SHORT).show()
                }
        }
        val sectionButton: Button = findViewById(R.id.btnManageSections)
        sectionButton.setOnClickListener {
            startActivity(Intent(this, CreateSectionActivity::class.java))
        }


        loadExistingValue()
        loadFriendCount()
    }

    private fun loadExistingValue() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val value = document.getLong("productivityPromptMinutes")?.toInt()
                if (value != null) {
                    thresholdInput.setText(value.toString())
                }
            }
    }
    private fun loadFriendCount() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val friends = document.get("friends") as? List<*>
                val count = friends?.size ?: 0
                friendCountText.text = "Friends: $count"
            }
    }

}
