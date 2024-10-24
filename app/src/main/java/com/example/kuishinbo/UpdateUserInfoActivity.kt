package com.example.kuishinbo

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UpdateUserInfoActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_user_info)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val nameEditText = findViewById<EditText>(R.id.name_edit_text)
        val cityEditText = findViewById<EditText>(R.id.city_edit_text)
        val saveButton = findViewById<Button>(R.id.save_button)

        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name")
                        val city = document.getString("city")
                        nameEditText.setText(name)
                        cityEditText.setText(city)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error fetching data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val city = cityEditText.text.toString()
            if (name.isNotEmpty() && city.isNotEmpty()) {
                val userData = hashMapOf(
                    "name" to name,
                    "city" to city
                )

                db.collection("users").document(user!!.uid).set(userData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Information updated", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error updating information: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}