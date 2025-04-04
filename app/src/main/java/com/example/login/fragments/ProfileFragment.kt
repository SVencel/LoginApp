package com.example.login.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.login.AddFriendActivity
import com.example.login.CreateSectionActivity
import com.example.login.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var thresholdInput: EditText
    private lateinit var saveButton: Button
    private lateinit var friendCountText: TextView
    private lateinit var addFriendButton: Button
    private lateinit var sectionButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        thresholdInput = view.findViewById(R.id.etCustomThreshold)
        saveButton = view.findViewById(R.id.btnSaveThreshold)
        friendCountText = view.findViewById(R.id.tvFriendCount)
        addFriendButton = view.findViewById(R.id.btnAddFriends)
        sectionButton = view.findViewById(R.id.btnManageSections)

        saveButton.setOnClickListener {
            val minutes = thresholdInput.text.toString().toIntOrNull()
            if (minutes == null || minutes < 1) {
                Toast.makeText(requireContext(), "Enter a number ≥ 5", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = FirebaseAuth.getInstance().currentUser ?: return@setOnClickListener
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid)
                .update("productivityPromptMinutes", minutes)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Saved! You’ll be reminded every $minutes minutes", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to save threshold", Toast.LENGTH_SHORT).show()
                }
        }

        addFriendButton.setOnClickListener {
            startActivity(Intent(requireContext(), AddFriendActivity::class.java))
        }

        sectionButton.setOnClickListener {
            startActivity(Intent(requireContext(), CreateSectionActivity::class.java))
        }

        loadExistingValue()
        loadFriendCount()

        return view
    }

    private fun loadExistingValue() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val value = document.getLong("productivityPromptMinutes")?.toInt()
                if (value != null) {
                    thresholdInput.setText(value.toString())
                }
            }
    }

    private fun loadFriendCount() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val friends = document.get("friends") as? List<*>
                val count = friends?.size ?: 0
                friendCountText.text = "Friends: $count"
            }
    }
}
