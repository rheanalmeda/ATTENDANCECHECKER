package com.example.attendancechecker

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class QRScannerActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var selectedClass: String
    private lateinit var selectedSubject: String
    private lateinit var sessionId: String

    private var professorUid: String = ""
    private var professorName: String = ""
    private var isScanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        selectedClass = intent.getStringExtra("classSection") ?: ""
        selectedSubject = intent.getStringExtra("subject") ?: ""
        sessionId = intent.getStringExtra("sessionId") ?: ""

        val professor = auth.currentUser

        if (professor == null ||
            selectedClass.isBlank() ||
            selectedSubject.isBlank() ||
            sessionId.isBlank()
        ) {
            Toast.makeText(
                this,
                "Missing login, class, subject, or session information.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        professorUid = professor.uid

        verifyProfessorAndStart()
    }

    private fun verifyProfessorAndStart() {
        db.collection("users")
            .document(professorUid)
            .get()
            .addOnSuccessListener { document ->

                if (isFinishing || isDestroyed) return@addOnSuccessListener

                val role = (
                        document.getString("Role")
                            ?: document.getString("role")
                        )?.trim()?.lowercase()

                if (!document.exists() || role != "professor") {
                    showMessage("This account is not authorized as a professor.")
                    finish()
                    return@addOnSuccessListener
                }

                val assignedSubjects =
                    (document.get("Subjects") as? List<*>)
                        ?.filterIsInstance<String>()
                        ?.map { it.trim() }
                        ?: emptyList()

                val subjectIsAssigned = assignedSubjects.any {
                    it.equals(selectedSubject, ignoreCase = true)
                }

                if (!subjectIsAssigned) {
                    showMessage("This subject is not assigned to your account.")
                    finish()
                    return@addOnSuccessListener
                }

                professorName =
                    document.getString("Name")
                        ?: document.getString("name")
                                ?: "Professor"

                showMessage(
                    "Attendance started: $selectedClass - $selectedSubject"
                )

                scanNextStudent()
            }
            .addOnFailureListener { exception ->
                if (isFinishing || isDestroyed) return@addOnFailureListener

                showMessage(
                    "Unable to verify professor: ${exception.message}"
                )
                finish()
            }
    }

    private fun scanNextStudent() {
        if (isFinishing || isDestroyed || isScanning) return

        isScanning = true

        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        val scanner = GmsBarcodeScanning.getClient(this, options)

        scanner.startScan()
            .addOnSuccessListener { barcode ->

                isScanning = false

                if (isFinishing || isDestroyed) return@addOnSuccessListener

                val studentUid = barcode.rawValue?.trim()

                if (studentUid.isNullOrBlank()) {
                    showMessage("The QR code contains no data.")
                    scanNextStudent()
                    return@addOnSuccessListener
                }

                processStudentQr(studentUid)
            }
            .addOnCanceledListener {
                isScanning = false
                finish()
            }
            .addOnFailureListener { exception ->

                isScanning = false

                if (isFinishing || isDestroyed) return@addOnFailureListener

                showMessage(
                    "QR scanner error: ${exception.message}"
                )
                finish()
            }
    }

    private fun processStudentQr(studentUid: String) {

        db.collection("users")
            .document(studentUid)
            .get()
            .addOnSuccessListener { student ->

                if (isFinishing || isDestroyed) return@addOnSuccessListener

                if (!student.exists()) {
                    showMessage("Student profile not found. Scan another QR code.")
                    scanNextStudent()
                    return@addOnSuccessListener
                }

                val studentRole = (
                        student.getString("role")
                            ?: student.getString("Role")
                        )?.trim()?.lowercase()

                if (studentRole != "student") {
                    showMessage("This QR code does not belong to a student.")
                    scanNextStudent()
                    return@addOnSuccessListener
                }

                val studentClass = student.getString("classSection") ?: ""

                if (normalizeClass(studentClass) != normalizeClass(selectedClass)) {
                    showMessage(
                        "Wrong class. This student belongs to $studentClass."
                    )
                    scanNextStudent()
                    return@addOnSuccessListener
                }

                val studentSubjects =
                    (student.get("subjects") as? List<*>)
                        ?.filterIsInstance<String>()
                        ?: emptyList()

                val isEnrolled = studentSubjects.any {
                    it.trim().equals(selectedSubject, ignoreCase = true)
                }

                if (!isEnrolled) {
                    showMessage(
                        "This student is not assigned to $selectedSubject."
                    )
                    scanNextStudent()
                    return@addOnSuccessListener
                }

                val email = student.getString("email")
                    ?: student.getString("Email")
                    ?: ""

                saveAttendance(
                    studentUid = studentUid,
                    studentEmail = email
                )
            }
            .addOnFailureListener { exception ->

                if (isFinishing || isDestroyed) return@addOnFailureListener

                showMessage(
                    "Unable to verify student: ${exception.message}"
                )
                scanNextStudent()
            }
    }

    private fun saveAttendance(
        studentUid: String,
        studentEmail: String
    ) {
        // One attendance document per student per session.
        val attendanceDocId = "${sessionId}_$studentUid"

        val attendanceRef = db.collection("attendance")
            .document(attendanceDocId)

        db.runTransaction { transaction ->

            val existingRecord = transaction.get(attendanceRef)

            if (existingRecord.exists()) {
                false
            } else {
                val attendanceData = hashMapOf(
                    "userID" to studentUid,
                    "email" to studentEmail,
                    "status" to "Present",
                    "subject" to selectedSubject,
                    "Date" to FieldValue.serverTimestamp(),
                    "classSection" to selectedClass,
                    "professorID" to professorUid,
                    "professorName" to professorName,
                    "sessionId" to sessionId
                )

                transaction.set(attendanceRef, attendanceData)
                true
            }
        }
            .addOnSuccessListener { wasSaved ->

                if (isFinishing || isDestroyed) return@addOnSuccessListener

                if (wasSaved) {
                    showMessage("Attendance recorded successfully.")
                } else {
                    showMessage("This student is already marked present in this session.")
                }

                // Continue scanning students in the same session.
                scanNextStudent()
            }
            .addOnFailureListener { exception ->

                if (isFinishing || isDestroyed) return@addOnFailureListener

                showMessage(
                    "Failed to save attendance: ${exception.message}"
                )
                scanNextStudent()
            }
    }

    private fun normalizeClass(value: String): String {
        return value.replace("\\s+".toRegex(), "").uppercase()
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
