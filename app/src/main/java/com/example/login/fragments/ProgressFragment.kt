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
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore
    import java.text.SimpleDateFormat
    import java.util.Date
    import java.util.Locale

    class ProgressFragment : Fragment() {

        private lateinit var viewPager: ViewPager2
        private lateinit var tabLayout: TabLayout
        private lateinit var tvNotificationCard: TextView
        private lateinit var tvPhoneOpenCount: TextView
        private var lastLongestStreak: Int? = null
        private var lastProductivityScore: Int? = null


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
                    0 -> "Daily"
                    1 -> "Weekly"
                    2 -> "Monthly"
                    else -> ""
                }
            }.attach()

            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateStatsUI(position)
                }
            })

            viewPager.setCurrentItem(0, false) // No animation
            updateStatsUI(0)
        }



        private fun getTodayDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
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
                }

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
