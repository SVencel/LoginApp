package com.example.login.fragments

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.login.LoginActivity
import com.example.login.MainActivity
import androidx.appcompat.app.AlertDialog
import com.example.login.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat


class ProfileFragment : Fragment() {

    private lateinit var thresholdInput: EditText
    private lateinit var saveButton: Button
    private lateinit var logoutButton: Button
    private lateinit var quoteInput: EditText
    private lateinit var userInfoText: TextView
    private lateinit var permissionStatusText: TextView
    private lateinit var goalsTextView: TextView



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
        goalsTextView = view.findViewById(R.id.tvGoals)


        goalsTextView.setOnClickListener {
            showAddGoalsDialog()
        }

        loadUserSettings()

        saveButton.setOnClickListener {
            saveSettings()
        }

        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            // 🔄 Optional: clear any saved login state if you use SharedPreferences
            val prefs = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            // 🚀 Navigate back to login screen
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        permissionStatusText = view.findViewById(R.id.tvPermissionStatus)
        updatePermissionStatus()


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


        goalsTextView?.setOnClickListener {
            showAddGoalsDialog()
        }

        db.collection("users").document(user.uid)
            .collection("onboardingAnswers")
            .document("initialSurvey")
            .get()
            .addOnSuccessListener { answers ->
                val goalsList = answers.get("goal_30_days") as? List<*> ?: emptyList<Any>()

                val hasGoals = goalsList.any { it.toString().isNotBlank() }

                goalsTextView?.text = if (hasGoals) {
                    goalsList
                        .filter { it.toString().isNotBlank() }
                        .joinToString("\n• ", prefix = "🎯 Your Goals:\n• ") { it.toString() }
                } else {
                    "🎯 No goals set yet. Tap to add."
                }

            }
    }

    private fun showAddGoalsDialog() {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 8)
        }

        val goalInputs = List(3) { i ->
            EditText(context).apply {
                hint = "Goal ${i + 1}"
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 16, 0, 0) }
                layout.addView(this)
            }
        }

        AlertDialog.Builder(context)
            .setTitle("Set Your Goals")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val goalMap = hashMapOf<String, Any>(
                    "goal_30_days" to goalInputs.map { it.text.toString().trim() }.filter { it.isNotEmpty() }
                )
                goalInputs.forEachIndexed { index, editText ->
                    val key = "goal_${index + 1}"
                    val value = editText.text.toString().trim()
                    if (value.isNotEmpty()) goalMap[key] = value
                }

                val user = FirebaseAuth.getInstance().currentUser ?: return@setPositiveButton
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(user.uid)
                    .collection("onboardingAnswers")
                    .document("initialSurvey")
                    .update(goalMap)
                    .addOnSuccessListener {
                        Toast.makeText(context, "✅ Goals saved!", Toast.LENGTH_SHORT).show()
                        loadUserSettings() // Refresh UI
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "❌ Failed to save goals.", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    private fun updatePermissionStatus() {
        val context = requireContext()
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val usageAccess = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED

        val accessibility = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )?.contains(context.packageName) == true

        val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

        val status = """
        🔐 Permissions:
        • Usage Access: ${if (usageAccess) "✅ Enabled" else "❌ Not Enabled"}
        • Accessibility: ${if (accessibility) "✅ Enabled" else "❌ Not Enabled"}
        • Notifications: ${if (notificationsEnabled) "✅ Enabled" else "❌ Not Enabled"}
    """.trimIndent()

        permissionStatusText.text = status

        if (!notificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    @Deprecated("Deprecated API, consider using registerForActivityResult instead")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            updatePermissionStatus()
        }
    }

}
