package com.example.login.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
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

        viewPager.adapter = DummyChartPagerAdapter()

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Streak"
                1 -> "Daily"
                2 -> "Weekly"
                3 -> "Monthly"
                else -> ""
            }
        }.attach()
    }

    // ✅ Correctly extended Adapter
    inner class DummyChartPagerAdapter :
        RecyclerView.Adapter<DummyChartPagerAdapter.DummyViewHolder>() {

        inner class DummyViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DummyViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_dummy_chart, parent, false)
            return DummyViewHolder(view)
        }

        override fun onBindViewHolder(holder: DummyViewHolder, position: Int) {
            // Optionally set chart name or sample data here
        }

        override fun getItemCount(): Int = 4
    }
}
