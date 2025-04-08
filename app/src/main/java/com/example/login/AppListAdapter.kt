package com.example.login

import android.graphics.Color
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private val apps: List<AppInfo>,
    private val selectedApps: MutableSet<String>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_APP = 0
        private const val TYPE_DIVIDER = 1
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appName: TextView = itemView.findViewById(R.id.appNameText)
        val appCheckbox: CheckBox = itemView.findViewById(R.id.appCheckbox)
        val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val topTag: TextView = itemView.findViewById(R.id.topAppTag)
    }

    override fun getItemViewType(position: Int): Int {
        return if (apps.size > 5 && position == 5) TYPE_DIVIDER else TYPE_APP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_DIVIDER) {
            val divider = View(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    2
                ).apply {
                    setMargins(0, 16, 0, 16)
                }
                setBackgroundColor(Color.LTGRAY)
            }
            object : RecyclerView.ViewHolder(divider) {}
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            AppViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_DIVIDER) return

        val actualIndex = if (apps.size > 5 && position > 5) position - 1 else position
        val app = apps[actualIndex]
        val appHolder = holder as AppViewHolder

        appHolder.appName.text = app.name
        appHolder.appCheckbox.setOnCheckedChangeListener(null)
        appHolder.appCheckbox.isChecked = selectedApps.contains(app.packageName)

        appHolder.appCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedApps.add(app.packageName)
            else selectedApps.remove(app.packageName)
        }

        if (app.isTopUsed) {
            appHolder.topTag.visibility = View.VISIBLE
        } else {
            appHolder.topTag.visibility = View.GONE
        }

        val pm = holder.itemView.context.packageManager
        val appIcon = pm.getApplicationIcon(app.packageName)
        appHolder.appIcon.setImageDrawable(appIcon)
    }

    override fun getItemCount(): Int {
        return apps.size + if (apps.size > 5) 1 else 0
    }
}
