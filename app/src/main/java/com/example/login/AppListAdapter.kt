package com.example.login

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.BaseAdapter
import android.content.pm.PackageManager

class AppListAdapter(
    private val context: Context,
    private val apps: List<Pair<String, String>>, // Pair<AppName, PackageName>
    private val selectedApps: MutableSet<String>
) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val packageManager: PackageManager = context.packageManager

    override fun getCount(): Int = apps.size
    override fun getItem(position: Int): Any = apps[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.list_item_app, parent, false)
        val appNameText: TextView = view.findViewById(R.id.appName)
        val appIcon: ImageView = view.findViewById(R.id.appIcon)
        val checkBox: CheckBox = view.findViewById(R.id.appCheckbox)

        val (appName, packageName) = apps[position]

        appNameText.text = appName
        appIcon.setImageDrawable(packageManager.getApplicationIcon(packageName))
        checkBox.isChecked = selectedApps.contains(packageName)

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedApps.add(packageName) else selectedApps.remove(packageName)
        }

        return view
    }
}
