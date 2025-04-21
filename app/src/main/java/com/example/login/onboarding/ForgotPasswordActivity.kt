package com.example.login.onboarding

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.login.R
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

            if (!isInternetAvailable(this)) {
                showToast("No internet connection. Please check your network and try again.")
                return@setOnClickListener
            }

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

    /** ✅ CHECK IF EMAIL IS VALID **/
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /** ✅ RESET PASSWORD **/
    private fun resetPassword(email: String) {
        resetPasswordButton.isEnabled = false
        resetPasswordButton.text = "Checking..."

        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    if (signInMethods.isNullOrEmpty()) {
                        showToast("No account found with that email.")
                        resetPasswordButton.isEnabled = true
                        resetPasswordButton.text = "Reset Password"
                    } else {
                        // Email exists, send reset
                        auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { resetTask ->
                                resetPasswordButton.isEnabled = true
                                resetPasswordButton.text = "Reset Password"

                                if (resetTask.isSuccessful) {
                                    showToast("Password reset email sent!")
                                    finish()
                                } else {
                                    showToast("Error: ${resetTask.exception?.message}")
                                }
                            }
                    }
                } else {
                    showToast("Error checking email: ${task.exception?.message}")
                    resetPasswordButton.isEnabled = true
                    resetPasswordButton.text = "Reset Password"
                }
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
