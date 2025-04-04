package com.example.login.fragments

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.login.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var tvStreakCount: TextView
    private lateinit var tvSummary: TextView
    private lateinit var tvQuote: TextView

    private val quotes = listOf(
        "“Small steps every day.”",
        "“Progress over perfection.”",
        "“Stay focused, stay sharp.”",
        "“One day at a time.”",
        "“Your future self will thank you.”"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        tvStreakCount = view.findViewById(R.id.tvStreak)
        tvSummary = view.findViewById(R.id.tvSummary)
        tvQuote = view.findViewById(R.id.tvQuote)

        tvQuote.text = quotes.random()
        fetchStreak()

        return view
    }

    private fun fetchStreak() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener {
                val streak = it.getLong("streakCount")?.toInt() ?: 0
                tvStreakCount.text = "🔥 $streak-day streak"
                tvSummary.text = when {
                    streak == 0 -> "Let’s get started today!"
                    streak < 3 -> "Nice work! Keep it up!"
                    streak < 7 -> "Great focus this week!"
                    else -> "You're on fire! 🔥"
                }
            }
    }
}
