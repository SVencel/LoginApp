package com.example.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailSignInButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        // ✅ Check if user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        val emailEditText: EditText = findViewById(R.id.etEmail)
        val passwordEditText: EditText = findViewById(R.id.etPassword)
        emailSignInButton = findViewById(R.id.btnEmailSignIn)
        val registerButton: Button = findViewById(R.id.btnRegister)
        val forgotPasswordButton: Button = findViewById(R.id.btnForgotPassword)

        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        forgotPasswordButton.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        emailSignInButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // ✅ Check for internet connection before attempting sign-in
            if (!isInternetAvailable(this)) {
                showToast("No internet connection! Please connect to WiFi or Mobile Data.")
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                showToast("Enter a valid email")
                return@setOnClickListener
            }

            if (!isValidPassword(password)) {
                showToast("Password must be at least 6 characters")
                return@setOnClickListener
            }

            signInUser(email, password)
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    private fun signInUser(email: String, password: String) {
        emailSignInButton.isEnabled = false // 🔹 Disable button
        emailSignInButton.text = "Signing in..." // 🔹 Update button text

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                emailSignInButton.isEnabled = true // 🔹 Re-enable button
                emailSignInButton.text = "Sign in" // 🔹 Reset button text

                if (task.isSuccessful) {
                    showToast("Login successful!")
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    showToast("Login Failed: ${task.exception?.message}")
                }
            }
    }

    // ✅ Check Internet Connection Before Signing In
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
