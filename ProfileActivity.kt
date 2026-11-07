package com.example.attendancechecker

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val nameText = findViewById<TextView>(R.id.nameText)
        val studentIdText = findViewById<TextView>(R.id.studentIdText)
        val emailText = findViewById<TextView>(R.id.emailText)
        val roleText = findViewById<TextView>(R.id.roleText)
        val backButton = findViewById<Button>(R.id.backButton)

        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(
                this,
                "No user is logged in.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        val uid = currentUser.uid

        // Get the student's profile from Firestore
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val name = document.getString("name") ?: "Not set"
                    val studentId =
                        document.getString("studentId") ?: "Not set"
                    val email =
                        document.getString("email")
                            ?: currentUser.email
                            ?: "Not set"
                    val role =
                        document.getString("role") ?: "Not set"

                    nameText.text = "Name: $name"
                    studentIdText.text = "Student ID: $studentId"
                    emailText.text = "Email: $email"
                    roleText.text = "Role: $role"

                } else {

                    Toast.makeText(
                        this,
                        "Student profile not found.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Error loading profile: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

        backButton.setOnClickListener {
            finish()
        }
    }
}