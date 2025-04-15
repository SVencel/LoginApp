package com.example.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class OnboardingQuestionsActivity : AppCompatActivity() {

    private lateinit var skipButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding_questions)

        updateProgress(0)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, OnboardingStep1Fragment())
                .commit()
        }

        skipButton = findViewById(R.id.btnSkipOnboarding)
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
        // Just go to main activity without saving anything in design mode
        goToMain()
    }

    fun updateProgress(currentStep: Int) {
        val progressBar = findViewById<ProgressBar>(R.id.onboardingProgressBar)
        progressBar.progress = currentStep + 1
        skipButton.visibility = if (currentStep == 2) Button.GONE else Button.VISIBLE
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
