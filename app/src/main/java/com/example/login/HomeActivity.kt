package com.example.login


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import android.widget.TextView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import androidx.appcompat.app.AppCompatActivity



class HomeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val user = auth.currentUser
        val welcomeText: TextView = findViewById(R.id.tvWelcome)
        val logoutButton: Button = findViewById(R.id.btnLogout)

        // Set welcome message (optional)
        user?.uid?.let { userId ->
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("username")
                        welcomeText.text = "Welcome, $username!"
                    } else {
                        welcomeText.text = "Welcome!"
                    }
                }
                .addOnFailureListener {
                    welcomeText.text = "Welcome!"
                }
        }

        // Logout button click
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
}
