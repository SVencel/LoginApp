package com.example.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

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

        findViewById<Button>(R.id.btnSkipOnboarding).setOnClickListener {
            goToMain()
        }

        skipButton.setOnClickListener {
            goToMain()
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

        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val userDocRef = db.collection("users").document(user.uid)

        userDocRef.collection("onboardingAnswers")
            .document("initialSurvey").set(answers)
            .addOnSuccessListener {
                Toast.makeText(this, "🎉 Answers saved successfully!", Toast.LENGTH_SHORT).show()

                // ✅ Launch HomeActivity and clear the backstack
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Failed to save answers.", Toast.LENGTH_SHORT).show()
            }
    }
    fun updateProgress(currentStep: Int) {
        val progressBar = findViewById<ProgressBar>(R.id.onboardingProgressBar)
        progressBar.progress = currentStep + 1  // Since progress bar starts from 0

    // ✅ Hide skip button on last step (step 2 here)
        skipButton.visibility = if (currentStep == 2) Button.GONE else Button.VISIBLE
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

}
