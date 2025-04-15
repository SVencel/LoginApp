package com.example.login.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.login.R
import com.example.login.fragments.FocusFragment.FocusSection

class FocusSectionAdapter(
    private val context: Context,
    private val sections: List<FocusSection>
) : RecyclerView.Adapter<FocusSectionAdapter.SectionViewHolder>() {

    inner class SectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sectionName: TextView = view.findViewById(R.id.tvSectionName)
        val timeRange: TextView = view.findViewById(R.id.tvTimeRange)
        val dayLayout: LinearLayout = view.findViewById(R.id.dayIndicatorLayout)
        val appIconsLayout: LinearLayout = view.findViewById(R.id.llAppIcons)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_section, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val section = sections[position]
        holder.sectionName.text = section.name
        holder.timeRange.text = "%02d:%02d - %02d:%02d".format(
            section.startHour, section.startMinute,
            section.endHour, section.endMinute
        )

        // Show day initials
        val dayMap = listOf("M", "T", "W", "T", "F", "S", "S")
        holder.dayLayout.removeAllViews()
        section.days.forEach { day ->
            val dayText = TextView(context).apply {
                text = dayMap[day - 1]
                textSize = 12f
                setPadding(4, 0, 4, 0)
            }
            holder.dayLayout.addView(dayText)
        }

        // App icons placeholder
        holder.appIconsLayout.removeAllViews()
        section.apps.forEach { pkg ->
            val icon = ImageView(context).apply {
                setImageResource(R.drawable.ic_gear) // Replace with your app icon logic
                layoutParams = LinearLayout.LayoutParams(60, 60).apply {
                    setMargins(4, 0, 4, 0)
                }
            }
            holder.appIconsLayout.addView(icon)
        }
    }

    override fun getItemCount(): Int = sections.size
}
