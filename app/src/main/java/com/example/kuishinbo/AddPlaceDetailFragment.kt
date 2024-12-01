package com.example.kuishinbo

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AddPlaceDetailFragment : Fragment() {

    companion object {
        private const val ARG_FILE_PATH = "file_path"

        fun newInstance(filePath: String): AddPlaceDetailFragment {
            val fragment = AddPlaceDetailFragment()
            val args = Bundle()
            args.putString(ARG_FILE_PATH, filePath)
            fragment.arguments = args
            return fragment
        }
    }
    private lateinit var nameEditText: TextInputEditText
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var ratingBar: RatingBar
    private lateinit var locationInfoTextView: TextView
    private lateinit var locationButton: Button
    private lateinit var addToMemoriesButton: Button
    private lateinit var locationImageView: ImageView
    private lateinit var filePath: String

    private var selectedLocation: LatLng? = null
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var mapSnapshot: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Ambil filePath dari arguments
        filePath = arguments?.getString(ARG_FILE_PATH) ?: ""
        return inflater.inflate(R.layout.fragment_add_place_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize all views
        nameEditText = view.findViewById(R.id.nameEditText)
        descriptionEditText = view.findViewById(R.id.descriptionEditText)
        ratingBar = view.findViewById(R.id.ratingBar)
        locationInfoTextView = view.findViewById(R.id.locationInfoTextView)
        locationButton = view.findViewById(R.id.locationButton)
        locationImageView = view.findViewById(R.id.locationImageView)
        addToMemoriesButton = view.findViewById(R.id.addToMemoriesButton)
        val filePath = arguments?.getString(ARG_FILE_PATH)
        locationImageView.visibility = View.GONE
        if (filePath != null) {
            locationButton.setOnClickListener {
                // Navigate to AddPlaceFragment to set the location
                (activity as? MainActivity)?.navigateToAddPlaceFragment()
            }

            // Listen for both the location and snapshot data
            parentFragmentManager.setFragmentResultListener(
                "location_result",
                viewLifecycleOwner
            ) { _, bundle ->
                selectedLocation = bundle.getParcelable("location") as? LatLng
                mapSnapshot = bundle.getParcelable("snapshot")

                // Update location information
                locationButton.text = "Change Location"

                // Show snapshot if available
                if (mapSnapshot != null) {
                    loadMapSnapshot()
                    locationImageView.visibility = View.VISIBLE
                } else {
                    Log.e("AddPlaceDetailFragment", "Snapshot is null")
                }
            }

            addToMemoriesButton.setOnClickListener {
                if (validateInputs() && mapSnapshot != null) {
                    uploadPhotoToStorageAndSaveData()
                }
            }
        }
    }

    // This function will load the map snapshot into the ImageView
    private fun loadMapSnapshot() {
        mapSnapshot?.let {
            locationImageView.setImageBitmap(it) // Set the bitmap into the ImageView
        }
    }

    private fun uploadPhotoToStorageAndSaveData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(activity, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Pastikan filePath berisi foto dari kamera
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(activity, "File not found", Toast.LENGTH_SHORT).show()
                return
            }

            val photoUri = Uri.fromFile(file)
            val storageRef = storage.reference.child("places/${currentUser.uid}/${file.name}")

            val uploadTask = storageRef.putFile(photoUri)
            uploadTask
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveToFirestore(downloadUri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(activity, "Photo upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("AddPlaceDetailFragment", "Photo upload failed", e)
                }
        } catch (e: Exception) {
            Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("AddPlaceDetailFragment", "Error in uploadPhotoToStorageAndSaveData", e)
        }
    }

    private fun saveToFirestore(imageUrl: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(activity, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        // Use the current time as a Firestore Timestamp
        val timestamp = Timestamp(System.currentTimeMillis() / 1000, 0) // Firestore Timestamp uses seconds, not milliseconds

        val placeData = hashMapOf(
            "userId" to currentUser.uid,
            "placeName" to nameEditText.text.toString(),
            "description" to descriptionEditText.text.toString(),
            "rating" to ratingBar.rating,
            "location" to selectedLocation?.let {
                mapOf(
                    "latitude" to it.latitude,
                    "longitude" to it.longitude
                )
            },
            "imageUrl" to imageUrl,
            "timestamp" to timestamp // Store Firestore Timestamp instead of formatted string
        )

        db.collection("memories")
            .add(placeData)
            .addOnSuccessListener {
                val intent = Intent(activity, SuccessAddActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(activity, "Failed to save data: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("AddPlaceDetailFragment", "Failed to save data", e)
            }
    }




    private fun validateInputs(): Boolean {
        val name = nameEditText.text.toString()
        val description = descriptionEditText.text.toString()

        if (name.isEmpty()) {
            Toast.makeText(activity, "Please enter a name for the place", Toast.LENGTH_SHORT).show()
            return false
        }

        if (description.isEmpty()) {
            Toast.makeText(activity, "Please enter a description", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedLocation == null) {
            Toast.makeText(activity, "Please set the location", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun showDiscardChangesDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setMessage("Are you sure you want to discard your changes?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .setNegativeButton("No", null)
            .create()
        dialog.show()
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
