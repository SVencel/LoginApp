package com.example.login

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class OnboardingQuestionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding_questions)

        updateProgress(0)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, OnboardingStep1Fragment())
                .commit()
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
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Failed to save answers.", Toast.LENGTH_SHORT).show()
            }
    }
    fun updateProgress(currentStep: Int) {
        val steps = listOf(
            findViewById<TextView>(R.id.progressStep1),
            findViewById<TextView>(R.id.progressStep2),
            findViewById<TextView>(R.id.progressStep3)
        )

        steps.forEachIndexed { index, textView ->
            if (index == currentStep) {
                textView.text = "●"
                textView.setTextColor(getColor(R.color.tealColor)) // or your primary color
            } else {
                textView.text = "○"
                textView.setTextColor(getColor(android.R.color.darker_gray))
            }
        }
    }


}
