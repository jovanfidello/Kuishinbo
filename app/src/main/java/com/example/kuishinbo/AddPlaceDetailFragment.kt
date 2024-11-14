package com.example.kuishinbo

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class AddPlaceDetailFragment : Fragment() {

    private lateinit var nameEditText: TextInputEditText
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var ratingBar: RatingBar
    private lateinit var locationInfoTextView: TextView
    private lateinit var locationButton: Button
    private lateinit var addToMemoriesButton: Button
    private lateinit var locationImageView: ImageView

    private var selectedLocation: LatLng? = null
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var mapSnapshot: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_place_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationInfoTextView = view.findViewById(R.id.locationInfoTextView)
        locationButton = view.findViewById(R.id.locationButton)
        locationImageView = view.findViewById(R.id.locationImageView)

        locationImageView.visibility = View.GONE

        locationButton.setOnClickListener {
            // Navigate to AddPlaceFragment to set the location
            (activity as? MainActivity)?.navigateToAddPlaceFragment()
        }

        // Listen for both the location and snapshot data
        parentFragmentManager.setFragmentResultListener("location_result", viewLifecycleOwner) { _, bundle ->
            selectedLocation = bundle.getParcelable("location") as? LatLng
            mapSnapshot = bundle.getParcelable("snapshot")

            // Update location information
            locationInfoTextView.text = "Location set: ${selectedLocation?.latitude}, ${selectedLocation?.longitude}"
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

        }
    }


    // This function will load the map snapshot into the ImageView
    private fun loadMapSnapshot() {
        mapSnapshot?.let {
            locationImageView.setImageBitmap(it) // Set the bitmap into the ImageView
        }
    }

    private fun uploadImage(filePath: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(activity, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val loadingDialog = AlertDialog.Builder(requireContext())
            .setMessage("Uploading image...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        try {
            val bitmap = BitmapFactory.decodeFile(filePath)
            if (bitmap == null) {
                loadingDialog.dismiss()
                Toast.makeText(activity, "Failed to load image", Toast.LENGTH_SHORT).show()
                return
            }

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageData = baos.toByteArray()

            val timestamp = System.currentTimeMillis()
            val fileName = "IMG_${timestamp}.jpg"
            val storageRef = storage.reference.child("places/${currentUser.uid}/$fileName")

            val uploadTask = storageRef.putBytes(imageData)
            uploadTask
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        savePlaceData(downloadUri.toString())
                        loadingDialog.dismiss()
                    }
                }
                .addOnFailureListener { e ->
                    loadingDialog.dismiss()
                    Toast.makeText(activity, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("AddPlaceDetailFragment", "Upload failed", e)
                }
        } catch (e: Exception) {
            loadingDialog.dismiss()
            Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("AddPlaceDetailFragment", "Error in uploadImage", e)
        }
    }

    private fun savePlaceData(imageUrl: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        val place = hashMapOf(
            "userId" to currentUser.uid,
            "name" to nameEditText.text.toString(),
            "description" to descriptionEditText.text.toString(),
            "rating" to ratingBar.rating,
            "location" to selectedLocation?.let { mapOf("latitude" to it.latitude, "longitude" to it.longitude) },
            "imageUrl" to imageUrl,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        db.collection("places")
            .add(place)
            .addOnSuccessListener {
                Toast.makeText(activity, "Place added successfully", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack() // Navigate back
            }
            .addOnFailureListener { e ->
                Toast.makeText(activity, "Failed to save place: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("AddPlaceDetailFragment", "Failed to save place", e)
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
