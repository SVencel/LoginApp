package com.example.login.fragments

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.login.R

class SettingsFragment : Fragment() {

    private lateinit var permissionStatusText: TextView
    private lateinit var enableMonitoringButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        permissionStatusText = view.findViewById(R.id.tvPermissionStatus)
        enableMonitoringButton = view.findViewById(R.id.btnEnableMonitoring)

        updatePermissionStatus()

        enableMonitoringButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        return view
    }

    private fun updatePermissionStatus() {
        val usageGranted = Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?.contains(requireContext().packageName) == true

        permissionStatusText.text = if (usageGranted) "Monitoring is ENABLED" else "Monitoring NOT enabled"
    }
}
