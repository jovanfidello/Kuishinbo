package com.example.kuishinbo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class UpdateUserInfoActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference

    private lateinit var selectedImageUri: Uri

    private lateinit var nameEditText: EditText
    private lateinit var countryAutoCompleteTextView: AutoCompleteTextView
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
        countryAutoCompleteTextView = findViewById(R.id.country_edit_text)
        profilePhotoView = findViewById(R.id.profile_photo_view)
        val saveButton = findViewById<Button>(R.id.save_button)
        val changePhotoButton = findViewById<Button>(R.id.change_photo_button)

        // Set up AutoCompleteTextView for country
        val countries = resources.getStringArray(R.array.countries) // Load country list from string resources
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, countries)
        countryAutoCompleteTextView.setAdapter(adapter)

        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name")
                        val country = document.getString("country")
                        val photoProfileUrl = document.getString("photoProfileUrl")
                        val timestamp = document.getTimestamp("timestamp")?.toDate()
                        nameEditText.setText(name)
                        countryAutoCompleteTextView.setText(country) // Set existing country data
                        photoProfileUrl?.let {
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
            val country = countryAutoCompleteTextView.text.toString()
            if (name.isNotEmpty() && country.isNotEmpty()) {
                if (::selectedImageUri.isInitialized) {
                    uploadImageToFirebase(name, country)
                } else {
                    saveUserInfo(name, country, null)
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
            Glide.with(this).load(selectedImageUri).into(profilePhotoView)
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImageToFirebase(name: String, country: String) {
        val userId = auth.currentUser?.uid
        val fileRef = storageRef.child("profile_photos/$userId.jpg")

        fileRef.putFile(selectedImageUri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    saveUserInfo(name, country, uri.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserInfo(name: String, country: String, photoProfileUrl: String?) {
        val userData = hashMapOf(
            "name" to name,
            "country" to country
        )
        photoProfileUrl?.let { userData["photoProfileUrl"] = it }

        auth.currentUser?.let { user ->
            db.collection("users").document(user.uid).set(userData, SetOptions.merge())
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
