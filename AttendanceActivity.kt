package com.example.attendancechecker

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class AttendanceActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val attendanceSummary =
            findViewById<TextView>(R.id.attendanceSummary)

        val backButton =
            findViewById<Button>(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }

        val currentUser = auth.currentUser

        if (currentUser == null) {
            attendanceSummary.text = "You are not logged in."
            return
        }

        val userId = currentUser.uid

        db.collection("attendance")
            .whereEqualTo("userID", userId)
            .get()
            .addOnSuccessListener { documents ->

                if (documents.isEmpty) {

                    attendanceSummary.text =
                        "No attendance records found.\n\nUser UID:\n$userId"

                    return@addOnSuccessListener
                }

                val result = StringBuilder()

                result.append("MY ATTENDANCE\n\n")

                for (document in documents) {

                    val subject =
                        document.getString("subject")
                            ?: "Unknown Subject"

                    val timestamp =
                        document.getTimestamp("Date")

                    val status =
                        document.getString("status")
                            ?: "Unknown Status"

                    result.append("Subject: $subject\n")

                    if (timestamp != null) {

                        val formatter = SimpleDateFormat(
                            "MMMM dd, yyyy • hh:mm a",
                            Locale.getDefault()
                        )

                        val formattedDate =
                            formatter.format(timestamp.toDate())

                        result.append(
                            "Date & Time: $formattedDate\n"
                        )

                    } else {

                        result.append(
                            "Date & Time: Unknown\n"
                        )
                    }

                    result.append("Status: $status\n")
                    result.append(
                        "\n--------------------\n\n"
                    )
                }

                attendanceSummary.text = result.toString()
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Error loading attendance: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
