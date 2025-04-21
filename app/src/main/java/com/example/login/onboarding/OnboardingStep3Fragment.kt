package com.example.login.onboarding

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.login.OnboardingViewModel
import com.example.login.R

class OnboardingStep3Fragment : Fragment() {

    private lateinit var viewModel: OnboardingViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_onboarding_step3, container, false)
        viewModel = ViewModelProvider(requireActivity())[OnboardingViewModel::class.java]

        val check1 = view.findViewById<CheckBox>(R.id.cbReasonLessPhone)
        val check2 = view.findViewById<CheckBox>(R.id.cbReasonBetterSleep)
        val check3 = view.findViewById<CheckBox>(R.id.cbReasonAddiction)
        val check4 = view.findViewById<CheckBox>(R.id.cbReasonRealLife)

        val etGoal1 = view.findViewById<EditText>(R.id.etGoalText1)
        val etGoal2 = view.findViewById<EditText>(R.id.etGoalText2)
        val etGoal3 = view.findViewById<EditText>(R.id.etGoalText3)

        // Hide keyboard when user presses Done on last goal field
        etGoal3.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(etGoal3.windowToken, 0)
                etGoal3.clearFocus()
                true
            } else {
                false
            }
        }

        val btnFinish = view.findViewById<Button>(R.id.btnFinishOnboarding)
        btnFinish.setOnClickListener {
            val reasons = mutableListOf<String>()
            if (check1.isChecked) reasons.add(check1.text.toString())
            if (check2.isChecked) reasons.add(check2.text.toString())
            if (check3.isChecked) reasons.add(check3.text.toString())
            if (check4.isChecked) reasons.add(check4.text.toString())

            val goals = listOf(
                etGoal1.text.toString(),
                etGoal2.text.toString(),
                etGoal3.text.toString()
            ).filter { it.isNotBlank() }

            viewModel.setAnswer("why_downloaded", reasons)
            viewModel.setAnswer("goal_30_days", goals)

            (activity as? OnboardingQuestionsActivity)?.finishOnboarding()
        }

        return view
    }
}
