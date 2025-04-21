package com.example.login.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.login.MainActivity
import com.example.login.OnboardingViewModel
import com.example.login.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OnboardingQuestionsActivity : AppCompatActivity() {

    private lateinit var skipButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding_questions)

        skipButton = findViewById(R.id.btnSkipOnboarding)

        updateProgress(0)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, OnboardingStep1Fragment())
                .commit()
        }

        skipButton.setOnClickListener {
            val username = intent.getStringExtra("username") ?: ""
            val email = intent.getStringExtra("email") ?: ""
            createUserAndProceedToMain(username, email)
        }
    }

    fun goToNext(fragment: Fragment) {
        val nextStep = when (fragment) {
            is OnboardingStep1Fragment -> 0
            is OnboardingStep2Fragment -> 1
            is OnboardingStep3Fragment -> 2
            else -> 0
        }

        updateProgress(nextStep)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun finishOnboarding() {
        val viewModel = ViewModelProvider(this)[OnboardingViewModel::class.java]
        val answers = viewModel.getAllAnswers()

        val username = intent.getStringExtra("username") ?: ""
        val email = intent.getStringExtra("email") ?: ""
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        val userMap = hashMapOf(
            "username" to username,
            "email" to email,
            "friends" to listOf<String>(),
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(user.uid)
            .set(userMap)
            .addOnSuccessListener {
                // Save onboarding answers
                db.collection("users").document(user.uid)
                    .collection("onboardingAnswers")
                    .document("initialSurvey")
                    .set(answers)
                    .addOnSuccessListener {
                        Toast.makeText(this, "🎉 Profile + answers saved!", Toast.LENGTH_SHORT).show()

                        getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                            .edit().putBoolean("isLoggedIn", true).apply()

                        startActivity(Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "❌ Failed to save answers.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Failed to create user profile.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createUserAndProceedToMain(username: String, email: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        val userMap = hashMapOf(
            "username" to username,
            "email" to email,
            "friends" to listOf<String>(),
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(user.uid)
            .set(userMap)
            .addOnSuccessListener {
                getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                    .edit().putBoolean("isLoggedIn", true).apply()

                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Failed to create user profile.", Toast.LENGTH_SHORT).show()
            }
    }

    fun updateProgress(currentStep: Int) {
        val progressBar = findViewById<ProgressBar>(R.id.onboardingProgressBar)
        progressBar.progress = currentStep + 1
        skipButton.visibility = if (currentStep == 2) Button.GONE else Button.VISIBLE
    }
}
