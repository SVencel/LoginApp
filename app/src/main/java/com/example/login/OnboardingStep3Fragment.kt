package com.example.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class OnboardingStep3Fragment : Fragment() {

    private lateinit var viewModel: OnboardingViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_onboarding_step3, container, false)

        viewModel = ViewModelProvider(requireActivity())[OnboardingViewModel::class.java]

        val check1 = view.findViewById<CheckBox>(R.id.cbReasonLessPhone)
        val check2 = view.findViewById<CheckBox>(R.id.cbReasonBetterSleep)
        val check3 = view.findViewById<CheckBox>(R.id.cbReasonAddiction)
        val check4 = view.findViewById<CheckBox>(R.id.cbReasonRealLife)

        val etGoal = view.findViewById<EditText>(R.id.etGoalText)

        val btnFinish = view.findViewById<Button>(R.id.btnFinishOnboarding)
        btnFinish.setOnClickListener {
            val reasons = mutableListOf<String>()
            if (check1.isChecked) reasons.add(check1.text.toString())
            if (check2.isChecked) reasons.add(check2.text.toString())
            if (check3.isChecked) reasons.add(check3.text.toString())
            if (check4.isChecked) reasons.add(check4.text.toString())

            viewModel.setAnswer("why_downloaded", reasons)
            viewModel.setAnswer("goal_30_days", etGoal.text.toString())

            // ✅ Save to Firebase in the activity
            (activity as? OnboardingQuestionsActivity)?.finishOnboarding()
        }

        return view
    }
}
