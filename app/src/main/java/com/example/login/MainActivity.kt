package com.example.login

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.login.fragments.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNavigationView)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener {
            val selectedFragment = when (it.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_focus -> FocusFragment()
                R.id.nav_progress -> ProgressFragment()
                R.id.nav_friends -> FriendsFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> null
            }

            selectedFragment?.let { fragment ->
                loadFragment(fragment)
                true
            } ?: false
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
