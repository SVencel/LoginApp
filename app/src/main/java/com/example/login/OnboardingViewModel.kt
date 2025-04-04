package com.example.login

import androidx.lifecycle.ViewModel

class OnboardingViewModel : ViewModel() {

    // Stores all answers with key-value pairs
    private val answers = mutableMapOf<String, Any>()

    // Set an answer to a specific question key
    fun setAnswer(questionKey: String, answer: Any) {
        answers[questionKey] = answer
    }

    // Retrieve a specific answer if needed
    fun getAnswer(questionKey: String): Any? {
        return answers[questionKey]
    }

    // Retrieve the full map of answers
    fun getAllAnswers(): Map<String, Any> {
        return answers.toMap()
    }

    // Optional: clear all stored answers
    fun reset() {
        answers.clear()
    }
}
