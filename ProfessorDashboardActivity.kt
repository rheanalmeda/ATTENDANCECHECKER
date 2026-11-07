
package com.example.attendancechecker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.jvm.java

class ProfessorDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_professor_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val welcomeText =
            findViewById<TextView>(R.id.professorWelcomeText)

        val selectClassSubjectButton =
            findViewById<Button>(R.id.selectClassSubjectButton)

        val attendanceReportsButton =
            findViewById<Button>(R.id.attendanceReportsButton)

        val logoutButton =
            findViewById<Button>(R.id.professorLogoutButton)

        // Display professor's name from Firestore
        val user = auth.currentUser

        if (user != null) {
            db.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    val name = document.getString("Name")
                        ?: document.getString("name")
                        ?: "Professor"

                    welcomeText.text = "Welcome, $name!"
                }
                .addOnFailureListener {
                    welcomeText.text = "Welcome, Professor!"
                }
        }

        // Select class and subject
        selectClassSubjectButton.setOnClickListener {
            startActivity(
                Intent(this, SelectClassSubjectActivity::class.java)
            )
        }

        // Attendance reports
        attendanceReportsButton.setOnClickListener {
            startActivity(
                Intent(this, AttendanceReportsActivity::class.java)
            )
        }

        // Logout
        logoutButton.setOnClickListener {
            auth.signOut()

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
