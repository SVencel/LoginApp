package com.example.login.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.login.LockScheduleActivity
import com.example.login.R

class FocusFragment : Fragment() {

    private lateinit var toggleOffline: Switch
    private lateinit var btnSchedule: Button
    private lateinit var quoteText: TextView

    private val quotes = listOf(
        "“You can't do big things if you're distracted by small things.”",
        "“Focus is more important than intelligence.”",
        "“Offline is the new luxury.”",
        "“Discipline is choosing between what you want now and what you want most.”"
    )

    private val PREF_KEY = "offlineMode"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_focus, container, false)

        toggleOffline = view.findViewById(R.id.switchGoOffline)
        btnSchedule = view.findViewById(R.id.btnLockScheduler)
        quoteText = view.findViewById(R.id.tvMotivationQuote)

        // Show motivational quote
        quoteText.text = quotes.random()

        // Lock schedule button
        btnSchedule.setOnClickListener {
            startActivity(Intent(requireContext(), LockScheduleActivity::class.java))
        }

        // Load saved offline toggle from SharedPreferences
        val prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isOffline = prefs.getBoolean(PREF_KEY, false)
        toggleOffline.isChecked = isOffline

        toggleOffline.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_KEY, isChecked).apply()
            Toast.makeText(
                requireContext(),
                if (isChecked) "🚫 Offline mode ON" else "✅ You're back online!",
                Toast.LENGTH_SHORT
            ).show()
        }

        return view
    }
}
