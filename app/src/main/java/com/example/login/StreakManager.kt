package com.example.login.utils

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

object StreakManager {

    fun saveTodayStreak(streakCount: Int) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val streakData = mapOf(
            "date" to today,
            "streak" to streakCount,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(user.uid)
            .collection("streakHistory")
            .document(today)  // one document per day
            .set(streakData)
    }
}
