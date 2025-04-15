package com.example.login.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.login.CreateSectionActivity
import com.example.login.R

class FocusFragment : Fragment() {

    private lateinit var btnManageSections: Button
    private lateinit var quoteText: TextView

    private val quotes = listOf(
        "“You can't do big things if you're distracted by small things.”",
        "“Focus is more important than intelligence.”",
        "“Offline is the new luxury.”",
        "“Discipline is choosing between what you want now and what you want most.”",
        "“STAY HARD”"
    )


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_focus, container, false)

        quoteText = view.findViewById(R.id.tvMotivationQuote)
        btnManageSections = view.findViewById(R.id.btnManageSections)

        quoteText.text = quotes.random()

        btnManageSections.setOnClickListener {
            startActivity(Intent(requireContext(), CreateSectionActivity::class.java))
        }

        return view
    }
}
