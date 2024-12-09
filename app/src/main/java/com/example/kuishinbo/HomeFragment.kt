package com.example.kuishinbo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment(), OnMapReadyCallback {
    private var googleMap: GoogleMap? = null
    private lateinit var infoWindow: MaterialCardView
    private lateinit var titleText: TextView
    private lateinit var seeMemoriesLink: TextView
    private lateinit var profileCard: MaterialCardView
    private lateinit var profileImageView: ImageView
    private var locationPermissionGranted = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var hasShownLocationToast = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        infoWindow = view.findViewById(R.id.infoWindow)
        titleText = view.findViewById(R.id.titleText)
        seeMemoriesLink = view.findViewById(R.id.seeMemoriesLink)
        profileCard = view.findViewById(R.id.profileCard)
        profileImageView = view.findViewById(R.id.profileImage)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Check location permission and get user's current location
        checkLocationPermission()

        // Setup map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Fetch user data for profile image
        loadProfileImage()

        // Setup profile click using navigation from MainActivity
        profileCard.setOnClickListener {
            (activity as? MainActivity)?.navigateToProfileFragment()
        }

        // Setup see memories click
        seeMemoriesLink.setOnClickListener {
            (activity as? MainActivity)?.navigateToMemoriesFragment()
        }
    }

    private fun checkLocationPermission() {
        // Check if location permission is granted
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, get user's current location
            locationPermissionGranted = true
            getUserLocation()
        } else {
            // Request location permission
            requestLocationPermission()
        }
    }

    private fun getUserLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    // Update the map's camera position to the user's current location
                    val userLocation = LatLng(location.latitude, location.longitude)
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                } else {
                    // Display the toast only once
                    if (!hasShownLocationToast) {
                        Toast.makeText(
                            context,
                            "For Better Experience, Turn On Your Location Feature",
                            Toast.LENGTH_SHORT
                        ).show()
                        hasShownLocationToast = true // Update the flag to prevent repeated toasts
                    }
                }
            }
        } catch (e: SecurityException) {
            // Handle the case when permission is not granted
            Toast.makeText(
                context,
                "Error: Location permission is required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun requestLocationPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        setupMap()

        // Setup custom info window
        googleMap?.setOnMarkerClickListener { marker ->
            titleText.text = marker.title
            infoWindow.visibility = View.VISIBLE
            true
        }

        // Add sample markers
        fetchUserPlacesMarkers()

        // Hide info window when clicking on map
        googleMap?.setOnMapClickListener {
            infoWindow.visibility = View.GONE
        }

        // Zoom to user's location if permission is granted
        checkLocationPermission()
    }

    private fun setupMap() {
        googleMap?.apply {
            try {
                uiSettings.apply {
                    isZoomControlsEnabled = true
                    isCompassEnabled = true
                }

                // Update location features based on permission
                if (locationPermissionGranted) {
                    isMyLocationEnabled = true
                    uiSettings.isMyLocationButtonEnabled = true
                } else {
                    isMyLocationEnabled = false
                    uiSettings.isMyLocationButtonEnabled = false
                }
            } catch (e: SecurityException) {
                Toast.makeText(
                    context,
                    "Error: Location permission is required",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadProfileImage() {
        val user = auth.currentUser
        if (user != null) {
            val userRef = db.collection("users").document(user.uid)
            userRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val photoProfileUrl = document.getString("photoProfileUrl")
                        photoProfileUrl?.let {
                            Glide.with(this)
                                .load(it)
                                .placeholder(R.drawable.ic_profile_placeholder) // Placeholder if no image
                                .into(profileImageView)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error loading profile image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Define fetchAdditionalData outside of fetchUserPlacesMarkers
    private fun fetchAdditionalData(selectedPlaceName: String, callback: (String?) -> Unit) {
        // Simulate a network/database call
        Handler(Looper.getMainLooper()).postDelayed({
            // Simulate a response with some additional data
            val additionalData = "Some additional data for $selectedPlaceName"
            callback(additionalData)
        }, 100)
    }

    private fun fetchUserPlacesMarkers() {
        val user = auth.currentUser
        if (user != null) {
            db.collection("memories")
                .whereEqualTo("userId", user.uid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        // Extract location from document as Map<String, Double>
                        val locationMap = document.get("location") as? Map<String, Double>
                        // Get name from document
                        val name = document.getString("placeName")

                        // Get other relevant details
                        val imageUrl = document.getString("imageUrl")
                        val placeName = document.getString("placeName")
                        val description = document.getString("description")
                        val rating = document.getDouble("rating")?.toFloat() ?: 0f
                        val timestamp = document.getTimestamp("timestamp")

                        // Ensure locationMap is not null and name exists
                        locationMap?.let { location ->
                            val latLng = LatLng(
                                location["latitude"] ?: 0.0,
                                location["longitude"] ?: 0.0
                            )

                            // Add marker with all necessary information
                            val marker = googleMap?.addMarker(
                                MarkerOptions()
                                    .position(latLng)
                                    .title(name ?: "Unknown Place")
                            )
                            // Store additional data as a tag with the marker
                            marker?.tag = Bundle().apply {
                                putString("imageUrl", imageUrl ?: "")
                                putString("placeName", placeName ?: "")
                                putString("description", description ?: "")
                                putFloat("rating", rating)
                                putLong("timestamp", timestamp?.seconds ?: 0)
                            }

                        }
                    }

                    // Set a custom info window click listener
                    googleMap?.setOnInfoWindowClickListener { marker ->
                        val markerData = marker.tag as? Bundle

                        // Check if markerData is null or incomplete
                        if (markerData == null) {
                            Log.e("HomeFragment", "Marker data is null. Cannot navigate to MemoriesFragment.")
                            return@setOnInfoWindowClickListener // Correct here as it’s not part of a loop, it’s just returning from lambda
                        }

                        val selectedPlaceName = markerData.getString("placeName")
                        val imageUrl = markerData.getString("imageUrl")

                        // Check if the selectedPlaceName is missing
                        if (selectedPlaceName.isNullOrEmpty()) {
                            Log.e("HomeFragment", "Required marker data (placeName) is missing.")
                            return@setOnInfoWindowClickListener
                        }

                        // Wait for data to be fully available before navigating
                        fetchAdditionalData(selectedPlaceName) { additionalData ->
                            // Create the MemoriesFragment and pass data as arguments
                            val memoriesFragment = MemoriesFragment().apply {
                                arguments = Bundle().apply {
                                    putString("selectedPlaceName", selectedPlaceName)
                                    putString("imageUrl", imageUrl ?: "") // Default to empty string if imageUrl is null
                                    putString("additionalData", additionalData) // Pass any additional data if necessary
                                }
                            }

                            // Perform fragment transaction with custom animations
                            (activity as? MainActivity)?.supportFragmentManager?.beginTransaction()
                                ?.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                                ?.replace(R.id.fragment_container, memoriesFragment)
                                ?.addToBackStack(null)
                                ?.commit()
                        }
                    }

                }
                .addOnFailureListener { e ->
                    // Show toast if there's an error fetching places
                    Toast.makeText(
                        context,
                        "Failed to fetch places: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        googleMap = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}