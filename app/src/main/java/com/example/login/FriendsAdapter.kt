package com.example.login

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Friend(
    val uid: String,
    val username: String,
    val streakCount: Int,
    val latestCheer: String
)
class FriendsAdapter(
    private val items: List<Friend>,
    private val onCheerClick: (Friend) -> Unit,
    private val onRemoveClick: (Friend) -> Unit
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    inner class FriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvFriendName)
        val streak: TextView = view.findViewById(R.id.tvStreakCount)
        val cheer: TextView = view.findViewById(R.id.tvCheerMessage)
        val btnCheer: Button = view.findViewById(R.id.btnCheer)
        val btnRemove: ImageButton = view.findViewById(R.id.btnRemove)
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
        holder.btnRemove.setOnClickListener { onRemoveClick(friend) }
    }

    override fun getItemCount(): Int = items.size
}
