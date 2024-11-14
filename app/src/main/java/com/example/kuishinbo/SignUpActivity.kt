package com.example.kuishinbo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Patterns

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val nameField = findViewById<EditText>(R.id.name)
        val countryField = findViewById<AutoCompleteTextView>(R.id.country)
        val emailField = findViewById<EditText>(R.id.email)
        val passwordField = findViewById<EditText>(R.id.password)
        val reconfirmPasswordField = findViewById<EditText>(R.id.reconfirm_password)
        val signupButton = findViewById<Button>(R.id.signup_button)
        val passwordToggle = findViewById<ImageView>(R.id.password_toggle)
        val reconfirmPasswordToggle = findViewById<ImageView>(R.id.reconfirm_password_toggle)
        val signInRedirect = findViewById<TextView>(R.id.signin_redirect_text)

        // Populate the location AutoCompleteTextView with Indonesian cities
        val cities = resources.getStringArray(R.array.countries)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cities)
        countryField.setAdapter(adapter)

        // Limit the dropdown to show only 5 items initially, rest scrollable
        countryField.dropDownHeight = 250 // Adjust as needed (in pixels)

        // Redirect to Login activity if the user already has an account
        signInRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Sign up button functionality
        signupButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            val reconfirmPassword = reconfirmPasswordField.text.toString()
            val name = nameField.text.toString()
            val country = countryField.text.toString()

            // Validate inputs
            if (name.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (country.isEmpty()) {
                Toast.makeText(this, "Location cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
                                val user = auth.currentUser

                                // Create user data map to save to Firestore
                                val userData = hashMapOf(
                                    "name" to name,
                                    "country" to country,
                                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()  // Add timestamp
                                )

                                // Save user data to Firestore in the 'users' collection, with a document named by user UID
                                val db = FirebaseFirestore.getInstance()
                                db.collection("users").document(user!!.uid)
                                    .set(userData)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
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


        // Toggle password visibility for password and reconfirm password
        passwordToggle.setOnClickListener {
            if (passwordField.inputType == 129) {
                passwordField.inputType = 1
                passwordToggle.setImageResource(R.drawable.ic_visible_password)
            } else {
                passwordField.inputType = 129
                passwordToggle.setImageResource(R.drawable.ic_invisible_password)
            }
            passwordField.setSelection(passwordField.text.length)
        }

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
