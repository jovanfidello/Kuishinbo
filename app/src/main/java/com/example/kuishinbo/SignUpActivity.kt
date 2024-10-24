package com.example.kuishinbo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.util.Patterns

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()

        val emailField = findViewById<EditText>(R.id.email)
        val passwordField = findViewById<EditText>(R.id.password)
        val reconfirmPasswordField = findViewById<EditText>(R.id.reconfirm_password)
        val signupButton = findViewById<Button>(R.id.signup_button)
        val passwordToggle = findViewById<ImageView>(R.id.password_toggle)
        val reconfirmPasswordToggle = findViewById<ImageView>(R.id.reconfirm_password_toggle)
        val signInRedirect = findViewById<TextView>(R.id.signin_redirect_text)

        signInRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        signupButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            val reconfirmPassword = reconfirmPasswordField.text.toString()

            // Validate inputs
            if (email.isEmpty()) {
                Toast.makeText(this, "Email cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                Toast.makeText(this, "Invalid email format.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "Password cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (reconfirmPassword.isEmpty()) {
                Toast.makeText(this, "Reconfirm password cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password == reconfirmPassword) {
                if (isValidPassword(password)) {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Password must contain at least 1 uppercase letter, 1 number, and be at least 8 characters long.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            }
        }

        // Toggle password visibility
        passwordToggle.setOnClickListener {
            if (passwordField.inputType == 129) { // 129 is the inputType for "textPassword"
                passwordField.inputType = 1 // 1 is for "text"
                passwordToggle.setImageResource(R.drawable.ic_visible_password) // Change icon to visible
            } else {
                passwordField.inputType = 129
                passwordToggle.setImageResource(R.drawable.ic_invisible_password) // Change icon to invisible
            }
            passwordField.setSelection(passwordField.text.length) // Move cursor to the end
        }

        // Toggle reconfirm password visibility
        reconfirmPasswordToggle.setOnClickListener {
            if (reconfirmPasswordField.inputType == 129) {
                reconfirmPasswordField.inputType = 1
                reconfirmPasswordToggle.setImageResource(R.drawable.ic_visible_password)
            } else {
                reconfirmPasswordField.inputType = 129
                reconfirmPasswordToggle.setImageResource(R.drawable.ic_invisible_password)
            }
            reconfirmPasswordField.setSelection(reconfirmPasswordField.text.length)
        }
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isDigit() }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}