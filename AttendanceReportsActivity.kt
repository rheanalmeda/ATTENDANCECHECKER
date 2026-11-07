package com.example.attendancechecker

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AttendanceReportsActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var studentList: LinearLayout
    private lateinit var summaryText: TextView
    private lateinit var saveButton: Button
    private lateinit var backButton: Button

    private var sessionId = ""
    private var selectedClass = ""
    private var selectedSubject = ""
    private var professorUid = ""

    private val absentSelections =
        mutableMapOf<String, CheckBox>()

    private var enrolledStudents =
        mutableListOf<StudentRecord>()

    private var presentIds = mutableSetOf<String>()
    private var existingIds = mutableSetOf<String>()

    data class StudentRecord(
        val uid: String,
        val name: String,
        val email: String,
        val studentId: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_reports)

        studentList = findViewById(R.id.studentList)
        summaryText = findViewById(R.id.summaryText)
        saveButton = findViewById(R.id.saveAbsencesButton)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }

        val prefs = getSharedPreferences(
            "AttendanceChecker",
            MODE_PRIVATE
        )

        sessionId = prefs.getString("sessionId", "") ?: ""
        selectedClass = prefs.getString("classSection", "") ?: ""
        selectedSubject = prefs.getString("subject", "") ?: ""
        professorUid = auth.currentUser?.uid ?: ""

        if (sessionId.isBlank() ||
            selectedClass.isBlank() ||
            selectedSubject.isBlank() ||
            professorUid.isBlank()
        ) {
            Toast.makeText(
                this,
                "Select a class and subject and start attendance first.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        title = "Attendance Reports"

        findViewById<TextView>(R.id.reportTitle).text =
            "$selectedClass - $selectedSubject"

        saveButton.setOnClickListener {
            saveAbsences()
        }

        loadSessionAndStudents()
    }

    private fun loadSessionAndStudents() {
        saveButton.isEnabled = false

        db.collection("attendanceSessions")
            .document(sessionId)
            .get()
            .addOnSuccessListener { session ->

                if (!session.exists() ||
                    session.getString("professorID") != professorUid ||
                    session.getString("classSection") != selectedClass ||
                    session.getString("subject") != selectedSubject ||
                    session.getString("status") != "in_progress"
                ) {
                    showMessage(
                        "This attendance session is unavailable or already completed."
                    )
                    finish()
                    return@addOnSuccessListener
                }

                loadAttendanceRecords()
            }
            .addOnFailureListener {
                showMessage("Unable to verify attendance session.")
            }
    }

    private fun loadAttendanceRecords() {
        db.collection("attendance")
            .whereEqualTo("sessionId", sessionId)
            .get()
            .addOnSuccessListener { records ->

                presentIds.clear()
                existingIds.clear()

                for (record in records.documents) {
                    val uid = record.getString("userID") ?: continue

                    existingIds.add(uid)

                    if (record.getString("status")
                            ?.equals("Present", true) == true
                    ) {
                        presentIds.add(uid)
                    }
                }

                loadEnrolledStudents()
            }
            .addOnFailureListener {
                showMessage("Unable to load attendance records.")
            }
    }

    private fun loadEnrolledStudents() {
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->

                enrolledStudents.clear()

                for (document in documents.documents) {
                    val role = (
                            document.getString("role")
                                ?: document.getString("Role")
                            )?.trim()?.lowercase() ?: "student"

                    if (role != "student") continue

                    val studentClass = document.getString("classSection")
                        ?: document.getString("ClassSection")
                        ?: document.getString("class")
                        ?: ""

                    if (normalizeClass(studentClass) != normalizeClass(selectedClass)) {
                        continue
                    }

                    val subjects = (
                            document.get("subjects")
                                ?: document.get("Subjects")
                            ) as? List<*>

                    val enrolled = if (subjects.isNullOrEmpty()) {
                        true
                    } else {
                        subjects
                            ?.filterIsInstance<String>()
                            ?.any {
                                it.trim().equals(
                                    selectedSubject,
                                    ignoreCase = true
                                )
                            } ?: true
                    }

                    if (!enrolled) continue

                    enrolledStudents.add(
                        StudentRecord(
                            uid = document.id,
                            name = document.getString("name")
                                ?: document.getString("Name")
                                ?: "Unnamed Student",
                            email = document.getString("email")
                                ?: document.getString("Email")
                                ?: "",
                            studentId = document.getString("studentId")
                                ?: document.getString("StudentID")
                                ?: "N/A"
                        )
                    )
                }

                enrolledStudents.sortBy { it.name.lowercase() }

                displayStudents()
            }
            .addOnFailureListener {
                showMessage("Unable to load enrolled students.")
            }
    }

    private fun normalizeClass(value: String): String {
        return value.replace("\\s+".toRegex(), "").lowercase()
    }

    private fun displayStudents() {
        studentList.removeAllViews()
        absentSelections.clear()

        var presentCount = 0
        var absentCount = 0

        for (student in enrolledStudents) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 12, 12, 12)
            }

            val checkBox = CheckBox(this)

            checkBox.text =
                "${student.name} (${student.studentId})"

            checkBox.isChecked = false

            when {
                student.uid in presentIds -> {
                    presentCount++
                    checkBox.isEnabled = false
                    checkBox.text =
                        "${student.name} (${student.studentId}) - PRESENT"
                }

                student.uid in existingIds -> {
                    absentCount++
                    checkBox.isEnabled = false
                    checkBox.text =
                        "${student.name} (${student.studentId}) - ABSENT"
                }

                else -> {
                    absentSelections[student.uid] = checkBox
                    checkBox.text =
                        "${student.name} (${student.studentId}) - Mark absent"
                }
            }

            row.addView(checkBox)
            studentList.addView(row)
        }

        summaryText.text =
            "Class: $selectedClass\n" +
                    "Subject: $selectedSubject\n" +
                    "Enrolled: ${enrolledStudents.size}\n" +
                    "Present: $presentCount\n" +
                    "Already absent: $absentCount\n" +
                    "Not yet recorded: ${
                        enrolledStudents.size - presentCount - absentCount
                    }"

        saveButton.isEnabled = true

        if (enrolledStudents.isEmpty()) {
            showMessage(
                "No students enrolled in this class and subject."
            )
        }
    }

    private fun saveAbsences() {
        saveButton.isEnabled = false

        val selectedStudents = enrolledStudents.filter { student ->
            student.uid !in existingIds &&
                    absentSelections[student.uid]?.isChecked == true
        }

        if (selectedStudents.isEmpty()) {
            showMessage("Select at least one student to mark absent.")
            saveButton.isEnabled = true
            return
        }

        val batch = db.batch()

        for (student in selectedStudents) {
            val attendanceRef = db.collection("attendance")
                .document("${sessionId}_${student.uid}")

            val data = hashMapOf(
                "userID" to student.uid,
                "email" to student.email,
                "status" to "Absent",
                "subject" to selectedSubject,
                "Date" to FieldValue.serverTimestamp(),
                "classSection" to selectedClass,
                "professorID" to professorUid,
                "sessionId" to sessionId
            )

            batch.set(attendanceRef, data)
        }

        batch.commit()
            .addOnSuccessListener {
                showMessage("Absent students saved successfully.")
                loadAttendanceRecords()
            }
            .addOnFailureListener { exception ->
                showMessage(
                    "Failed to save absences: ${exception.message}"
                )
                saveButton.isEnabled = true
            }
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
