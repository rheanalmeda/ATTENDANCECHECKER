package com.example.attendancechecker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val welcomeText =
            findViewById<TextView>(R.id.welcomeText)

        val classText =
            findViewById<TextView>(R.id.classText)

        val qrButton =
            findViewById<Button>(R.id.qrButton)

        val attendanceHistoryButton =
            findViewById<Button>(R.id.attendanceHistoryButton)

        val subjectsButton =
            findViewById<Button>(R.id.subjectsButton)

        val profileButton =
            findViewById<Button>(R.id.profileButton)

        val logoutButton =
            findViewById<Button>(R.id.logoutButton)

        val currentUser = auth.currentUser

        if (currentUser == null) {
            finish()
            return
        }

        val userId = currentUser.uid

        // Load student information from Firestore
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val name =
                        document.getString("name") ?: "Student"

                    val classSection =
                        document.getString("classSection")
                            ?: "Class not assigned"

                    welcomeText.text =
                        "Welcome, $name"

                    classText.text =
                        classSection

                } else {

                    welcomeText.text =
                        "Welcome, ${currentUser.email}"

                    classText.text =
                        "User document not found"
                }
            }
            .addOnFailureListener { exception ->

                welcomeText.text =
                    "Welcome, ${currentUser.email}"

                classText.text =
                    "Firestore error"

                Toast.makeText(
                    this,
                    "Error loading profile: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

        // My QR Code
        qrButton.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    MyQRCodeActivity::class.java
                )
            )
        }

        // My Attendance
        attendanceHistoryButton.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    AttendanceActivity::class.java
                )
            )
        }

        // My Subjects
        subjectsButton.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SubjectsActivity::class.java
                )
            )
        }

        // My Profile
        profileButton.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    ProfileActivity::class.java
                )
            )
        }

        // Logout
        logoutButton.setOnClickListener {

            auth.signOut()

            Toast.makeText(
                this,
                "Logged out successfully",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(
                this,
                MainActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
        }
    }
}
