package com.example.kuishinbo

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.kuishinbo.databinding.FragmentMemoriesBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*

class MemoriesFragment : Fragment() {

    private lateinit var backButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var photoViewer: ViewPager2
    private lateinit var photoDetails: TextView
    private lateinit var placeName: TextView
    private lateinit var placeDescription: TextView
    private lateinit var placeRating: RatingBar
    private lateinit var shareOptions: LinearLayout
    private lateinit var othersButton: ImageButton
    private val db = FirebaseFirestore.getInstance()
    private val memoriesCollection = db.collection("memories")

    private lateinit var memoryList: List<MemoryData> // List to hold the memory data

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentMemoriesBinding.inflate(inflater, container, false)
        val selectedDate = requireActivity().intent.getStringExtra("selected_date")
        val imageUrl = requireActivity().intent.getStringExtra("image_url")

        // Initialize UI components
        backButton = binding.backButton
        settingsButton = binding.settingsButton
        photoViewer = binding.photoViewer
        photoDetails = binding.photoDetails
        placeName = binding.placeName
        placeDescription = binding.placeDescription
        placeRating = binding.placeRating
        shareOptions = binding.shareOptions
        othersButton = binding.othersButton

        // Reverse swipe direction
        photoViewer.layoutDirection = View.LAYOUT_DIRECTION_LTR

        // Load memory data from Firebase
        loadMemoryData(selectedDate, imageUrl)

        // Settings button functionality
        settingsButton.setOnClickListener {
            (activity as? MainActivity)?.navigateToSettingFragment()
        }

        // Set back button click listener
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        return binding.root
    }

    private fun loadMemoryData(selectedDate: String?, imageUrl: String?) {
        val query = if (selectedDate != null) {
            // Filter memories based on selectedDate
            memoriesCollection
                .orderBy("timestamp", Query.Direction.ASCENDING)
        } else {
            // Load all memories if no selectedDate
            memoriesCollection.orderBy("timestamp", Query.Direction.ASCENDING)
        }

        query.get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    return@addOnSuccessListener
                }

                memoryList = documents.map { doc ->
                    val timestamp = doc.getTimestamp("timestamp") // Ensure it's a Timestamp object
                    val timestampString = timestamp?.toDate()?.let { formatDate(it) } ?: "Unknown"

                    MemoryData(
                        doc.getString("placeName") ?: "",
                        doc.getString("description") ?: "",
                        doc.getString("imageUrl") ?: "",
                        doc.getDouble("rating")?.toFloat() ?: 0f,
                        timestamp ?: Timestamp.now() // Store Timestamp object
                    )
                }

                // If imageUrl is given, show the photo and details for that date
                imageUrl?.let {
                    val selectedMemory = memoryList.find { memory -> memory.imageUrl == it }
                    selectedMemory?.let { memory ->
                        updateMemoryDetails(memory)
                    }
                }

                // If selectedDate is given, find the memory matching that date
                if (selectedDate != null) {
                    val selectedMemory = memoryList.find { memory ->
                        formatDate(memory.timestamp.toDate()).contains(selectedDate)
                    }
                    selectedMemory?.let { memory ->
                        updateMemoryDetails(memory)
                        val selectedPosition = memoryList.indexOf(memory)
                        photoViewer.setCurrentItem(selectedPosition)
                    }
                } else {
                    // If no selectedDate, show the first photo
                    updateMemoryDetails(memoryList[0])
                }

                // Set adapter with loaded data
                val photoAdapter = PhotoAdapter(requireContext(), memoryList)
                photoViewer.adapter = photoAdapter

                // Set share button listeners
                othersButton.setOnClickListener { shareToOthers() }

                // Listen for page changes to update details
                photoViewer.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        updateMemoryDetails(memoryList[position])
                    }
                })
            }
            .addOnFailureListener { exception ->
                // Handle errors
            }
    }

    // Helper function to format Date into String
    fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("dd MMMM yyyy 'at' hh:mm:ss", Locale("id", "ID"))
        return formatter.format(date)
    }

    private fun updateMemoryDetails(memory: MemoryData) {
        placeName.text = memory.placeName
        placeDescription.text = memory.description
        placeRating.rating = memory.rating
        photoDetails.text = memory.timestamp.toDate().let { formatDate(it) }
    }

    private fun shareToOthers() {
        val memory = memoryList[photoViewer.currentItem]
        val imageUrl = memory.imageUrl

        // Check if the image URL is valid and proceed with sharing
        if (imageUrl.isNotEmpty()) {
            // Launch a coroutine for background work
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Perform background work in IO dispatcher (for image processing and file I/O)
                    val bitmap = withContext(Dispatchers.IO) {
                        getBitmapFromUrl(imageUrl)
                    }

                    // Add watermark in background
                    val watermarkedBitmap = withContext(Dispatchers.IO) {
                        addWatermark(bitmap)
                    }

                    // Save the image to external storage in background
                    val savedImageUri = withContext(Dispatchers.IO) {
                        saveImageToExternalStorage(watermarkedBitmap)
                    }

                    // Create share intent
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, savedImageUri)
                        putExtra(Intent.EXTRA_TEXT, "Hey, check out my memories! #KuisinboApp\n" +
                                "Download the app here: https://play.google.com/store/apps/details?id=com.example.kuishinbo")
                        type = "image/*"
                    }

                    // Check if WhatsApp is installed
                    val packageManager = requireContext().packageManager
                    val whatsappIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, savedImageUri)
                        putExtra(Intent.EXTRA_TEXT, "Hey, check out my memories! #KuisinboApp")
                        type = "image/*"
                        setPackage("com.whatsapp")
                    }

                    // Check if Instagram is installed
                    val instagramIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, savedImageUri)
                        type = "image/*"
                        setPackage("com.instagram.android")
                    }

                    // Try sharing to WhatsApp first
                    if (isAppInstalled("com.whatsapp", packageManager)) {
                        startActivity(whatsappIntent)
                    }
                    // If Instagram is installed, try sharing to Instagram Stories
                    else if (isAppInstalled("com.instagram.android", packageManager)) {
                        startActivity(instagramIntent)
                    }
                    // If neither WhatsApp nor Instagram is installed, show the share chooser
                    else {
                        startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }

                } catch (e: Exception) {
                    e.printStackTrace()  // Handle any errors that occurred during background processing
                }
            }
        }
    }

    // Function to check if the app is installed
    private fun isAppInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    // Convert image URL to Bitmap
    private fun getBitmapFromUrl(imageUrl: String): Bitmap {
        // Load the image as a Bitmap (e.g., using Glide)
        val glideBitmap = Glide.with(requireContext()).asBitmap().load(imageUrl).submit().get()
        return glideBitmap
    }

    // Function to add watermark to the image
    private fun addWatermark(originalBitmap: Bitmap): Bitmap {
        val watermarkText = "Kuisinbo App"
        val width = originalBitmap.width
        val height = originalBitmap.height

        // Create a mutable bitmap to work with
        val watermarkedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(watermarkedBitmap)
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 80f
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Set the position for the watermark (bottom right corner)
        val xPos = width - 300f
        val yPos = height - 100f
        canvas.drawText(watermarkText, xPos, yPos, paint)

        return watermarkedBitmap
    }

    // Save image to external storage and return its Uri
    private fun saveImageToExternalStorage(bitmap: Bitmap): Uri {
        val externalStorageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val contentResolver = requireContext().contentResolver

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "watermarked_image.jpg")
            put(MediaStore.Images.Media.DISPLAY_NAME, "watermarked_image")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uri = contentResolver.insert(externalStorageUri, values) ?: return Uri.EMPTY
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }

        return uri
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

// Modify MemoryData to match your actual constructor
data class MemoryData(
    val placeName: String,
    val description: String,
    val imageUrl: String,
    val rating: Float,
    val timestamp: Timestamp // Ensure you store Timestamp, not String
)

