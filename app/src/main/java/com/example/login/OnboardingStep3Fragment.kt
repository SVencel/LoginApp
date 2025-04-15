package com.example.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class OnboardingStep3Fragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_onboarding_step3, container, false)

        val btnFinish = view.findViewById<Button>(R.id.btnFinishOnboarding)

        btnFinish.setOnClickListener {
            // Skip logic and simulate onboarding completion
            (activity as? OnboardingQuestionsActivity)?.finishOnboarding()

        }

        return view
    }
}
