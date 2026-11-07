
package com.example.attendancechecker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerTextView = findViewById<TextView>(R.id.registerTextView)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

            if (email.isEmpty()) {
                emailEditText.error = "Enter your email"
                emailEditText.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordEditText.error = "Enter your password"
                passwordEditText.requestFocus()
                return@setOnClickListener
            }

            loginButton.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    if (!task.isSuccessful) {
                        loginButton.isEnabled = true

                        val error = task.exception

                        Log.e(
                            "AttendanceLogin",
                            "Firebase sign-in failed",
                            error
                        )

                        val message = when (error) {
                            is FirebaseNetworkException ->
                                "Network error. Check your internet connection."

                            is FirebaseAuthInvalidUserException ->
                                "This account may be disabled or unavailable."

                            is FirebaseAuthInvalidCredentialsException ->
                                "Firebase rejected the email or password. Check the student account in Firebase Authentication."

                            else ->
                                "Login failed: ${error?.localizedMessage ?: "Unknown error"}"
                        }

                        Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                        ).show()

                        return@addOnCompleteListener
                    }

                    val user = auth.currentUser

                    if (user == null) {
                        loginButton.isEnabled = true

                        Toast.makeText(
                            this,
                            "Unable to identify your account. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addOnCompleteListener
                    }

                    db.collection("users")
                        .document(user.uid)
                        .get()
                        .addOnSuccessListener { document ->

                            if (isFinishing || isDestroyed) {
                                return@addOnSuccessListener
                            }

                            if (!document.exists()) {
                                auth.signOut()
                                loginButton.isEnabled = true

                                Toast.makeText(
                                    this,
                                    "Your Firestore user profile was not found. Contact the administrator.",
                                    Toast.LENGTH_LONG
                                ).show()

                                return@addOnSuccessListener
                            }

                            val role = (
                                    document.getString("Role")
                                        ?: document.getString("role")
                                    )?.trim()?.lowercase()

                            when (role) {
                                "student" -> {
                                    startActivity(
                                        Intent(
                                            this,
                                            DashboardActivity::class.java
                                        )
                                    )
                                    finish()
                                }

                                "professor" -> {
                                    startActivity(
                                        Intent(
                                            this,
                                            ProfessorDashboardActivity::class.java
                                        )
                                    )
                                    finish()
                                }

                                else -> {
                                    auth.signOut()
                                    loginButton.isEnabled = true

                                    Toast.makeText(
                                        this,
                                        "Account role is missing or unauthorized.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                        .addOnFailureListener { exception ->

                            if (isFinishing || isDestroyed) {
                                return@addOnFailureListener
                            }

                            Log.e(
                                "AttendanceLogin",
                                "Failed to retrieve Firestore user profile",
                                exception
                            )

                            auth.signOut()
                            loginButton.isEnabled = true

                            Toast.makeText(
                                this,
                                "Could not load profile: ${exception.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
        }

        registerTextView.setOnClickListener {
            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }
    }
}
