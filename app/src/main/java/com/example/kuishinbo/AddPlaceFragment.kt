package com.example.kuishinbo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AddPlaceFragment : Fragment(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private var selectedLocationMarker: Marker? = null
    private var locationPermissionGranted = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_place, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Request location permission if needed
        checkLocationPermission()

        // Initialize map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Confirm button to submit the selected location
        val confirmButton = view.findViewById<FloatingActionButton>(R.id.confirm_button)
        confirmButton.setOnClickListener {
            if (selectedLocationMarker != null) {
                // Proceed with saving or using the selected location
                Toast.makeText(context, "Location confirmed: ${selectedLocationMarker!!.position}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Please select a location on the map", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                locationPermissionGranted = true
                updateMapWithPermission()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(context, "Location permission is needed to select a place on the map", Toast.LENGTH_LONG).show()
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            }
            else -> {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
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
                Toast.makeText(context, "Location permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        setupMap()

        // Handle map click for selecting a place
        googleMap?.setOnMapClickListener { latLng ->
            selectedLocationMarker?.remove() // Remove previous marker if any
            selectedLocationMarker = googleMap?.addMarker(
                MarkerOptions().position(latLng).title("Selected Location")
            )
        }
    }

    private fun setupMap() {
        googleMap?.apply {
            uiSettings.isZoomControlsEnabled = true

            // Move the camera to a default location (e.g., user's last known location or a popular location)
            val defaultLocation = LatLng(-6.2088, 106.8456) // Jakarta
            moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))

            // Update location-related features based on permission status
            updateMapWithPermission()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            locationPermissionGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            updateMapWithPermission()
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
