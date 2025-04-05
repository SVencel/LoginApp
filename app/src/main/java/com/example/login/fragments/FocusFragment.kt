package com.example.login.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.login.CreateSectionActivity
import com.example.login.LockScheduleActivity
import com.example.login.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FocusFragment : Fragment() {

    private lateinit var toggleOffline: Switch
    private lateinit var btnSchedule: Button
    private lateinit var btnManageSections: Button
    private lateinit var quoteText: TextView
    private lateinit var rvSections: RecyclerView
    private lateinit var sectionAdapter: SectionAdapter

    data class Section(
        val name: String,
        val apps: List<String>,
        val fromTime: String,
        val toTime: String
    )
    private val sectionList = mutableListOf<Section>()



    private val quotes = listOf(
        "“You can't do big things if you're distracted by small things.”",
        "“Focus is more important than intelligence.”",
        "“Offline is the new luxury.”",
        "“Discipline is choosing between what you want now and what you want most.”",
        "“STAY HARD”"
    )

    private val PREF_KEY = "offlineMode"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_focus, container, false)

        toggleOffline = view.findViewById(R.id.switchGoOffline)
        btnSchedule = view.findViewById(R.id.btnLockScheduler)
        btnManageSections = view.findViewById(R.id.btnManageSections)
        quoteText = view.findViewById(R.id.tvMotivationQuote)
        rvSections = view.findViewById(R.id.rvSections)

        quoteText.text = quotes.random()

        btnSchedule.setOnClickListener {
            startActivity(Intent(requireContext(), LockScheduleActivity::class.java))
        }

        btnManageSections.setOnClickListener {
            startActivity(Intent(requireContext(), CreateSectionActivity::class.java))
        }

        val prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isOffline = prefs.getBoolean(PREF_KEY, false)
        toggleOffline.isChecked = isOffline

        toggleOffline.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_KEY, isChecked).apply()
            Toast.makeText(
                requireContext(),
                if (isChecked) "🚫 Offline mode ON" else "✅ You're back online!",
                Toast.LENGTH_SHORT
            ).show()
        }

        setupSectionList() // ✅ Don't forget to call this

        return view
    }

    private fun setupSectionList() {
        sectionAdapter = SectionAdapter(sectionList)
        rvSections.layoutManager = LinearLayoutManager(requireContext())
        rvSections.adapter = sectionAdapter

        fetchUserSections()
    }

    private fun fetchUserSections() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid)
            .collection("sections")
            .get()
            .addOnSuccessListener { documents ->
                sectionList.clear()
                for (doc in documents) {
                    val name = doc.getString("name") ?: continue
                    val apps = doc.get("apps") as? List<String> ?: emptyList()

                    val startHour = doc.getLong("startHour")?.toInt() ?: 0
                    val startMinute = doc.getLong("startMinute")?.toInt() ?: 0
                    val endHour = doc.getLong("endHour")?.toInt() ?: 0
                    val endMinute = doc.getLong("endMinute")?.toInt() ?: 0

                    val fromTime = String.format("%02d:%02d", startHour, startMinute)
                    val toTime = String.format("%02d:%02d", endHour, endMinute)

                    sectionList.add(Section(name, apps, fromTime, toTime))
                }
                sectionAdapter.notifyDataSetChanged()
            }


    }


    class SectionAdapter(private val sections: List<Section>) :
        RecyclerView.Adapter<SectionAdapter.SectionViewHolder>() {

        inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameText: TextView = itemView.findViewById(R.id.tvSectionName)
            val appIconsLayout: LinearLayout = itemView.findViewById(R.id.llAppIcons)
            val timeRangeText: TextView = itemView.findViewById(R.id.tvTimeRange)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_section, parent, false)
            return SectionViewHolder(view)
        }

        override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
            val section = sections[position]
            holder.nameText.text = section.name
            holder.timeRangeText.text = "${section.fromTime} - ${section.toTime}"
            holder.appIconsLayout.removeAllViews()

            val pm = holder.itemView.context.packageManager
            val visibleApps = section.apps.take(4)

            visibleApps.forEach { packageName ->
                try {
                    val icon = pm.getApplicationIcon(packageName)
                    val imageView = ImageView(holder.itemView.context).apply {
                        setImageDrawable(icon)
                        layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                            setMargins(8, 0, 8, 0)
                        }
                    }
                    holder.appIconsLayout.addView(imageView)
                } catch (_: Exception) {}
            }

            if (section.apps.size > 4) {
                val extra = TextView(holder.itemView.context).apply {
                    text = "+${section.apps.size - 4}"
                    setTextColor(android.graphics.Color.DKGRAY)
                    setPadding(16, 0, 0, 0)
                }
                holder.appIconsLayout.addView(extra)
            }
        }

        override fun getItemCount(): Int = sections.size
    }

}
