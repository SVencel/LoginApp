package com.example.login

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var thresholdInput: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        thresholdInput = findViewById(R.id.etCustomThreshold)
        saveButton = findViewById(R.id.btnSaveThreshold)

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

        loadExistingValue()
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
}
