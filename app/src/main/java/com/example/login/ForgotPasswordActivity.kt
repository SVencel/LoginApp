package com.example.login

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var resetPasswordButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        val emailEditText: EditText = findViewById(R.id.etResetEmail)
        resetPasswordButton = findViewById(R.id.btnResetPassword)
        val backToLoginButton: Button = findViewById(R.id.btnBackToLogin)

        resetPasswordButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            if (!isValidEmail(email)) {
                showToast("Enter a valid email")
                return@setOnClickListener
            }

            resetPassword(email)
        }

        backToLoginButton.setOnClickListener {
            finish()  // Closes the current activity and returns to login
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun resetPassword(email: String) {
        resetPasswordButton.isEnabled = false  // 🔹 Disable button to prevent multiple clicks
        resetPasswordButton.text = "Sending..."

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                resetPasswordButton.isEnabled = true  // 🔹 Re-enable button
                resetPasswordButton.text = "Reset Password"  // 🔹 Reset button text

                if (task.isSuccessful) {
                    showToast("Password reset email sent!")
                    finish()  // Close activity after success
                } else {
                    showToast("Error: ${task.exception?.message}")
                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
