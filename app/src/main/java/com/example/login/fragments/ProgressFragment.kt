package com.example.login.fragments

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.chartViewPager)
        tabLayout = view.findViewById(R.id.chartTabIndicator)
        val tvNotificationCard: TextView = view.findViewById(R.id.tvNotificationCard)

        viewPager.adapter = ChartPagerAdapter(requireContext())

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val label = when (position) {
                0 -> "Streak"
                1 -> "Daily"
                2 -> "Weekly"
                3 -> "Monthly"
                else -> ""
            }
            tab.text = label
            tab.contentDescription = "$label tab"
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val recyclerView = viewPager.getChildAt(0) as? RecyclerView
                val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
                        as? ChartPagerAdapter.ChartViewHolder
                viewPager.postDelayed({ viewHolder?.clearHighlight() }, 50)

                // 🔔 Update the notification count based on tab
                val label = when (position) {
                    1 -> "today"
                    2 -> "this week"
                    3 -> "this month"
                    else -> null
                }

                val fakeCount = when (position) {
                    1 -> 42
                    2 -> 184
                    3 -> 730
                    else -> null
                }

                if (label != null && fakeCount != null) {
                    tvNotificationCard.text = "🔔 Notifications $label: $fakeCount"
                } else {
                    tvNotificationCard.text = ""
                }
            }
        })

        // Set initial
        tvNotificationCard.text = "🔔 Notifications today: 42"
    }

}
