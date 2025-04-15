package com.example.login

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity

class ForgotPasswordActivity : ComponentActivity() {

    private lateinit var resetPasswordButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val emailEditText: EditText = findViewById(R.id.etResetEmail)
        resetPasswordButton = findViewById(R.id.btnResetPassword)
        val backToLoginButton: Button = findViewById(R.id.btnBackToLogin)

        resetPasswordButton.setOnClickListener {
            showToast("🛠️ Design mode: Simulating password reset")
        }

        backToLoginButton.setOnClickListener {
            finish() // Return to login
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
