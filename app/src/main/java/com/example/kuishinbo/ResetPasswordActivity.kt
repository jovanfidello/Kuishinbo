package com.example.kuishinbo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

class ResetPasswordActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var successTextView: TextView
    private lateinit var backToLoginTextView: TextView
    private lateinit var backButtonImageButton: ImageButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        auth = FirebaseAuth.getInstance()

        emailEditText = findViewById(R.id.email_edit_text)
        successTextView = findViewById(R.id.success_text_view)
        backToLoginTextView = findViewById(R.id.back_to_login_text_view)
        backButtonImageButton = findViewById(R.id.back_button)
        // Hide success message and back to login option initially
        successTextView.visibility = View.GONE
        backToLoginTextView.visibility = View.GONE

        // Send password reset email
        findViewById<Button>(R.id.send_otp_button).setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                checkIfEmailExists(email)
            } else {
                Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show()
            }
        }

        // Back to login action
        backToLoginTextView.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        backButtonImageButton.setOnClickListener {
            finish()
        }

    }

    private fun checkIfEmailExists(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Check your email for the reset link.", Toast.LENGTH_SHORT).show()
                    displaySuccessMessage()
                } else {
                    val exception = task.exception
                    if (exception != null) {
                        val errorCode = (exception as FirebaseAuthException).errorCode
                        when (errorCode) {
                            "ERROR_USER_NOT_FOUND" -> {
                                Toast.makeText(this, "Email is not registered.", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Error: ${exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun displaySuccessMessage() {
        val mainMessage = "An email has been sent with instructions to change your password."
        val additionalMessage = " If you do not receive the password reset link email, please check if the email is correct or if it is registered."

        // Create a SpannableString to apply styles
        val spannableString = SpannableString(mainMessage + additionalMessage)

        // Change color of the additional message
        val start = mainMessage.length
        val end = start + additionalMessage.length
        spannableString.setSpan(ForegroundColorSpan(Color.RED), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        successTextView.text = spannableString
        successTextView.visibility = View.VISIBLE
        backToLoginTextView.visibility = View.VISIBLE
    }
}