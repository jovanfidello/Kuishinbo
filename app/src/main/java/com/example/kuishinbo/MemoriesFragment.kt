package com.example.kuishinbo

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
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
    private lateinit var downloadButton: ImageButton
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
        downloadButton = binding.downloadButton

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
                downloadButton.setOnClickListener { downloadImage(memoryList[0].imageUrl) }

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
        // Logic for sharing to other platforms
        val imageUrl = memoryList[photoViewer.currentItem].imageUrl
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, Uri.parse(imageUrl))
            type = "image/*"
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun downloadImage(imageUrl: String) {
        // Logic to download the image to the local gallery
        val url = Uri.parse(imageUrl)
        val bitmap = Glide.with(requireContext())
            .asBitmap()
            .load(url)
            .submit()
            .get()

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "memory_image.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uri = requireActivity().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            requireActivity().contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
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

// Modify MemoryData to match your actual constructor
data class MemoryData(
    val placeName: String,
    val description: String,
    val imageUrl: String,
    val rating: Float,
    val timestamp: Timestamp // Ensure you store Timestamp, not String
)

