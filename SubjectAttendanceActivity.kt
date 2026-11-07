package com.example.attendancechecker

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class SubjectAttendanceActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subject_attendance)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val subjectTitle =
            findViewById<TextView>(R.id.subjectTitle)

        val presentText =
            findViewById<TextView>(R.id.presentText)

        val absentText =
            findViewById<TextView>(R.id.absentText)

        val totalText =
            findViewById<TextView>(R.id.totalText)

        val rateText =
            findViewById<TextView>(R.id.rateText)

        val attendanceDetails =
            findViewById<TextView>(R.id.attendanceDetails)

        val currentUser = auth.currentUser

        if (currentUser == null) {
            attendanceDetails.text = "You are not logged in."
            return
        }

        val subject =
            intent.getStringExtra("SUBJECT")

        if (subject.isNullOrEmpty()) {
            subjectTitle.text = "Unknown Subject"
            attendanceDetails.text = "No subject selected."
            return
        }

        subjectTitle.text = subject

        val userId = currentUser.uid

        db.collection("attendance")
            .whereEqualTo("userID", userId)
            .whereEqualTo("subject", subject)
            .get()
            .addOnSuccessListener { documents ->

                var presentCount = 0
                var absentCount = 0

                val result = StringBuilder()

                if (documents.isEmpty) {

                    presentText.text = "Present: 0"
                    absentText.text = "Absent: 0"
                    totalText.text = "Total Classes: 0"
                    rateText.text = "Attendance Rate: 0%"
                    attendanceDetails.text =
                        "No attendance records found."

                    return@addOnSuccessListener
                }

                result.append("ATTENDANCE HISTORY\n\n")

                for (document in documents) {

                    val status =
                        document.getString("status")
                            ?: "Unknown"

                    val timestamp =
                        document.getTimestamp("Date")

                    if (status.equals(
                            "Present",
                            ignoreCase = true
                        )
                    ) {
                        presentCount++
                    }

                    if (status.equals(
                            "Absent",
                            ignoreCase = true
                        )
                    ) {
                        absentCount++
                    }

                    result.append(
                        "Status: $status\n"
                    )

                    if (timestamp != null) {

                        val formatter =
                            SimpleDateFormat(
                                "MMMM dd, yyyy • hh:mm a",
                                Locale.getDefault()
                            )

                        result.append(
                            "Date: ${
                                formatter.format(
                                    timestamp.toDate()
                                )
                            }\n"
                        )

                    } else {

                        result.append(
                            "Date: Unknown\n"
                        )
                    }

                    result.append(
                        "\n--------------------\n\n"
                    )
                }

                val totalClasses =
                    presentCount + absentCount

                val attendanceRate =
                    if (totalClasses > 0) {
                        (presentCount.toDouble() /
                                totalClasses.toDouble()) * 100
                    } else {
                        0.0
                    }

                presentText.text =
                    "Present: $presentCount"

                absentText.text =
                    "Absent: $absentCount"

                totalText.text =
                    "Total Classes: $totalClasses"

                rateText.text =
                    "Attendance Rate: ${
                        String.format(
                            Locale.getDefault(),
                            "%.1f",
                            attendanceRate
                        )
                    }%"

                attendanceDetails.text =
                    result.toString()
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