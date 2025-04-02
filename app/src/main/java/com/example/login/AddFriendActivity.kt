package com.example.login

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddFriendActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var resultText: TextView
    private lateinit var addFriendButton: Button

    private var foundUserId: String? = null // Will store userId of found friend

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        searchInput = findViewById(R.id.etSearchUsername)
        searchButton = findViewById(R.id.btnSearchUser)
        resultText = findViewById(R.id.tvSearchResult)
        addFriendButton = findViewById(R.id.btnAddFriend)

        searchButton.setOnClickListener {
            val username = searchInput.text.toString().trim()
            searchForUser(username)
        }

        addFriendButton.setOnClickListener {
            foundUserId?.let { friendId ->
                sendFriendRequest(friendId)
            }
        }
    }

    private fun searchForUser(username: String) {
        if (username.isEmpty()) {
            Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    resultText.text = "❌ No user found"
                    foundUserId = null
                    addFriendButton.isEnabled = false
                } else {
                    val doc = docs.documents.first()
                    val userId = doc.id
                    val foundUsername = doc.getString("username") ?: "User"
                    resultText.text = "✅ Found: $foundUsername"
                    foundUserId = userId
                    addFriendButton.isEnabled = true
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error searching user", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendFriendRequest(friendId: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        if (friendId == currentUserId) {
            Toast.makeText(this, "You cannot add yourself", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = db.collection("users").document(currentUserId)

        userRef.update("friends", com.google.firebase.firestore.FieldValue.arrayUnion(friendId))
            .addOnSuccessListener {
                Toast.makeText(this, "🎉 Friend added!", Toast.LENGTH_SHORT).show()
                finish() // Close activity
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Failed to add friend", Toast.LENGTH_SHORT).show()
            }
    }
}
