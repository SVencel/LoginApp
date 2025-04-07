package com.example.login.fragments

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.login.CreateSectionActivity
import com.example.login.LockScheduleActivity
import com.example.login.R
import java.util.Calendar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FocusFragment : Fragment() {

    private lateinit var toggleOffline: Switch
    private lateinit var switchDoomscroll: Switch
    private lateinit var tvDoomscrollLimit: TextView
    private lateinit var btnSchedule: Button
    private lateinit var btnManageSections: Button
    private lateinit var quoteText: TextView
    private lateinit var rvSections: RecyclerView
    private lateinit var sectionAdapter: SectionAdapter

    private val sectionList = mutableListOf<Section>()

    private val quotes = listOf(
        "“You can't do big things if you're distracted by small things.”",
        "“Focus is more important than intelligence.”",
        "“Offline is the new luxury.”",
        "“Discipline is choosing between what you want now and what you want most.”",
        "“STAY HARD”"
    )

    data class Section(
        val name: String,
        val apps: List<String>,
        val fromTime: String,
        val toTime: String,
        val days: List<Int>,
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int
    )

    private val PREF_KEY = "offlineMode"
    private val DOOM_PREFS = "doomPrefs"
    private val KEY_ENABLED = "doomEnabled"
    private val KEY_SCROLLS = "doomScrolls"
    private val KEY_WINDOW = "doomWindow"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_focus, container, false)

        toggleOffline = view.findViewById(R.id.switchGoOffline)
        switchDoomscroll = view.findViewById(R.id.switchDoomscroll)
        tvDoomscrollLimit = view.findViewById(R.id.tvDoomscrollLimit)
        btnSchedule = view.findViewById(R.id.btnLockScheduler)
        btnManageSections = view.findViewById(R.id.btnManageSections)
        quoteText = view.findViewById(R.id.tvMotivationQuote)
        rvSections = view.findViewById(R.id.rvSections)

        quoteText.text = quotes.random()

        btnSchedule.setOnClickListener {
            startActivity(Intent(requireContext(), LockScheduleActivity::class.java))
        }

        btnManageSections.setOnClickListener {
            startActivityForResult(Intent(requireContext(), CreateSectionActivity::class.java), 101)
        }

        val prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isOffline = prefs.getBoolean(PREF_KEY, false)
        toggleOffline.isChecked = isOffline

        toggleOffline.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_KEY, isChecked).apply()
            if (isChecked) {
                Toast.makeText(requireContext(), "🚫 Going offline...", Toast.LENGTH_SHORT).show()
                goOffline()
            } else {
                Toast.makeText(requireContext(), "✅ You're back online!", Toast.LENGTH_SHORT).show()
                goOnline()
            }
        }

        setupDoomscrollingUI()
        setupSectionList()

        return view
    }

    override fun onResume() {
        super.onResume()
        fetchUserSections()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101) {
            fetchUserSections()
        }
    }

    private fun goOffline() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                Toast.makeText(requireContext(), "Grant Do Not Disturb access", Toast.LENGTH_LONG).show()
                return
            }
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            wifiManager.isWifiEnabled = false
        } else {
            startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
            Toast.makeText(requireContext(), "Please disable Wi-Fi manually", Toast.LENGTH_LONG).show()
        }

        startActivity(Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS))
        Toast.makeText(requireContext(), "Please disable Mobile Data manually", Toast.LENGTH_LONG).show()
    }

    private fun goOnline() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            wifiManager.isWifiEnabled = true
        } else {
            startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
            Toast.makeText(requireContext(), "Enable Wi-Fi manually", Toast.LENGTH_SHORT).show()
        }

        startActivity(Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS))
        Toast.makeText(requireContext(), "Enable Mobile Data manually", Toast.LENGTH_SHORT).show()
    }

    private fun setupDoomscrollingUI() {
        val prefs = requireContext().getSharedPreferences(DOOM_PREFS, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(KEY_ENABLED, true)
        val scrolls = prefs.getInt(KEY_SCROLLS, 5)
        val window = prefs.getInt(KEY_WINDOW, 10)

        switchDoomscroll.isChecked = isEnabled
        tvDoomscrollLimit.text = "Limit: $scrolls scrolls in $window minutes"

        switchDoomscroll.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(KEY_ENABLED, checked).apply()
            Toast.makeText(
                requireContext(),
                if (checked) "☠️ Doomscroll detection ON" else "🛑 Doomscroll detection OFF",
                Toast.LENGTH_SHORT
            ).show()
        }
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

        val now = Calendar.getInstance()
        Log.d("FocusCheck", "Now: Day=${now.get(Calendar.DAY_OF_WEEK)}, Time=${now.get(Calendar.HOUR_OF_DAY)}:${now.get(Calendar.MINUTE)}")

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
                    val days = (doc.get("days") as? List<Long>)?.map { it.toInt() } ?: emptyList()

                    val fromTime = String.format("%02d:%02d", startHour, startMinute)
                    val toTime = String.format("%02d:%02d", endHour, endMinute)

                    sectionList.add(
                        Section(
                            name = name,
                            apps = apps,
                            fromTime = fromTime,
                            toTime = toTime,
                            days = days,
                            startHour = startHour,
                            startMinute = startMinute,
                            endHour = endHour,
                            endMinute = endMinute
                        )
                    )
                }
                activeSectionsCache = sectionList.toList()
                sectionAdapter.notifyDataSetChanged()
            }
    }

    class SectionAdapter(private val sections: MutableList<Section>) :
        RecyclerView.Adapter<SectionAdapter.SectionViewHolder>() {

        inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameText: TextView = itemView.findViewById(R.id.tvSectionName)
            val appIconsLayout: LinearLayout = itemView.findViewById(R.id.llAppIcons)
            val timeRangeText: TextView = itemView.findViewById(R.id.tvTimeRange)
            val optionsIcon: ImageView = itemView.findViewById(R.id.ivSectionOptions)
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

            val dayLayout = holder.itemView.findViewById<LinearLayout>(R.id.dayIndicatorLayout)
            dayLayout.removeAllViews()

            val dayLetters = listOf("M", "T", "W", "Th", "F", "Sa", "Su")
            val context = holder.itemView.context

            section.days.sorted().forEach { day ->
                if (day in 1..7) {
                    val sizePx = dpToPx(context, 28)
                    val dayText = TextView(context).apply {
                        text = dayLetters[day - 1]
                        textSize = 12f
                        setTextColor(android.graphics.Color.WHITE)
                        setBackgroundResource(R.drawable.day_circle_background)
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                            setMargins(dpToPx(context, 4), 0, dpToPx(context, 4), 0)
                        }
                    }
                    dayLayout.addView(dayText)
                }
            }

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

            holder.optionsIcon.setOnClickListener {
                val popup = PopupMenu(holder.itemView.context, holder.optionsIcon)
                popup.menuInflater.inflate(R.menu.section_options_menu, popup.menu)

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.menu_modify -> {
                            val context = holder.itemView.context
                            val intent = Intent(context, CreateSectionActivity::class.java).apply {
                                putExtra("sectionName", section.name)
                                putExtra("apps", section.apps.toTypedArray())
                                putExtra("fromTime", section.fromTime)
                                putExtra("toTime", section.toTime)
                                putIntegerArrayListExtra("days", ArrayList(section.days))
                            }
                            context.startActivity(intent)
                            true
                        }

                        R.id.menu_delete -> {
                            AlertDialog.Builder(holder.itemView.context)
                                .setTitle("Delete Section")
                                .setMessage("Are you sure you want to delete \"${section.name}\"?")
                                .setPositiveButton("Delete") { _, _ ->
                                    val user = FirebaseAuth.getInstance().currentUser
                                    val db = FirebaseFirestore.getInstance()
                                    user?.let {
                                        db.collection("users").document(user.uid)
                                            .collection("sections").document(section.name)
                                            .delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(holder.itemView.context, "Deleted ${section.name}", Toast.LENGTH_SHORT).show()
                                                sections.removeAt(position)
                                                notifyItemRemoved(position)
                                                notifyItemRangeChanged(position, sections.size)
                                            }
                                    }
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                            true
                        }

                        else -> false
                    }
                }

                popup.show()
            }
        }

        override fun getItemCount(): Int = sections.size

        private fun dpToPx(context: Context, dp: Int): Int {
            val density = context.resources.displayMetrics.density
            return (dp * density).toInt()
        }
    }

    companion object {
        private var activeSectionsCache: List<Section>? = null

        fun getCurrentlyBlockedApps(context: Context): Set<String> {
            val now = Calendar.getInstance()
            val currentDay = now.get(Calendar.DAY_OF_WEEK)
            val adjustedDay = if (currentDay == Calendar.SUNDAY) 7 else currentDay - 1
            val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

            return activeSectionsCache.orEmpty().filter { section ->
                adjustedDay in section.days &&
                        nowMinutes in (section.startHour * 60 + section.startMinute) until (section.endHour * 60 + section.endMinute)
            }.flatMap { it.apps }.toSet()
        }
    }
}
