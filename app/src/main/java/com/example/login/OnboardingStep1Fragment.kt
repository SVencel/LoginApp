package com.example.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class OnboardingStep1Fragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_onboarding_step1, container, false)

        val btnNext = view.findViewById<Button>(R.id.btnNextStep1)

        btnNext.setOnClickListener {
            // Skip data handling – for design preview only
            (activity as? OnboardingQuestionsActivity)?.goToNext(OnboardingStep2Fragment())
        }

        return view
    }
}
