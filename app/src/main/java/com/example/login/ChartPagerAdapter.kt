package com.example.login

import android.app.usage.UsageStatsManager
import android.graphics.Color
import android.content.Context
import android.icu.util.Calendar
import android.view.LayoutInflater
import android.view.View
import kotlin.math.roundToInt
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChartPagerAdapter(private val context: Context) : RecyclerView.Adapter<ChartPagerAdapter.ChartViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_chart, parent, false)
        return ChartViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChartViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = 4 // 0: Streak, 1: Daily, 2: Weekly, 3: Monthly

    inner class ChartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val chart: BarChart = view.findViewById(R.id.barChart)
        private val notificationText: TextView = view.findViewById(R.id.tvNotificationCount)

        fun bind(position: Int) {
            when (position) {
                0 -> {
                    loadStreakChart(chart)
                    notificationText.text = "" // Streak page doesn't show notifications
                }
                1 -> {
                    loadAppUsageChart(chart, UsageStatsManager.INTERVAL_DAILY)
                    loadNotificationCount(UsageStatsManager.INTERVAL_DAILY, notificationText)
                }
                2 -> {
                    loadAppUsageChart(chart, UsageStatsManager.INTERVAL_WEEKLY)
                    loadNotificationCount(UsageStatsManager.INTERVAL_WEEKLY, notificationText)
                }
                3 -> {
                    loadAppUsageChart(chart, UsageStatsManager.INTERVAL_MONTHLY)
                    loadNotificationCount(UsageStatsManager.INTERVAL_MONTHLY, notificationText)
                }
            }
        }


        fun clearHighlight() {
            chart.highlightValues(null)  // 💡 This removes the marker tooltip
        }
    }

    private fun loadNotificationCount(intervalType: Int, textView: TextView) {
        // Fake count for now — later replace with actual usage stats if available
        val calendar = Calendar.getInstance()
        val label = when (intervalType) {
            UsageStatsManager.INTERVAL_DAILY -> "today"
            UsageStatsManager.INTERVAL_WEEKLY -> "this week"
            UsageStatsManager.INTERVAL_MONTHLY -> "this month"
            else -> ""
        }

        // Placeholder value (replace with real NotificationListener data when ready)
        val mockCount = when (intervalType) {
            UsageStatsManager.INTERVAL_DAILY -> 42
            UsageStatsManager.INTERVAL_WEEKLY -> 184
            UsageStatsManager.INTERVAL_MONTHLY -> 730
            else -> 0
        }

        textView.text = "🔔 Notifications $label: $mockCount"
    }


    private fun loadStreakChart(chart: BarChart) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid).collection("streakHistory")
            .get()
            .addOnSuccessListener { documents ->
                val entries = ArrayList<BarEntry>()
                var index = 0
                for (document in documents) {
                    val streakValue = document.getLong("streak")?.toInt() ?: 0
                    entries.add(BarEntry(index.toFloat(), streakValue.toFloat()))
                    index++
                }

                val dataSet = BarDataSet(entries, "Streak Progress")
                dataSet.color = Color.parseColor("#4CAF50")
                val barData = BarData(dataSet)
                chart.data = barData
                chart.description.isEnabled = false
                chart.invalidate()
            }
    }

    private fun loadAppUsageChart(chart: BarChart, intervalType: Int) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        when (intervalType) {
            UsageStatsManager.INTERVAL_DAILY -> calendar.add(Calendar.DAY_OF_YEAR, -1)
            UsageStatsManager.INTERVAL_WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, -1)
            UsageStatsManager.INTERVAL_MONTHLY -> calendar.add(Calendar.MONTH, -1)
        }
        val startTime = calendar.timeInMillis
        chart.setDrawMarkers(true)



        val stats = usageStatsManager.queryUsageStats(intervalType, startTime, endTime)
        val usageMap = mutableMapOf<String, Long>()

        for (usageStat in stats) {
            val totalTime = usageStat.totalTimeInForeground
            if (totalTime >= 60000) { // Only show apps used more than 1 minute
                usageMap[usageStat.packageName] = (usageMap[usageStat.packageName] ?: 0) + totalTime
            }
        }

        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        val colors = mutableListOf<Int>()
        var index = 0
        val packageManager = context.packageManager

        for ((packageName, time) in usageMap.toList().sortedByDescending { it.second }.take(10)) {
            val label = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
            } catch (e: Exception) {
                packageName
            }

            val minutes = time / 60000f
            entries.add(BarEntry(index.toFloat(), minutes))
            labels.add(label)
            colors.add(getColorForAppCategory(packageName))
            index++
        }

        val dataSet = BarDataSet(entries, "App Usage (Minutes)")
        dataSet.colors = colors
        val barData = BarData(dataSet)
        barData.barWidth = 0.9f
        chart.data = barData

        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            granularity = 1f
            isGranularityEnabled = true
            labelRotationAngle = -30f
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            textSize = 10f
        }

        chart.axisLeft.textSize = 12f
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.setFitBars(true)
        chart.setTouchEnabled(true)
        chart.setDrawGridBackground(false)
        chart.invalidate()

        val markerView = UsageMarkerView(context, labels)
        markerView.chartView = chart
        chart.marker = markerView
    }

    private fun getColorForAppCategory(packageName: String): Int {
        val pm = context.packageManager
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val category = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                appInfo.category
            } else null

            when (category) {
                android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> Color.parseColor("#FF9800") // Orange
                android.content.pm.ApplicationInfo.CATEGORY_GAME -> Color.parseColor("#4CAF50") // Green
                android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> Color.parseColor("#2196F3") // Blue
                else -> Color.GRAY
            }
        } catch (e: Exception) {
            Color.GRAY
        }
    }

    class UsageMarkerView(context: Context, private val labels: List<String>) :
        MarkerView(context, R.layout.marker_view) {

        private val markerText: TextView = findViewById(R.id.markerText)

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            if (e != null && e.x >= 0 && e.x < labels.size) {
                val index = e.x.roundToInt().coerceIn(0, labels.size - 1)
                val label = labels[index]
                val minutes = e.y.toInt()
                markerText.text = "$label\n$minutes min"
            } else {
                markerText.text = "N/A"
            }
            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF {
            return MPPointF(-(width / 2f), -height.toFloat())
        }
    }


}
