package com.example.login

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BlockedAppAdapter(private val apps: List<String>) :
    RecyclerView.Adapter<BlockedAppAdapter.BlockedViewHolder>() {

    class BlockedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appName: TextView = view.findViewById(R.id.tvBlockedAppName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_blocked_app, parent, false)
        return BlockedViewHolder(view)
    }

    override fun onBindViewHolder(holder: BlockedViewHolder, position: Int) {
        val pm = holder.itemView.context.packageManager
        val packageName = apps[position]
        val label = try {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            packageName
        }
        holder.appName.text = label
    }

    override fun getItemCount() = apps.size
}
