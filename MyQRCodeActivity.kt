package com.example.attendancechecker

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

class MyQRCodeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_qr_code)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val qrImageView =
            findViewById<ImageView>(R.id.qrImageView)

        val studentNameText =
            findViewById<TextView>(R.id.studentNameText)

        val studentClassText =
            findViewById<TextView>(R.id.studentClassText)

        val backButton =
            findViewById<Button>(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }

        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(
                this,
                "You are not logged in.",
                Toast.LENGTH_LONG
            ).show()

            finish()
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
                        "Student profile not found.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                val name =
                    document.getString("name")
                        ?: "Student"

                val classSection =
                    document.getString("classSection")
                        ?: "Class not assigned"

                studentNameText.text = name
                studentClassText.text = classSection

                // The QR contains the student's Firebase UID.
                val qrData = userId

                generateQRCode(
                    qrData,
                    qrImageView
                )
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Error loading profile: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun generateQRCode(
        data: String,
        imageView: ImageView
    ) {
        try {

            val bitMatrix: BitMatrix =
                MultiFormatWriter().encode(
                    data,
                    BarcodeFormat.QR_CODE,
                    800,
                    800
                )

            val width = bitMatrix.width
            val height = bitMatrix.height

            val bitmap =
                Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.RGB_565
                )

            for (x in 0 until width) {
                for (y in 0 until height) {

                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) {
                            Color.BLACK
                        } else {
                            Color.WHITE
                        }
                    )
                }
            }

            imageView.setImageBitmap(bitmap)

        } catch (exception: Exception) {

            Toast.makeText(
                this,
                "Unable to generate QR code.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
