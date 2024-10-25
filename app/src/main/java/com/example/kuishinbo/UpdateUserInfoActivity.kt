package com.example.kuishinbo

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class UpdateUserInfoActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference

    private lateinit var selectedImageUri: Uri

    private lateinit var nameEditText: EditText
    private lateinit var cityEditText: EditText
    private lateinit var profilePhotoView: ImageView

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_user_info)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        nameEditText = findViewById(R.id.name_edit_text)
        cityEditText = findViewById(R.id.city_edit_text)
        profilePhotoView = findViewById(R.id.profile_photo_view)
        val saveButton = findViewById<Button>(R.id.save_button)
        val changePhotoButton = findViewById<Button>(R.id.change_photo_button)

        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name")
                        val city = document.getString("city")
                        val photoProfileUrl = document.getString("photoProfileUrl") // Updated variable name

                        nameEditText.setText(name)
                        cityEditText.setText(city)
                        photoProfileUrl?.let {
                            // Use Glide to load the image
                            Glide.with(this).load(it).into(profilePhotoView)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error fetching data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        changePhotoButton.setOnClickListener {
            openFileChooser()
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val city = cityEditText.text.toString()
            if (name.isNotEmpty() && city.isNotEmpty()) {
                if (::selectedImageUri.isInitialized) {
                    uploadImageToFirebase(name, city)
                } else {
                    saveUserInfo(name, city, null)
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openFileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data!!
            // Load the image into the ImageView (profilePhotoView)
            Glide.with(this).load(selectedImageUri).into(profilePhotoView) // Use Glide to load the selected image
        }
    }

    private fun uploadImageToFirebase(name: String, city: String) {
        val userId = auth.currentUser?.uid
        val fileRef = storageRef.child("profile_photos/$userId.jpg")

        fileRef.putFile(selectedImageUri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    saveUserInfo(name, city, uri.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserInfo(name: String, city: String, photoProfileUrl: String?) { // Updated parameter name
        val userData = hashMapOf(
            "name" to name,
            "city" to city
        )
        photoProfileUrl?.let { userData["photoProfileUrl"] = it }

        auth.currentUser?.let { user ->
            db.collection("users").document(user.uid).set(userData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Information updated", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating information: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
