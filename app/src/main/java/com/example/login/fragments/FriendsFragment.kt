package com.example.login.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.login.AddFriendActivity
import com.example.login.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FriendsAdapter
    private val friendList = mutableListOf<Friend>()
    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private lateinit var friendCountText: TextView



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_friends, container, false)

        recyclerView = view.findViewById(R.id.rvFriends)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = FriendsAdapter(friendList) { friend -> cheerFriend(friend) }
        recyclerView.adapter = adapter

        loadFriends()
        friendCountText = view.findViewById(R.id.tvFriendCount)


        view.findViewById<Button>(R.id.btnAddNewFriend).setOnClickListener {
            startActivity(Intent(requireContext(), AddFriendActivity::class.java))
        }

        return view
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
                            adapter.notifyDataSetChanged()
                            friendCountText.text = "Friends: ${friends.size}"

                        }
                }
            }
        }
    }

    private fun cheerFriend(friend: Friend) {
        val message = "💪 Keep going, ${friend.username}!"
        db.collection("users").document(friend.uid)
            .update("latestCheer", message)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "🎉 You cheered ${friend.username}", Toast.LENGTH_SHORT).show()
            }
    }

    data class Friend(
        val uid: String,
        val username: String,
        val streakCount: Int,
        val latestCheer: String
    )

    class FriendsAdapter(
        private val items: List<Friend>,
        private val onCheerClick: (Friend) -> Unit
    ) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

        inner class FriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tvFriendName)
            val streak: TextView = view.findViewById(R.id.tvStreakCount)
            val cheer: TextView = view.findViewById(R.id.tvCheerMessage)
            val btnCheer: Button = view.findViewById(R.id.btnCheer)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_friend, parent, false)
            return FriendViewHolder(view)
        }

        override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
            val friend = items[position]
            holder.name.text = friend.username
            holder.streak.text = "🔥 Streak: ${friend.streakCount} days"
            holder.cheer.text = friend.latestCheer
            holder.btnCheer.setOnClickListener { onCheerClick(friend) }
        }

        override fun getItemCount(): Int = items.size
    }
}
