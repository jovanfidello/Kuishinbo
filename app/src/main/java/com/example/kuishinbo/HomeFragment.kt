package com.example.kuishinbo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.google.android.gms.maps.model.Marker
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
                    // Handle the case when location is null
                    Toast.makeText(context, "Unable to get user's location", Toast.LENGTH_SHORT).show()
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

    private fun fetchUserPlacesMarkers() {
        val user = auth.currentUser
        if (user != null) {
            db.collection("memories")
                .whereEqualTo("userId", user.uid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        // Ambil location dari document sebagai Map<String, Double>
                        val locationMap = document.get("location") as? Map<String, Double>
                        // Ambil name dari document
                        val name = document.getString("placeName")

                        // Pastikan locationMap tidak null dan name ada
                        locationMap?.let { location ->
                            val latLng = LatLng(
                                location["latitude"] ?: 0.0,  // Ambil latitude, default ke 0.0
                                location["longitude"] ?: 0.0   // Ambil longitude, default ke 0.0
                            )

                            // Tambahkan marker ke peta
                            googleMap?.addMarker(
                                MarkerOptions()
                                    .position(latLng)  // Posisi marker menggunakan latLng
                                    .title(name ?: "Unknown Place")  // Set title dengan nama tempat, atau "Unknown Place" jika null
                            )
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Menampilkan toast jika terjadi kegagalan saat mengambil data
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