package com.example.login.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.login.CreateSectionActivity
import com.example.login.R
import com.example.login.adapters.FocusSectionAdapter

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

        loadLocalSampleSections(view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadLocalSampleSections(view)
    }


    private fun loadLocalSampleSections(view: View) {
        val rv = view?.findViewById<RecyclerView>(R.id.rvSections) ?: return

        val sections = listOf(
            FocusSection(
                name = "Morning Focus",
                apps = listOf("com.instagram.android", "com.tiktok.android"),
                startHour = 7, startMinute = 0,
                endHour = 9, endMinute = 30,
                days = listOf(1, 2, 3, 4, 5)
            ),
            FocusSection(
                name = "Study Session",
                apps = listOf("com.twitter.android", "com.snapchat.android"),
                startHour = 18, startMinute = 0,
                endHour = 20, endMinute = 0,
                days = listOf(1, 2, 3, 4)
            ),
            FocusSection(
                name = "Deep Work",
                apps = listOf("com.facebook.katana", "com.reddit.frontpage"),
                startHour = 10, startMinute = 0,
                endHour = 12, endMinute = 0,
                days = listOf(1, 3, 5)
            ),
            FocusSection(
                name = "Evening Detox",
                apps = listOf("com.tiktok.android"),
                startHour = 21, startMinute = 0,
                endHour = 23, endMinute = 0,
                days = listOf(7)
            )
        )

        rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        rv.adapter = FocusSectionAdapter(requireContext(), sections)
    }


    data class FocusSection(
        val name: String,
        val apps: List<String>,
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int,
        val days: List<Int>
    )

}
