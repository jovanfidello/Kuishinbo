package com.example.kuishinbo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EntryUserInfoActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var profileImageView: ImageView
    private lateinit var uploadImageButton: Button
    private lateinit var usernameInput: EditText
    private lateinit var domicileInput: EditText
    private lateinit var saveButton: Button

    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry_user_info)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        profileImageView = findViewById(R.id.profile_image_view)
        uploadImageButton = findViewById(R.id.upload_image_button)
        usernameInput = findViewById(R.id.username_input)
        domicileInput = findViewById(R.id.domicile_input)
        saveButton = findViewById(R.id.save_button)

        val user = auth.currentUser

        uploadImageButton.setOnClickListener {
            openGallery()
        }

        saveButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val domicile = domicileInput.text.toString()

            if (username.isEmpty() || domicile.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if the username is taken
            db.collection("users").whereEqualTo("username", username).get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        saveUserInfo(user!!.uid, username, domicile)
                    } else {
                        Toast.makeText(this, "Username is already taken", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error checking username", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Opens the gallery to select an image
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 1000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000 && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            profileImageView.setImageURI(selectedImageUri)
        }
    }

    // Save user info to Firestore and upload profile picture
    // Save user info to Firestore and upload profile picture
    private fun saveUserInfo(userId: String, username: String, domicile: String) {
        val userRef = db.collection("users").document(userId)

        // Upload profile picture if available
        if (selectedImageUri != null) {
            val storageRef = storage.reference.child("profile_pictures/$userId.jpg")
            storageRef.putFile(selectedImageUri!!).addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val userData = hashMapOf(
                        "username" to username,
                        "domicile" to domicile,
                        "profilePictureUrl" to uri.toString(),
                        "registrationDate" to FieldValue.serverTimestamp() // Add registration date
                    )
                    userRef.set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "User info saved", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error saving user info", Toast.LENGTH_SHORT).show()
                        }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error uploading profile picture", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Save info without profile picture
            val userData = hashMapOf(
                "username" to username,
                "domicile" to domicile,
                "registrationDate" to FieldValue.serverTimestamp() // Add registration date
            )
            userRef.set(userData)
                .addOnSuccessListener {
                    Toast.makeText(this, "User info saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error saving user info", Toast.LENGTH_SHORT).show()
                }
        }
    }

}
