package com.example.login

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private val apps: List<AppInfo>,
    private val selectedApps: MutableSet<String>
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appName: TextView = itemView.findViewById(R.id.appNameText)
        val appCheckbox: CheckBox = itemView.findViewById(R.id.appCheckbox)
        val appIcon: ImageView = itemView.findViewById(R.id.appIcon) // 🔹 Add this line
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.appName.text = app.name
        holder.appCheckbox.setOnCheckedChangeListener(null) // prevent unwanted callbacks
        holder.appCheckbox.isChecked = selectedApps.contains(app.packageName)

        // ✅ Update selected apps when checkbox is clicked
        holder.appCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedApps.add(app.packageName)
            } else {
                selectedApps.remove(app.packageName)
            }
        }

        // Set the app icon
        val packageManager = holder.itemView.context.packageManager
        val appIcon = packageManager.getApplicationIcon(app.packageName)
        holder.appIcon.setImageDrawable(appIcon)
    }

    override fun getItemCount(): Int = apps.size
}
