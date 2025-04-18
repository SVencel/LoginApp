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
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.util.Locale


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
        private val barChart: BarChart = view.findViewById(R.id.barChart)
        private val lineChart: LineChart = view.findViewById(R.id.lineChart)


        fun bind(position: Int) {
            when (position) {
                0 -> {
                    barChart.visibility = View.VISIBLE
                    lineChart.visibility = View.GONE
                    loadStreakChart(barChart)
                }
                1 -> {
                    barChart.visibility = View.VISIBLE
                    lineChart.visibility = View.GONE
                    loadAppUsageChart(barChart, UsageStatsManager.INTERVAL_DAILY)
                }
                2 -> {
                    barChart.visibility = View.VISIBLE
                    lineChart.visibility = View.GONE
                    loadWeeklyStackedBarChart(barChart)
                }
                3 -> {
                    barChart.visibility = View.GONE
                    lineChart.visibility = View.VISIBLE
                    loadMonthlyLineChart(lineChart)
                }
            }
        }

        fun clearHighlight() {
            if (barChart.visibility == View.VISIBLE) {
                barChart.highlightValues(null)
            }
            if (lineChart.visibility == View.VISIBLE) {
                lineChart.highlightValues(null)
            }
        }

    }

    private fun loadMonthlyLineChart(chart: LineChart) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        val startTime = calendar.timeInMillis

        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

        val categoryMap = mutableMapOf<String, MutableList<Float>>() // Category -> 30 float values

        val categories = listOf("Social", "Productivity", "Game", "Other")
        categories.forEach { categoryMap[it] = MutableList(30) { 0f } }

        for (stat in stats) {
            val dayIndex = (((stat.firstTimeStamp - startTime) / (1000 * 60 * 60 * 24)).toInt()).coerceIn(0, 29)
            val time = stat.totalTimeInForeground
            if (time < 60000) continue

            val category = getCategoryName(stat.packageName)
            val list = categoryMap[category] ?: continue
            list[dayIndex] += time / 60000f // convert to minutes
        }

        val lineDataSets = mutableListOf<ILineDataSet>()

        val colors = mapOf(
            "Social" to Color.parseColor("#FF9800"),
            "Productivity" to Color.parseColor("#2196F3"),
            "Game" to Color.parseColor("#4CAF50"),
            "Other" to Color.GRAY
        )

        for ((category, dataPoints) in categoryMap) {
            val entries = dataPoints.mapIndexed { i, minutes -> Entry(i.toFloat(), minutes) }
            val dataSet = LineDataSet(entries, category).apply {
                color = colors[category] ?: Color.GRAY
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            lineDataSets.add(dataSet)
        }

        val lineData = LineData(lineDataSets)
        chart.data = lineData

        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter((1..30).map { it.toString() })
            granularity = 1f
            position = XAxis.XAxisPosition.BOTTOM
            textSize = 10f
        }

        chart.axisLeft.textSize = 12f
        chart.axisRight.isEnabled = false
        chart.description.text = "App Usage (Minutes) in Past 30 Days"
        chart.setTouchEnabled(true)
        chart.invalidate()
    }

    private fun loadWeeklyStackedBarChart(chart: BarChart) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -6) // Past 7 days including today
        val startTime = calendar.timeInMillis

        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

        // Prepare per-day usage map: dayIndex (0-6) -> category -> minutes
        val dayCategoryUsage = Array(7) { mutableMapOf<String, Float>() }

        for (stat in stats) {
            val time = stat.totalTimeInForeground
            if (time < 60000) continue

            val dayIndex = (((stat.firstTimeStamp - startTime) / (1000 * 60 * 60 * 24)).toInt()).coerceIn(0, 6)
            val category = getCategoryName(stat.packageName)
            val minutes = time / 60000f
            dayCategoryUsage[dayIndex][category] = (dayCategoryUsage[dayIndex][category] ?: 0f) + minutes
        }

        val categories = listOf("Social", "Productivity", "Game", "Other")
        val colors = listOf(
            Color.parseColor("#FF9800"), // Social - Orange
            Color.parseColor("#2196F3"), // Productivity - Blue
            Color.parseColor("#4CAF50"), // Game - Green
            Color.GRAY                    // Other
        )

        val entries = mutableListOf<BarEntry>()
        for (i in 0..6) {
            val categoryValues = categories.map { dayCategoryUsage[i][it] ?: 0f }.toFloatArray()
            entries.add(BarEntry(i.toFloat(), categoryValues))
        }

        val dataSet = BarDataSet(entries, "Weekly App Usage")
        dataSet.colors = colors
        dataSet.setStackLabels(categories.toTypedArray())

        val barData = BarData(dataSet)
        barData.barWidth = 0.8f
        chart.data = barData

        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(getPast7DayLabels())
            granularity = 1f
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            textSize = 10f
        }

        chart.axisLeft.textSize = 12f
        chart.axisRight.isEnabled = false
        chart.description.text = "App Usage (Stacked by Category)"
        chart.setFitBars(true)
        chart.invalidate()
    }

    private fun getPast7DayLabels(): List<String> {
        val format = java.text.SimpleDateFormat("EEE", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        return List(7) {
            val label = format.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            label
        }
    }


    private fun getCategoryName(packageName: String): String {
        val pm = context.packageManager
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val category = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                appInfo.category
            } else null

            when (category) {
                android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                android.content.pm.ApplicationInfo.CATEGORY_GAME -> "Game"
                else -> "Other"
            }
        } catch (_: Exception) {
            "Other"
        }
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
