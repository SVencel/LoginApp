package com.example.login

import android.content.Context
import android.graphics.Color
import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

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

        fun bind(position: Int) {
            // Show dummy data based on position
            when (position) {
                0 -> loadDummyChart(chart, "Streak")
                1 -> loadDummyChart(chart, "Daily")
                2 -> loadDummyChart(chart, "Weekly")
                3 -> loadDummyChart(chart, "Monthly")
            }
        }

        fun clearHighlight() {
            chart.highlightValues(null)
        }
    }

    private fun loadDummyChart(chart: BarChart, label: String) {
        val entries = listOf(
            BarEntry(0f, 2f),
            BarEntry(1f, 4f),
            BarEntry(2f, 6f),
            BarEntry(3f, 3f),
            BarEntry(4f, 5f),
        )

        val dataSet = BarDataSet(entries, "$label Data")
        dataSet.color = Color.parseColor("#4CAF50")

        val barData = BarData(dataSet)
        barData.barWidth = 0.9f
        chart.data = barData

        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
        }

        chart.axisLeft.textSize = 12f
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.setFitBars(true)
        chart.setDrawGridBackground(false)
        chart.invalidate()
    }
}
