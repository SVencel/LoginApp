package com.example.login

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FriendRequestsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvRequests: RecyclerView
    private lateinit var adapter: FriendRequestAdapter
    private val requestsList = mutableListOf<FriendRequest>()

    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_requests)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: return

        rvRequests = findViewById(R.id.rvFriendRequests)
        rvRequests.layoutManager = LinearLayoutManager(this)
        adapter = FriendRequestAdapter(requestsList,
            onAccept = { senderId -> acceptFriend(senderId) },
            onDecline = { senderId -> declineFriend(senderId) })
        rvRequests.adapter = adapter

        loadFriendRequests()
    }

    private fun loadFriendRequests() {
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { doc ->
                val requests = doc.get("friendRequests") as? Map<*, *> ?: emptyMap<String, Boolean>()
                requestsList.clear()

                if (requests.isEmpty()) {
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                for (senderId in requests.keys) {
                    db.collection("users").document(senderId.toString()).get()
                        .addOnSuccessListener { senderDoc ->
                            val username = senderDoc.getString("username") ?: "Unknown"
                            requestsList.add(FriendRequest(senderId.toString(), username))
                            adapter.notifyDataSetChanged()
                        }
                }
            }
    }

    private fun acceptFriend(senderId: String) {
        val batch = db.batch()
        val currentUserRef = db.collection("users").document(currentUserId)
        val senderRef = db.collection("users").document(senderId)

        batch.update(currentUserRef, "friends", FieldValue.arrayUnion(senderId))
        batch.update(senderRef, "friends", FieldValue.arrayUnion(currentUserId))
        batch.update(currentUserRef, "friendRequests.${senderId}", FieldValue.delete())

        batch.commit().addOnSuccessListener {
            Toast.makeText(this, "✅ Friend added!", Toast.LENGTH_SHORT).show()
            loadFriendRequests()
        }
    }

    private fun declineFriend(senderId: String) {
        val currentUserRef = db.collection("users").document(currentUserId)
        currentUserRef.update("friendRequests.${senderId}", FieldValue.delete())
            .addOnSuccessListener {
                Toast.makeText(this, "❌ Request declined", Toast.LENGTH_SHORT).show()
                loadFriendRequests()
            }
    }
}

data class FriendRequest(
    val userId: String,
    val username: String
)
