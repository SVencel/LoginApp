package com.example.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class OnboardingStep2Fragment : Fragment() {

    private lateinit var viewModel: OnboardingViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_onboarding_step2, container, false)
        viewModel = ViewModelProvider(requireActivity())[OnboardingViewModel::class.java]

        val etAppUsed = view.findViewById<EditText>(R.id.etAppUsed)
        val rgLoseTrack = view.findViewById<RadioGroup>(R.id.rgLoseTrack)
        val rgSleepImpact = view.findViewById<RadioGroup>(R.id.rgSleepImpact)
        val btnNext = view.findViewById<Button>(R.id.btnNextStep2)

        // ✅ Handle keyboard "Done" press to hide keyboard
        etAppUsed.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(etAppUsed.windowToken, 0)
                etAppUsed.clearFocus()
                true
            } else {
                false
            }
        }

        btnNext.setOnClickListener {
            val appUsed = etAppUsed.text.toString()
            val loseTrack = getSelectedText(rgLoseTrack)
            val sleepImpact = getSelectedText(rgSleepImpact)

            viewModel.setAnswer("most_used_app", appUsed)
            viewModel.setAnswer("loses_track_of_time", loseTrack)
            viewModel.setAnswer("sleep_affected_by_phone", sleepImpact)

            (activity as? OnboardingQuestionsActivity)?.goToNext(OnboardingStep3Fragment())
        }

        return view
    }

    private fun getSelectedText(radioGroup: RadioGroup): String {
        val selectedId = radioGroup.checkedRadioButtonId
        return if (selectedId != -1) {
            radioGroup.findViewById<RadioButton>(selectedId)?.text?.toString() ?: ""
        } else {
            ""
        }
    }
}
