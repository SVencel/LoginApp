package com.example.login

import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.login.fragments.*
import com.example.login.utils.ProductivitySessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private var productivitySessionManager: ProductivitySessionManager? = null
    private var thresholdListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createHardcoreNotificationChannel()

        bottomNav = findViewById(R.id.bottomNavigationView)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener {
            val selectedFragment = when (it.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_focus -> FocusFragment()
                R.id.nav_progress -> ProgressFragment()
                R.id.nav_friends -> FriendsFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> null
            }

            selectedFragment?.let { fragment ->
                loadFragment(fragment)
                true
            } ?: false
        }

        observeProductivityThreshold()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }


    private fun observeProductivityThreshold() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user.uid)

        thresholdListener = userRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                println("⚠️ Error fetching productivity threshold or no data.")
                return@addSnapshotListener
            }

            val threshold = snapshot.getLong("productivityPromptMinutes")?.toInt() ?: 60
            println("🔁 Live threshold update: $threshold minutes")

            productivitySessionManager?.stop()
            productivitySessionManager = ProductivitySessionManager(this, threshold) {
                showProductivityDialog()
            }
            productivitySessionManager?.start()
        }
    }

    private fun showProductivityDialog() {
        runOnUiThread {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🧠 Are you being productive?")
                .setMessage("You've been using your device for a while. Staying focused?")
                .setPositiveButton("✅ Yes") { _, _ -> logProductivityResponse(true) }
                .setNegativeButton("❌ No") { _, _ -> logProductivityResponse(false) }
                .show()
        }
    }

    private fun logProductivityResponse(isProductive: Boolean) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

        val data = mapOf(
            "timestamp" to now,
            "isProductive" to isProductive
        )

        db.collection("users")
            .document(user.uid)
            .collection("productivityCheck")
            .add(data)
            .addOnSuccessListener {
                println("📘 Logged productivity response: $isProductive")
            }
    }

    private fun createHardcoreNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "hardcore_mode_channel"
            val name = "Hardcore Mode Alerts"
            val descriptionText = "Notifications that are allowed during Hardcore Mode"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = android.app.NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                setBypassDnd(true) // 🔥 THIS lets it break through DND
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        productivitySessionManager?.stop()
        thresholdListener?.remove()
    }
}
