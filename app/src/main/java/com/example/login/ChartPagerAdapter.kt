package com.example.login

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
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

    override fun getItemCount(): Int = 2

    inner class ChartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val chart: BarChart = view.findViewById(R.id.barChart)

        fun bind(position: Int) {
            if (position == 0) {
                loadStreakChart(chart)
            } else {
                loadAppUsageChart(chart)
            }
        }
    }

    // Fetch streak history from Firestore and populate chart
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
                val barData = BarData(dataSet)
                chart.data = barData
                chart.invalidate()
            }
    }

    // Fetch app usage stats (Mocking it for now, update with real data)
    private fun loadAppUsageChart(chart: BarChart) {
        val entries = mutableListOf<BarEntry>()

        // Example data (Replace with actual app usage data)
        entries.add(BarEntry(0f, 30f)) // App 1
        entries.add(BarEntry(1f, 45f)) // App 2
        entries.add(BarEntry(2f, 60f)) // App 3
        entries.add(BarEntry(3f, 20f)) // App 4
        entries.add(BarEntry(4f, 10f)) // App 5

        val dataSet = BarDataSet(entries, "App Usage (Minutes)")
        val barData = BarData(dataSet)
        chart.data = barData
        chart.invalidate()
    }
}
