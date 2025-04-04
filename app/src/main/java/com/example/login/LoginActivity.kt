package com.example.login

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var signInButton: Button
    private lateinit var forgotPasswordButton: Button
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        emailInput = findViewById(R.id.etEmail)
        passwordInput = findViewById(R.id.etPassword)
        signInButton = findViewById(R.id.btnSignIn)
        forgotPasswordButton = findViewById(R.id.btnForgotPassword)
        registerButton = findViewById(R.id.btnRegister)

        signInButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (!isInternetAvailable()) {
                showToast("No internet connection.")
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showToast("Enter a valid email.")
                return@setOnClickListener
            }

            if (password.length < 6) {
                showToast("Password must be at least 6 characters.")
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        forgotPasswordButton.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        signInButton.isEnabled = false
        signInButton.text = "Signing in..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                signInButton.isEnabled = true
                signInButton.text = "Sign In"

                if (task.isSuccessful) {
                    showToast("Welcome back!")

                    // ✅ Navigate to home
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    showToast("Login failed: ${task.exception?.message}")
                }
            }
    }

    private fun isInternetAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            val info = cm.activeNetworkInfo
            info != null && info.isConnected
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
