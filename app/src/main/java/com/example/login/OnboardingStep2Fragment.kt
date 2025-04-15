package com.example.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class OnboardingStep2Fragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_onboarding_step2, container, false)

        val btnNext = view.findViewById<Button>(R.id.btnNextStep2)

        btnNext.setOnClickListener {
            // Skip logic and go straight to next step
            (activity as? OnboardingQuestionsActivity)?.goToNext(OnboardingStep3Fragment())
        }

        return view
    }
}
