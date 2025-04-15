package com.example.login

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val usernameEditText: EditText = findViewById(R.id.etUsername)
        val emailEditText: EditText = findViewById(R.id.etRegisterEmail)
        val passwordEditText: EditText = findViewById(R.id.etRegisterPassword)
        registerButton = findViewById(R.id.btnRegisterUser)
        val backToLoginButton: Button = findViewById(R.id.btnBackToLogin)

        registerButton.setOnClickListener {
            startActivity(Intent(this, OnboardingQuestionsActivity::class.java))
        }

        backToLoginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
