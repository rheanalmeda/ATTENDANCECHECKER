package com.example.attendancechecker

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SubjectsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subjects)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val classText =
            findViewById<TextView>(R.id.classText)

        val subjectsContainer =
            findViewById<LinearLayout>(R.id.subjectsContainer)

        val backButton =
            findViewById<Button>(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }

        val currentUser = auth.currentUser

        if (currentUser == null) {
            return
        }

        val userId = currentUser.uid

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    Toast.makeText(
                        this,
                        "Student information not found.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                val classSection =
                    document.getString("classSection")
                        ?: "Class not assigned"

                classText.text = classSection

                val subjects =
                    document.get("subjects") as? List<*>

                if (subjects.isNullOrEmpty()) {

                    val noSubjectsText =
                        TextView(this)

                    noSubjectsText.text =
                        "No subjects assigned."

                    noSubjectsText.textSize = 18f
                    noSubjectsText.setTextColor(
                        Color.parseColor("#546E7A")
                    )

                    subjectsContainer.addView(
                        noSubjectsText
                    )

                    return@addOnSuccessListener
                }

                subjectsContainer.removeAllViews()

                subjects.forEach { subjectValue ->

                    val subject =
                        subjectValue.toString()

                    val button =
                        Button(this)

                    button.text = subject

                    button.textSize = 16f
                    button.setTypeface(null, Typeface.BOLD)

                    button.isAllCaps = false
                    button.setTextColor(Color.WHITE)
                    button.background =
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.green_rounded_button
                        )
                    button.backgroundTintList =
                        ColorStateList.valueOf(
                            Color.parseColor("#087A32")
                        )
                    button.stateListAnimator = null
                    button.elevation = 3f

                    val params =
                        LinearLayout.LayoutParams(
                            (320 * resources.displayMetrics.density).toInt(),
                            (64 * resources.displayMetrics.density).toInt()
                        )

                    params.setMargins(
                        0,
                        0,
                        0,
                        (18 * resources.displayMetrics.density).toInt()
                    )

                    button.layoutParams = params

                    button.setOnClickListener {

                        val intent =
                            Intent(
                                this,
                                SubjectAttendanceActivity::class.java
                            )

                        intent.putExtra(
                            "SUBJECT",
                            subject
                        )

                        startActivity(intent)
                    }

                    subjectsContainer.addView(button)
                }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Error loading subjects: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
