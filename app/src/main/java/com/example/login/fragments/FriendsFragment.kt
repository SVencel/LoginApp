package com.example.login.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.login.*
import com.example.login.Friend
import com.example.login.FriendsAdapter
import androidx.appcompat.app.AlertDialog
import com.example.login.FriendRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FriendsFragment : Fragment() {

    private lateinit var friendCountText: TextView
    private lateinit var requestsTitle: TextView
    private lateinit var friendsTitle: TextView
    private lateinit var rvRequests: RecyclerView
    private lateinit var rvFriends: RecyclerView

    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    private val friendList = mutableListOf<Friend>()
    private val requestList = mutableListOf<FriendRequest>()

    private lateinit var friendsAdapter: FriendsAdapter
    private lateinit var requestAdapter: FriendRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_friends, container, false)

        friendCountText = view.findViewById(R.id.tvFriendCount)
        requestsTitle = view.findViewById(R.id.tvFriendRequestsTitle)
        friendsTitle = view.findViewById(R.id.tvFriendsTitle)
        rvRequests = view.findViewById(R.id.rvFriendRequests)
        rvFriends = view.findViewById(R.id.rvFriends)

        rvRequests.layoutManager = LinearLayoutManager(requireContext())
        rvFriends.layoutManager = LinearLayoutManager(requireContext())

        requestAdapter = FriendRequestAdapter(
            requestList,
            onAccept = { senderId -> acceptFriend(senderId) },
            onDecline = { senderId -> declineFriend(senderId) }
        )
        friendsAdapter = FriendsAdapter(
            friendList,
            onCheerClick = { friend -> cheerFriend(friend) },
            onRemoveClick = { friend -> confirmRemoveFriend(friend) }
        )

        rvRequests.adapter = requestAdapter
        rvFriends.adapter = friendsAdapter

        view.findViewById<Button>(R.id.btnAddNewFriend).setOnClickListener {
            startActivity(Intent(requireContext(), AddFriendActivity::class.java))
        }

        loadFriendRequests()
        loadFriends()

        return view
    }

    private fun loadFriendRequests() {
        val user = currentUser ?: return
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val requests = doc.get("friendRequests") as? Map<*, *> ?: emptyMap<String, Boolean>()
                requestList.clear()

                if (requests.isNotEmpty()) {
                    requestsTitle.visibility = View.VISIBLE
                    rvRequests.visibility = View.VISIBLE
                } else {
                    requestsTitle.visibility = View.GONE
                    rvRequests.visibility = View.GONE
                }

                for (senderId in requests.keys) {
                    db.collection("users").document(senderId.toString()).get()
                        .addOnSuccessListener { senderDoc ->
                            val username = senderDoc.getString("username") ?: "Unknown"
                            requestList.add(FriendRequest(senderId.toString(), username))
                            requestAdapter.notifyDataSetChanged()
                        }
                }
            }
    }

    private fun loadFriends() {
        currentUser?.let { user ->
            db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
                val friends = doc.get("friends") as? List<*> ?: return@addOnSuccessListener
                friendList.clear()

                for (friendId in friends) {
                    db.collection("users").document(friendId.toString()).get()
                        .addOnSuccessListener { friendDoc ->
                            val name = friendDoc.getString("username") ?: "Unknown"
                            val streak = friendDoc.getLong("streakCount")?.toInt() ?: 0
                            val cheer = friendDoc.getString("latestCheer") ?: ""

                            friendList.add(Friend(friendId.toString(), name, streak, cheer))
                            friendList.sortByDescending { it.streakCount }
                            friendsAdapter.notifyDataSetChanged()
                            friendCountText.text = "Friends: ${friendList.size}"
                        }
                }
            }
        }
    }

    private fun acceptFriend(senderId: String) {
        val currentUserId = currentUser?.uid ?: return
        val batch = db.batch()
        val currentUserRef = db.collection("users").document(currentUserId)
        val senderRef = db.collection("users").document(senderId)

        batch.update(currentUserRef, "friends", FieldValue.arrayUnion(senderId))
        batch.update(senderRef, "friends", FieldValue.arrayUnion(currentUserId))
        batch.update(currentUserRef, "friendRequests.${senderId}", FieldValue.delete())

        // 👇 Clear UI state BEFORE reload for immediate feedback
        requestList.removeAll { it.userId == senderId }
        requestAdapter.notifyDataSetChanged()

        batch.commit().addOnSuccessListener {
            Toast.makeText(requireContext(), "✅ Friend added!", Toast.LENGTH_SHORT).show()
            loadFriendRequests()
            loadFriends()
        }
    }

    private fun declineFriend(senderId: String) {
        val currentUserId = currentUser?.uid ?: return

        // 👇 Clear UI state BEFORE reload for immediate feedback
        requestList.removeAll { it.userId == senderId }
        requestAdapter.notifyDataSetChanged()

        db.collection("users").document(currentUserId)
            .update("friendRequests.${senderId}", FieldValue.delete())
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "❌ Request declined", Toast.LENGTH_SHORT).show()
                loadFriendRequests()
            }
    }

    private fun confirmRemoveFriend(friend: Friend) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Friend")
            .setMessage("Are you sure you want to remove ${friend.username}?")
            .setPositiveButton("Yes") { _, _ -> removeFriend(friend) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeFriend(friend: Friend) {
        val currentUserId = currentUser?.uid ?: return

        val batch = db.batch()
        val currentUserRef = db.collection("users").document(currentUserId)
        val friendRef = db.collection("users").document(friend.uid)

        batch.update(currentUserRef, "friends", FieldValue.arrayRemove(friend.uid))
        batch.update(friendRef, "friends", FieldValue.arrayRemove(currentUserId))

        batch.commit().addOnSuccessListener {
            Toast.makeText(requireContext(), "❌ Removed ${friend.username}", Toast.LENGTH_SHORT).show()
            loadFriends()
        }
    }


    private fun cheerFriend(friend: Friend) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                val senderUsername = doc.getString("username") ?: "Someone"
                val message = "💪 Keep going, ${friend.username}! From: $senderUsername"

                db.collection("users").document(friend.uid)
                    .update("latestCheer", message)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "🎉 You cheered ${friend.username}", Toast.LENGTH_SHORT).show()
                    }
            }
    }
}
