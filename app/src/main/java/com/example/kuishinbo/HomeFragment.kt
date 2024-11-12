package com.example.kuishinbo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.card.MaterialCardView

class HomeFragment : Fragment(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private lateinit var infoWindow: MaterialCardView
    private lateinit var titleText: TextView
    private lateinit var seeMemoriesLink: TextView
    private var currentMarker: Marker? = null
    private var locationPermissionGranted = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
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

        // Check location permission first
        checkLocationPermission()

        // Setup map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Setup profile click menggunakan navigasi dari MainActivity
        view.findViewById<MaterialCardView>(R.id.profileCard).setOnClickListener {
            (activity as? MainActivity)?.navigateToProfileFragment()
        }

        // Setup see memories click
        seeMemoriesLink.setOnClickListener {
            (activity as? MainActivity)?.navigateToCalenderFragment()
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                locationPermissionGranted = true
                updateMapWithPermission()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Show an explanation to the user
                Toast.makeText(
                    context,
                    "Location permission is needed to show your location on the map",
                    Toast.LENGTH_LONG
                ).show()
                requestLocationPermission()
            }
            else -> {
                requestLocationPermission()
            }
        }
    }

    private fun updateMapWithPermission() {
        googleMap?.let { map ->
            try {
                if (locationPermissionGranted) {
                    map.isMyLocationEnabled = true
                    map.uiSettings.isMyLocationButtonEnabled = true
                } else {
                    map.isMyLocationEnabled = false
                    map.uiSettings.isMyLocationButtonEnabled = false
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                locationPermissionGranted = (grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED)
                updateMapWithPermission()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        setupMap()

        // Setup custom info window
        googleMap?.setOnMarkerClickListener { marker ->
            currentMarker = marker
            titleText.text = marker.title
            infoWindow.visibility = View.VISIBLE
            true
        }

        // Add sample markers
        addSampleMarkers()

        // Hide info window when clicking on map
        googleMap?.setOnMapClickListener {
            infoWindow.visibility = View.GONE
        }
    }

    private fun setupMap() {
        googleMap?.apply {
            try {
                uiSettings.apply {
                    isZoomControlsEnabled = true
                    isCompassEnabled = true
                }

                // Update location features based on permission
                updateMapWithPermission()

                // Set default location (replace with actual user location)
                val defaultLocation = LatLng(-6.2088, 106.8456) // Jakarta
                moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
            } catch (e: SecurityException) {
                Toast.makeText(
                    context,
                    "Error: Location permission is required",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun addSampleMarkers() {
        val locations = listOf(
            Pair(LatLng(-6.2088, 106.8456), "Bebek Biryani"),
            Pair(LatLng(-6.2100, 106.8470), "Favorite Restaurant"),
            Pair(LatLng(-6.2070, 106.8440), "Coffee Shop")
        )

        locations.forEach { (position, title) ->
            googleMap?.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(title)
            )
        }
    }

    private fun requestLocationPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        googleMap = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}