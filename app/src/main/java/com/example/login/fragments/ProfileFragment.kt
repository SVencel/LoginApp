package com.example.login.fragments

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.login.R

class ProfileFragment : Fragment() {

    private lateinit var thresholdInput: EditText
    private lateinit var saveButton: Button
    private lateinit var logoutButton: Button
    private lateinit var quoteInput: EditText
    private lateinit var userInfoText: TextView
    private lateinit var permissionStatusText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        thresholdInput = view.findViewById(R.id.etThreshold)
        saveButton = view.findViewById(R.id.btnSaveSettings)
        logoutButton = view.findViewById(R.id.btnLogout)
        quoteInput = view.findViewById(R.id.etQuote)
        userInfoText = view.findViewById(R.id.tvUserInfo)
        permissionStatusText = view.findViewById(R.id.tvPermissionStatus)

        // Sample static UI content
        userInfoText.text = "Logged in as: design@example.com"
        thresholdInput.setText("60")
        quoteInput.setText("Keep grinding 💪")

        permissionStatusText.text = """
            🔐 Permissions:
            • Usage Access: ✅ Enabled
            • Accessibility: ✅ Enabled
        """.trimIndent()

        return view
    }
}
