package com.example.login.fragments

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.app.NotificationManager.Policy
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import android.graphics.Color
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.login.CreateSectionActivity
import com.example.login.hardcore.HardcoreModeService
import com.example.login.R
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.Calendar
import androidx.appcompat.widget.SwitchCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FocusFragment : Fragment() {

    private lateinit var switchHardcore: SwitchCompat
    private lateinit var switchDoomscroll: SwitchCompat
    private lateinit var tvDoomscrollLimit: TextView
    private lateinit var btnManageSections: Button
    private lateinit var quoteText: TextView
    private lateinit var rvSections: RecyclerView
    private lateinit var sectionAdapter: SectionAdapter
    private lateinit var seekBarDoomSensitivity: SeekBar
    private lateinit var tvSensitivityLabel: TextView
    private lateinit var switchProductivity: SwitchCompat
    private lateinit var seekBarProductivity: SeekBar
    private lateinit var tvProductivityLabel: TextView

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
        val endMinute: Int,
        val enabled: Boolean = true
    )

    private val PREF_KEY = "hardcoreMode"
    private val DOOM_PREFS = "doomPrefs"
    private val KEY_ENABLED = "doomEnabled"
    private val KEY_SCROLLS = "doomScrolls"
    private val KEY_WINDOW = "doomWindow"
    private val KEY_SENSITIVITY = "doomSensitivity"
    private val sensitivityLabels = listOf("Light", "Medium", "Hard")


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_focus, container, false)

        switchHardcore = view.findViewById(R.id.switchHardcore)
        switchDoomscroll = view.findViewById(R.id.switchDoomscroll)
        tvDoomscrollLimit = view.findViewById(R.id.tvDoomscrollLimit)
        btnManageSections = view.findViewById(R.id.btnManageSections)
        quoteText = view.findViewById(R.id.tvMotivationQuote)
        rvSections = view.findViewById(R.id.rvSections)
        seekBarDoomSensitivity = view.findViewById(R.id.seekBarDoomSensitivity)
        tvSensitivityLabel = view.findViewById(R.id.tvSensitivityLabel)
        switchProductivity = view.findViewById(R.id.switchProductivity)
        seekBarProductivity = view.findViewById(R.id.seekBarProductivity)
        tvProductivityLabel = view.findViewById(R.id.tvProductivityLabel)


        quoteText.text = quotes.random()


        btnManageSections.setOnClickListener {
            startActivityForResult(Intent(requireContext(), CreateSectionActivity::class.java), 101)
        }

        val generalPrefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val doomPrefs = requireContext().getSharedPreferences(DOOM_PREFS, Context.MODE_PRIVATE)

// HARDCORE MODE INIT
        val isHardcore = generalPrefs.getBoolean(PREF_KEY, false)
        switchHardcore.isChecked = isHardcore

        switchHardcore.setOnCheckedChangeListener { _, isChecked ->
            generalPrefs.edit().putBoolean(PREF_KEY, isChecked).apply()
            if (isChecked) {
                requireContext().startService(Intent(requireContext(), HardcoreModeService::class.java))
                Toast.makeText(requireContext(), "☠️ HARDCORE MODE activated", Toast.LENGTH_SHORT).show()
                enableHardcoreMode()
            } else {
                requireContext().stopService(Intent(requireContext(), HardcoreModeService::class.java))
                Toast.makeText(requireContext(), "✅ Welcome back to the chaos", Toast.LENGTH_SHORT).show()
                disableHardcoreMode()
            }
        }
        val infoIcon = view.findViewById<ImageView>(R.id.ivHardcoreInfo)
        infoIcon.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("☠️ Hardcore Mode")
                .setMessage("Allows calls, text messages, and alarms. Everything else is silenced.")
                .setPositiveButton("OK", null)
                .show()
        }

// DOOMSCROLL SENSITIVITY INIT
        val savedSensitivity = doomPrefs.getInt(KEY_SENSITIVITY, 1)
        seekBarDoomSensitivity.progress = savedSensitivity
        tvSensitivityLabel.text = "Detection: ${sensitivityLabels[savedSensitivity]}"

        seekBarDoomSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                doomPrefs.edit().putInt(KEY_SENSITIVITY, progress).apply()
                tvSensitivityLabel.text = "Detection: ${sensitivityLabels[progress]}"
                tvDoomscrollLimit.text = getDoomscrollLimitText(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val headerHardcore = view.findViewById<LinearLayout>(R.id.headerHardcore)
        val expandableHardcore = view.findViewById<LinearLayout>(R.id.expandableHardcore)

        headerHardcore.setOnClickListener {
            if (expandableHardcore.visibility == View.VISIBLE) {
                expandableHardcore.visibility = View.GONE
            } else {
                expandableHardcore.visibility = View.VISIBLE
            }
        }

        val headerDoomscroll = view.findViewById<LinearLayout>(R.id.headerDoomscroll)
        val expandableDoomscroll = view.findViewById<LinearLayout>(R.id.expandableDoomscroll)

        headerDoomscroll.setOnClickListener {
            if (expandableDoomscroll.visibility == View.VISIBLE) {
                expandableDoomscroll.visibility = View.GONE
            } else {
                expandableDoomscroll.visibility = View.VISIBLE
            }
        }
        val headerProductivity = view.findViewById<LinearLayout>(R.id.headerProductivity)
        val expandableProductivity = view.findViewById<LinearLayout>(R.id.expandableProductivity)

        headerProductivity.setOnClickListener {
            if (expandableProductivity.visibility == View.VISIBLE) {
                expandableProductivity.visibility = View.GONE
            } else {
                expandableProductivity.visibility = View.VISIBLE
            }
        }


        val prefs = requireContext().getSharedPreferences("productivityPrefs", Context.MODE_PRIVATE)

        val detectionEnabled = prefs.getBoolean("detectionEnabled", true)
        switchProductivity.isChecked = detectionEnabled

        val savedMinutes = prefs.getInt("productivityPromptMinutes", 60).coerceIn(10, 120)
        val progress = (savedMinutes - 10) / 5
        seekBarProductivity.progress = progress
        tvProductivityLabel.text = "Prompt after: $savedMinutes minutes"


        switchProductivity.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("detectionEnabled", isChecked).apply()
            Toast.makeText(requireContext(), if (isChecked) "🎯 Productivity detection ON" else "🎯 Productivity detection OFF", Toast.LENGTH_SHORT).show()
        }

        seekBarProductivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val actualMinutes = 10 + (progress * 5)
                prefs.edit().putInt("productivityPromptMinutes", actualMinutes).apply()
                tvProductivityLabel.text = "Prompt after: $actualMinutes minutes"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })



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

    private fun enableHardcoreMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (!notificationManager.isNotificationPolicyAccessGranted) {
                startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                Toast.makeText(requireContext(), "Grant Do Not Disturb access", Toast.LENGTH_LONG).show()
                return
            }

            // Build the policy to allow calls, messages, and alarms
            val policy = Policy(
                Policy.PRIORITY_CATEGORY_CALLS or
                        Policy.PRIORITY_CATEGORY_MESSAGES or
                        Policy.PRIORITY_CATEGORY_ALARMS,

                Policy.PRIORITY_SENDERS_ANY,
                0 // No visual suppression
            )

            // Apply the policy and enable DND mode
            notificationManager.notificationPolicy = policy
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }
    }


    private fun disableHardcoreMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }

    private fun getDoomscrollLimitText(level: Int): String {
        return when (level) {
            0 -> "Limit: 15 scrolls in 5 minutes"
            1 -> "Limit: 9 scrolls in 5 minutes"
            2 -> "Limit: 6 scrolls in 5 minutes"
            else -> "Limit: 9 scrolls in 5 minutes"
        }
    }


    private fun setupDoomscrollingUI() {
        val prefs = requireContext().getSharedPreferences(DOOM_PREFS, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(KEY_ENABLED, true)

        switchDoomscroll.isChecked = isEnabled
        tvDoomscrollLimit.text = getDoomscrollLimitText(
            requireContext().getSharedPreferences(DOOM_PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_SENSITIVITY, 1)
        )

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
        val currentDay = now.get(Calendar.DAY_OF_WEEK).let { if (it == Calendar.SUNDAY) 7 else it - 1 }
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

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
                    val enabled = doc.getBoolean("enabled") ?: true

                    val fromTime = String.format("%02d:%02d", startHour, startMinute)
                    val toTime = String.format("%02d:%02d", endHour, endMinute)

                    sectionList.add(
                        Section(
                            name, apps, fromTime, toTime, days,
                            startHour, startMinute, endHour, endMinute, enabled
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
            val card: CardView = itemView as CardView
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

            val now = Calendar.getInstance()
            val day = now.get(Calendar.DAY_OF_WEEK).let { if (it == Calendar.SUNDAY) 7 else it - 1 }
            val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val startMin = section.startHour * 60 + section.startMinute
            val endMin = section.endHour * 60 + section.endMinute

            val activeNow = section.enabled && (day in section.days) &&
                    if (startMin < endMin) nowMin in startMin until endMin else nowMin >= startMin || nowMin < endMin

            val bgColor = when {
                !section.enabled -> R.color.section_disabled
                activeNow -> R.color.section_active
                else -> R.color.section_inactive
            }

            holder.card.setCardBackgroundColor(ContextCompat.getColor(context, bgColor))

            // Optional: Dim section name if disabled
            holder.nameText.setTextColor(
                if (!section.enabled) Color.GRAY else Color.BLACK
            )


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

                // Dynamically set Enable/Disable title
                val toggleItem = popup.menu.findItem(R.id.menu_toggle_enable)
                toggleItem.title = if (section.enabled) "Disable" else "Enable"

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

                        R.id.menu_toggle_enable -> {
                            val user = FirebaseAuth.getInstance().currentUser ?: return@setOnMenuItemClickListener true
                            val db = FirebaseFirestore.getInstance()
                            val newStatus = !section.enabled

                            db.collection("users").document(user.uid)
                                .collection("sections").document(section.name)
                                .update("enabled", newStatus)
                                .addOnSuccessListener {
                                    sections[position] = section.copy(enabled = newStatus)
                                    notifyItemChanged(position)
                                }
                                .addOnFailureListener {
                                    Toast.makeText(holder.itemView.context, "Failed to update", Toast.LENGTH_SHORT).show()
                                }
                            true
                        }

                        R.id.menu_delete -> {
                            // your delete code here
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
                section.enabled &&
                adjustedDay in section.days
                        && nowMinutes in (section.startHour * 60 + section.startMinute) until (section.endHour * 60 + section.endMinute)

            }.flatMap { it.apps }.toSet()
        }
    }
}
