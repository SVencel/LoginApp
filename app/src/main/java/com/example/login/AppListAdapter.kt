package com.example.login

import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView


class AppListAdapter(
    private val apps: List<AppInfo>,
    private val selectedApps: MutableSet<String>
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appName: TextView = itemView.findViewById(R.id.appNameText)
        val appCheckbox: CheckBox = itemView.findViewById(R.id.appCheckbox)
        val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val topTag: TextView = itemView.findViewById(R.id.topAppTag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.appName.text = app.name
        holder.appCheckbox.isChecked = selectedApps.contains(app.packageName)

        // Toggle on click
        holder.appCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedApps.add(app.packageName)
            else selectedApps.remove(app.packageName)
        }

        // Use placeholder icon
        holder.appIcon.setImageResource(R.drawable.ic_launcher_foreground)

        holder.topTag.visibility = if (app.isTopUsed) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int = apps.size
}
