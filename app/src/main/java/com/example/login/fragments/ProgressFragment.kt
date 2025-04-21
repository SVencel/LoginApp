package com.example.login.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.login.ChartPagerAdapter
import com.example.login.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class ProgressFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var tvNotificationCard: TextView
    private lateinit var tvPhoneOpenCount: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.chartViewPager)
        tabLayout = view.findViewById(R.id.chartTabIndicator)
        tvNotificationCard = view.findViewById(R.id.tvNotificationCard)
        tvPhoneOpenCount = view.findViewById(R.id.tvPhoneOpenCount)

        viewPager.adapter = ChartPagerAdapter(requireContext())

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Streak"
                1 -> "Daily"
                2 -> "Weekly"
                3 -> "Monthly"
                else -> ""
            }
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateStatsUI(position)
            }
        })

        // Set initial display
        updateStatsUI(1)
    }

    private fun updateStatsUI(position: Int) {
        val label = when (position) {
            1 -> "today"
            2 -> "this week"
            3 -> "this month"
            else -> null
        }

        val notifCount = when (position) {
            1 -> getNotificationCountForDays(requireContext(), 1)
            2 -> getNotificationCountForDays(requireContext(), 7)
            3 -> getNotificationCountForDays(requireContext(), 30)
            else -> null
        }

        val phoneOpenCount = when (position) {
            1 -> getPhoneOpensForDays(requireContext(), 1)
            2 -> getPhoneOpensForDays(requireContext(), 7)
            3 -> getPhoneOpensForDays(requireContext(), 30)
            else -> null
        }


        if (label != null && notifCount != null && phoneOpenCount != null) {
            tvNotificationCard.text = "🔔 Notifications $label: $notifCount"
            tvPhoneOpenCount.text = "📱 Phone Opened $label: $phoneOpenCount"
        } else {
            tvNotificationCard.text = ""
            tvPhoneOpenCount.text = ""
        }
    }


    private fun getTodayNotificationCount(context: Context): Int {
        val prefs = context.getSharedPreferences("monitorPrefs", Context.MODE_PRIVATE)
        val today = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        val key = "notifCount_$today"
        return prefs.getInt(key, 0)
    }

    private fun getNotificationCountForDays(context: Context, daysBack: Int): Int {
        val prefs = context.getSharedPreferences("monitorPrefs", Context.MODE_PRIVATE)
        val today = System.currentTimeMillis() / (1000 * 60 * 60 * 24)

        var total = 0
        for (i in 0 until daysBack) {
            val dayKey = "notifCount_${today - i}"
            total += prefs.getInt(dayKey, 0)
        }
        return total
    }


    private fun getPhoneOpensForDays(context: Context, daysBack: Int): Int {
        val prefs = context.getSharedPreferences("monitorPrefs", Context.MODE_PRIVATE)
        val today = System.currentTimeMillis() / (1000 * 60 * 60 * 24)

        var total = 0
        for (i in 0 until daysBack) {
            val key = "phoneOpens_${today - i}"
            total += prefs.getInt(key, 0)
        }
        return total
    }

}
