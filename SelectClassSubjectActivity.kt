package com.example.attendancechecker

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class SelectClassSubjectActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var classSpinner: Spinner
    private lateinit var subjectSpinner: Spinner
    private lateinit var continueButton: Button
    private lateinit var backButton: Button

    private val classes = listOf(
        "BSIT - 3A",
        "BSIT - 3B",
        "BSIT - 3C",
        "BSIT - 3D"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_class_subject)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        classSpinner = findViewById(R.id.classSpinner)
        subjectSpinner = findViewById(R.id.subjectSpinner)
        continueButton = findViewById(R.id.continueButton)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }

        val classAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            classes
        )

        classAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        classSpinner.adapter = classAdapter

        continueButton.isEnabled = false

        loadProfessorSubjects()

        continueButton.setOnClickListener {
            val selectedClass =
                classSpinner.selectedItem?.toString()

            val selectedSubject =
                subjectSpinner.selectedItem?.toString()

            if (selectedClass.isNullOrBlank() ||
                selectedSubject.isNullOrBlank()
            ) {
                Toast.makeText(
                    this,
                    "Please select a class and subject.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val professorUid = auth.currentUser?.uid

            if (professorUid.isNullOrBlank()) {
                Toast.makeText(
                    this,
                    "Please log in again.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Create a unique attendance session ID.
            val sessionId = UUID.randomUUID().toString()

            // Prepare the session data for Firestore.
            val sessionData = hashMapOf(
                "sessionId" to sessionId,
                "professorID" to professorUid,
                "classSection" to selectedClass,
                "subject" to selectedSubject,
                "startedAt" to FieldValue.serverTimestamp(),
                "status" to "in_progress"
            )

            continueButton.isEnabled = false

            // Create the attendance session.
            db.collection("attendanceSessions")
                .document(sessionId)
                .set(sessionData)
                .addOnSuccessListener {
                    if (isFinishing || isDestroyed) return@addOnSuccessListener

                    // Save the active session details locally.
                    getSharedPreferences(
                        "AttendanceChecker",
                        MODE_PRIVATE
                    ).edit()
                        .putString("sessionId", sessionId)
                        .putString("classSection", selectedClass)
                        .putString("subject", selectedSubject)
                        .apply()

                    // Open the professor's QR scanner.
                    val intent = Intent(
                        this,
                        QRScannerActivity::class.java
                    )

                    intent.putExtra("classSection", selectedClass)
                    intent.putExtra("subject", selectedSubject)
                    intent.putExtra("sessionId", sessionId)

                    startActivity(intent)
                    continueButton.isEnabled = true
                }
                .addOnFailureListener { exception ->
                    if (isFinishing || isDestroyed) return@addOnFailureListener

                    continueButton.isEnabled = true

                    Toast.makeText(
                        this,
                        "Unable to start attendance: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun loadProfessorSubjects() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                this,
                "Please log in again.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        continueButton.isEnabled = false

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (isFinishing || isDestroyed) {
                    return@addOnSuccessListener
                }

                val role = (
                        document.getString("Role")
                            ?: document.getString("role")
                        )?.trim()?.lowercase()

                if (!document.exists() || role != "professor") {
                    Toast.makeText(
                        this,
                        "Professor account not found or not authorized.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@addOnSuccessListener
                }

                // Professor subjects use the uppercase "Subjects" field.
                val subjects = (
                        document.get("Subjects")
                            ?: document.get("subjects")
                        ) as? List<*>

                val validSubjects = subjects
                    ?.filterIsInstance<String>()
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.distinct()
                    ?: emptyList()

                if (validSubjects.isEmpty()) {
                    Toast.makeText(
                        this,
                        "No subjects are assigned to your account.",
                        Toast.LENGTH_LONG
                    ).show()

                    subjectSpinner.adapter = null
                    continueButton.isEnabled = false
                    return@addOnSuccessListener
                }

                val subjectAdapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    validSubjects
                )

                subjectAdapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )

                subjectSpinner.adapter = subjectAdapter
                continueButton.isEnabled = true
            }
            .addOnFailureListener { exception ->
                if (isFinishing || isDestroyed) {
                    return@addOnFailureListener
                }

                continueButton.isEnabled = false

                Toast.makeText(
                    this,
                    "Failed to load subjects: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
