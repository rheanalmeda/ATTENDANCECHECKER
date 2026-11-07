package com.example.attendancechecker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val nameEditText =
            findViewById<EditText>(R.id.nameEditText)

        val studentIdEditText =
            findViewById<EditText>(R.id.studentIdEditText)

        val emailEditText =
            findViewById<EditText>(R.id.emailEditText)

        val passwordEditText =
            findViewById<EditText>(R.id.passwordEditText)

        val classSectionEditText =
            findViewById<EditText>(R.id.classSectionEditText)

        val registerButton =
            findViewById<Button>(R.id.registerButton)

        val backToLoginTextView =
            findViewById<TextView>(
                R.id.backToLoginTextView
            )

        registerButton.setOnClickListener {

            val name =
                nameEditText.text.toString().trim()

            val studentId =
                studentIdEditText.text.toString().trim()

            val email =
                emailEditText.text.toString().trim()

            val password =
                passwordEditText.text.toString()

            val classSection =
                classSectionEditText.text.toString().trim()

            if (name.isEmpty()) {
                nameEditText.error = "Enter your full name"
                nameEditText.requestFocus()
                return@setOnClickListener
            }

            if (studentId.isEmpty()) {
                studentIdEditText.error = "Enter your student ID"
                studentIdEditText.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                emailEditText.error = "Enter your email"
                emailEditText.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                passwordEditText.error =
                    "Password must be at least 6 characters"
                passwordEditText.requestFocus()
                return@setOnClickListener
            }

            if (classSection.isEmpty()) {
                classSectionEditText.error =
                    "Enter your class / section"
                classSectionEditText.requestFocus()
                return@setOnClickListener
            }

            // Find subjects based on class
            val subjects = getSubjectsForClass(classSection)

            if (subjects == null) {

                classSectionEditText.error =
                    "Class / section not found"

                Toast.makeText(
                    this,
                    "Please enter a registered class such as BSIT - 3A, 3B, 3C, or 3D",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            registerButton.isEnabled = false

            auth.createUserWithEmailAndPassword(
                email,
                password
            )
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        val user =
                            auth.currentUser

                        if (user == null) {
                            registerButton.isEnabled = true
                            return@addOnCompleteListener
                        }

                        val userId = user.uid

                        val userData = hashMapOf(
                            "name" to name,
                            "studentId" to studentId,
                            "email" to email,
                            "role" to "student",
                            "classSection" to classSection,
                            "subjects" to subjects
                        )

                        db.collection("users")
                            .document(userId)
                            .set(userData)
                            .addOnSuccessListener {

                                Toast.makeText(
                                    this,
                                    "Registration successful! Automatically enrolled in $classSection.",
                                    Toast.LENGTH_LONG
                                ).show()

                                auth.signOut()

                                startActivity(
                                    Intent(
                                        this,
                                        MainActivity::class.java
                                    )
                                )

                                finish()
                            }
                            .addOnFailureListener { exception ->

                                registerButton.isEnabled = true

                                Toast.makeText(
                                    this,
                                    "Error saving profile: ${exception.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                    } else {

                        registerButton.isEnabled = true

                        Toast.makeText(
                            this,
                            "Registration failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        backToLoginTextView.setOnClickListener {

            finish()
        }
    }

    private fun getSubjectsForClass(
        classSection: String
    ): List<String>? {

        val normalized = classSection
            .trim()
            .uppercase()
            .replace("\\s+".toRegex(), "")

        return when {
            normalized.contains("BSIT3A") || normalized.contains("BSIT-3A") -> listOf(
                "Advanced Database Systems",
                "Web Systems and Technologies",
                "Networking 2",
                "Software Engineering",
                "Information Assurance and Security"
            )

            normalized.contains("BSIT3B") || normalized.contains("BSIT-3B") -> listOf(
                "Systems Integration and Architecture",
                "Quantitative Methods",
                "Information Assurance and Security",
                "Web Systems and Technologies",
                "Network Management"
            )

            normalized.contains("BSIT3C") || normalized.contains("BSIT-3C") -> listOf(
                "Applications Development and Emerging Technologies",
                "System Analysis and Design",
                "Human-Computer Interaction",
                "Information Assurance and Security",
                "Information Management"
            )

            normalized.contains("BSIT3D") || normalized.contains("BSIT-3D") -> listOf(
                "Mobile Application Development",
                "Human-Computer Interaction",
                "Information Assurance and Security",
                "Database Management Systems",
                "Systems Integration and Architecture"
            )

            normalized.startsWith("BSIT") -> listOf(
                "Mobile Application Development",
                "Human-Computer Interaction",
                "Information Assurance and Security",
                "Database Management Systems",
                "Systems Integration and Architecture"
            )

            else -> null
        }
    }
}
