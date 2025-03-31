package com.example.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

class OnboardingStep1Fragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding_step1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val btnNext = view.findViewById<Button>(R.id.btnNextStep1)

        val rgPhoneHours = view.findViewById<RadioGroup>(R.id.rgPhoneHours)
        val rgTimeOfDay = view.findViewById<RadioGroup>(R.id.rgTimeOfDay)
        val rgMainReason = view.findViewById<RadioGroup>(R.id.rgMainReason)

        btnNext.setOnClickListener {
            val selectedHours = rgPhoneHours.checkedRadioButtonId
            val selectedTime = rgTimeOfDay.checkedRadioButtonId
            val selectedReason = rgMainReason.checkedRadioButtonId

            if (selectedHours == -1 || selectedTime == -1 || selectedReason == -1) {
                Toast.makeText(requireContext(), "Please answer all questions", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: Store answers using shared ViewModel or pass via Bundle

            //(activity as? OnboardingQuestionsActivity)?.goToNext(OnboardingStep2Fragment())
        }
    }
}
