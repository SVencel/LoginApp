package com.example.login

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.BaseAdapter

data class AppInfo(val name: String, val packageName: String, val icon: Drawable)

class AppListAdapter(
    private val context: Context,
    private val appList: List<AppInfo>,
    private val selectedApps: MutableSet<String>
) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int = appList.size
    override fun getItem(position: Int): Any = appList[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.list_item_app, parent, false)

        val appIcon: ImageView = view.findViewById(R.id.appIcon)
        val appName: TextView = view.findViewById(R.id.appName)
        val appCheckbox: CheckBox = view.findViewById(R.id.appCheckbox)

        val app = appList[position]

        appIcon.setImageDrawable(app.icon)
        appName.text = app.name
        appCheckbox.isChecked = selectedApps.contains(app.packageName)

        appCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedApps.add(app.packageName)
            } else {
                selectedApps.remove(app.packageName)
            }
        }

        return view
    }

    fun getSelectedApps(): Set<String> {
        return selectedApps
    }
}
