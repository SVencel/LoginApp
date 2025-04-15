package com.example.login.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.login.*


class FriendsFragment : Fragment() {

    private lateinit var friendCountText: TextView
    private lateinit var friendsTitle: TextView
    private lateinit var rvFriends: RecyclerView

    private val sampleFriends = listOf(
        Friend("1", "Alice", 5, "💪 Keep going, Alice!"),
        Friend("2", "Bob", 12, "🔥 You're crushing it!"),
        Friend("3", "Charlie", 8, "👏 Stay strong!"),
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_friends, container, false)

        friendCountText = view.findViewById(R.id.tvFriendCount)
        friendsTitle = view.findViewById(R.id.tvFriendsTitle)
        rvFriends = view.findViewById(R.id.rvFriends)

        rvFriends.layoutManager = LinearLayoutManager(requireContext())
        rvFriends.adapter = FriendsAdapter(sampleFriends) {}

        friendCountText.text = "Friends: ${sampleFriends.size}"

        view.findViewById<Button>(R.id.btnAddNewFriend).setOnClickListener {
            startActivity(Intent(requireContext(), AddFriendActivity::class.java))
        }

        return view
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
