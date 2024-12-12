package com.example.kuishinbo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference


class OtherSettingFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference

    private lateinit var selectedImageUri: Uri

    private lateinit var nameEditText: EditText
    private lateinit var countryAutoCompleteTextView: AutoCompleteTextView
    private lateinit var profilePhotoView: ImageView

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_other_setting, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        nameEditText = view.findViewById(R.id.name_edit_text)
        countryAutoCompleteTextView = view.findViewById(R.id.country_edit_text)
        profilePhotoView = view.findViewById(R.id.profile_photo_view)
        val saveButton = view.findViewById<Button>(R.id.save_button)
        val changePhotoButton = view.findViewById<Button>(R.id.change_photo_button)
        val changePasswordButton = view.findViewById<Button>(R.id.change_password_button)
        val deleteAccountButton = view.findViewById<Button>(R.id.delete_account_button)
        val backButton = view.findViewById<ImageButton>(R.id.back_button)

        // Set up AutoCompleteTextView for country
        val countries = resources.getStringArray(R.array.countries) // Load country list from string resources
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, countries)
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
                            Glide.with(requireContext()).load(it).into(profilePhotoView)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error fetching data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Back button functionality
        backButton.setOnClickListener {
            (activity as? MainActivity)?.navigateToSettingFragment()
        }

        changePhotoButton.setOnClickListener {
            openFileChooser()
        }

        changePasswordButton.setOnClickListener {
            startActivity(Intent(requireActivity(), ResetPasswordActivity::class.java))
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
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        deleteAccountButton.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }

        return view
    }

    private fun openFileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == -1 && data != null && data.data != null) { // Use -1 instead of RESULT_OK
            selectedImageUri = data.data!!
            Glide.with(requireContext()).load(selectedImageUri).into(profilePhotoView)
        } else {
            Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "Information updated", Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.navigateToSettingFragment()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error updating information: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showDeleteAccountConfirmationDialog() {
        // Show confirmation dialog before deleting the account
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Yes") { _, _ ->
                deleteUserAccount()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss() // Close dialog on "No"
            }
            .setCancelable(false) // Prevent dialog from being dismissed by tapping outside
            .show()
    }

    private fun deleteUserAccount() {
        val user = auth.currentUser

        if (user != null) {
            // Delete user's Firestore document
            db.collection("users").document(user.uid).delete()
                .addOnSuccessListener {
                    // After Firestore deletion, delete the user's Firebase Authentication account
                    user.delete()
                        .addOnSuccessListener {
                            // Successfully deleted the account
                            Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show()

                            // Navigate to login activity
                            val intent = Intent(requireActivity(), LoginActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                            requireActivity().finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Error deleting account: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error deleting data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "No user logged in", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView?.visibility = View.VISIBLE
    }
}
