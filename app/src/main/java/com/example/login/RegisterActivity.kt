package com.example.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPref: SharedPreferences
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        try {
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            sharedPref = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        } catch (e: Exception) {
            Log.e("RegisterActivity", "Firebase init error: ${e.message}")
            showToast("Firebase initialization failed. Try restarting the app.")
        }

        val usernameEditText: EditText = findViewById(R.id.etUsername)
        val emailEditText: EditText = findViewById(R.id.etRegisterEmail)
        val passwordEditText: EditText = findViewById(R.id.etRegisterPassword)
        registerButton = findViewById(R.id.btnRegisterUser)
        val backToLoginButton: Button = findViewById(R.id.btnBackToLogin)

        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (!isInternetAvailable(this)) {
                showToast("No internet connection. Please check your network and try again.")
                return@setOnClickListener
            }

            if (!validateInput(username, email, password)) return@setOnClickListener

            registerUser(username, email, password)
        }

        backToLoginButton.setOnClickListener {
            startActivity(Intent(this, OnboardingQuestionsActivity::class.java))
            finish()
        }
    }

    /** ✅ VALIDATE USER INPUT **/
    private fun validateInput(username: String, email: String, password: String): Boolean {
        if (username.isEmpty()) {
            showToast("Please enter a username")
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Please enter a valid email address")
            return false
        }
        if (password.length < 6) {
            showToast("Password must be at least 6 characters")
            return false
        }
        return true
    }

    /** ✅ REGISTER USER IN FIREBASE **/
    private fun registerUser(username: String, email: String, password: String) {
        registerButton.isEnabled = false // 🔹 Prevent multiple clicks
        registerButton.text = "Registering..." // 🔹 Show progress

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                registerButton.isEnabled = true // 🔹 Re-enable button
                registerButton.text = "Sign Up" // 🔹 Reset button text

                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        saveUserToFirestore(userId, username, email)
                    }
                } else {
                    showToast("Registration Failed: ${task.exception?.message}")
                }
            }
    }

    /** ✅ SAVE USER TO FIRESTORE **/
    private fun saveUserToFirestore(userId: String, username: String, email: String) {
        val userMap = hashMapOf(
            "username" to username,
            "email" to email
        )

        db.collection("users").document(userId).set(userMap)
            .addOnSuccessListener {
                sharedPref.edit().putBoolean("isLoggedIn", true).apply() // 🔹 Save login state
                showToast("Registration successful!")
                startActivity(Intent(this, OnboardingQuestionsActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                showToast("Failed to save user data. Please try again.")
            }
    }

    /** ✅ CHECK INTERNET CONNECTION **/
    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }

    /** ✅ SHOW TOAST MESSAGE **/
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
