package com.example.login.fragments

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.login.R

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

        // Static sample data
        tvStreakCount.text = "🔥 4-day streak"
        tvSummary.text = "You're doing great! Keep up the focus!"
        tvQuote.text = quotes.random()

        return view
    }
}
