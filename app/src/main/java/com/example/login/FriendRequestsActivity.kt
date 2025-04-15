package com.example.login

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FriendRequestsActivity : AppCompatActivity() {

    private lateinit var rvRequests: RecyclerView
    private lateinit var adapter: FriendRequestAdapter
    private val requestsList = mutableListOf<FriendRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_requests)

        rvRequests = findViewById(R.id.rvFriendRequests)
        rvRequests.layoutManager = LinearLayoutManager(this)

        // Sample mock data for design display
        requestsList.add(FriendRequest("1", "Alice"))
        requestsList.add(FriendRequest("2", "Bob"))
        requestsList.add(FriendRequest("3", "Charlie"))

        adapter = FriendRequestAdapter(requestsList)
        rvRequests.adapter = adapter
    }

    data class FriendRequest(
        val userId: String,
        val username: String
    )

}
