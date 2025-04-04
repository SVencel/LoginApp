package com.example.login.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.login.MainActivity
import com.example.login.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var thresholdInput: EditText
    private lateinit var saveButton: Button
    private lateinit var logoutButton: Button
    private lateinit var quoteInput: EditText
    private lateinit var userInfoText: TextView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        thresholdInput = view.findViewById(R.id.etThreshold)
        saveButton = view.findViewById(R.id.btnSaveSettings)
        logoutButton = view.findViewById(R.id.btnLogout)
        quoteInput = view.findViewById(R.id.etQuote)
        userInfoText = view.findViewById(R.id.tvUserInfo)

        loadUserSettings()

        saveButton.setOnClickListener {
            saveSettings()
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()
        }

        return view
    }

    private fun loadUserSettings() {
        val user = auth.currentUser ?: return
        userInfoText.text = "Logged in as: ${user.email}"

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val threshold = doc.getLong("productivityPromptMinutes")?.toInt() ?: 60
                val quote = doc.getString("customMotivation") ?: ""

                thresholdInput.setText(threshold.toString())
                quoteInput.setText(quote)
            }
    }

    private fun saveSettings() {
        val user = auth.currentUser ?: return
        val thresholdStr = thresholdInput.text.toString().trim()
        val customQuote = quoteInput.text.toString().trim()

        val threshold = thresholdStr.toIntOrNull()
        if (threshold == null || threshold <= 0) {
            Toast.makeText(requireContext(), "Enter a valid number", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(user.uid)
            .update(
                mapOf(
                    "productivityPromptMinutes" to threshold,
                    "customMotivation" to customQuote
                )
            )
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "✅ Saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "❌ Error saving", Toast.LENGTH_SHORT).show()
            }
    }
}
