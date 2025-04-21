package com.example.login.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.login.OnboardingViewModel
import com.example.login.R

class OnboardingStep1Fragment : Fragment() {

    private lateinit var viewModel: OnboardingViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_onboarding_step1, container, false)

        // 🔹 Get the shared ViewModel from the Activity
        viewModel = ViewModelProvider(requireActivity())[OnboardingViewModel::class.java]

        val btnNext = view.findViewById<Button>(R.id.btnNextStep1)

        btnNext.setOnClickListener {
            // Collect user answers from the RadioGroups
            val usageGroup = view.findViewById<RadioGroup>(R.id.rgPhoneUsage)
            val timeGroup = view.findViewById<RadioGroup>(R.id.rgTimeOfDay)
            val reasonGroup = view.findViewById<RadioGroup>(R.id.rgPhoneReason)

            val usageAnswer = getSelectedText(usageGroup)
            val timeAnswer = getSelectedText(timeGroup)
            val reasonAnswer = getSelectedText(reasonGroup)

            // Save answers to ViewModel
            viewModel.setAnswer("average_phone_usage", usageAnswer)
            viewModel.setAnswer("most_used_time_of_day", timeAnswer)
            viewModel.setAnswer("common_phone_reason", reasonAnswer)

            // Move to next screen
            (activity as? OnboardingQuestionsActivity)?.goToNext(OnboardingStep2Fragment())
        }

        return view
    }

    // Helper function to get text from selected RadioButton
    private fun getSelectedText(radioGroup: RadioGroup): String {
        val selectedId = radioGroup.checkedRadioButtonId
        return if (selectedId != -1) {
            radioGroup.findViewById<RadioButton>(selectedId)?.text?.toString() ?: ""
        } else {
            ""
        }
    }
}
