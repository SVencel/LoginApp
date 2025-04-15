package com.example.login

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class FriendRequestAdapter(
    private val requests: List<FriendRequest>,
    private val onAccept: (String) -> Unit = {},
    private val onDecline: (String) -> Unit = {}
) : RecyclerView.Adapter<FriendRequestAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUsername: TextView = view.findViewById(R.id.tvRequesterUsername)
        val btnAccept: Button = view.findViewById(R.id.btnAccept)
        val btnDecline: Button = view.findViewById(R.id.btnDecline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = requests.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        holder.tvUsername.text = request.username
        holder.btnAccept.setOnClickListener {
            // Placeholder for design preview
        }
        holder.btnDecline.setOnClickListener {
            // Placeholder for design preview
        }
    }
}
