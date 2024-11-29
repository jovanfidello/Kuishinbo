package com.example.kuishinbo

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import java.util.*
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
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.auth.FirebaseAuth
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.kuishinbo.databinding.FragmentMemoriesBinding
import com.google.firebase.firestore.QueryDocumentSnapshot
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class MemoriesFragment : Fragment() {

    private lateinit var backButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var photoViewer: ViewPager2
    private lateinit var photoDetails: TextView
    private lateinit var placeName: TextView
    private lateinit var placeDescription: TextView
    private lateinit var placeRating: RatingBar
    private lateinit var shareOptions: LinearLayout
    private lateinit var instagramButton: ImageButton
    private lateinit var whatsappButton: ImageButton
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
        val rootView = binding.root

        // Initialize UI components
        backButton = binding.backButton
        settingsButton = binding.settingsButton
        photoViewer = binding.photoViewer
        photoDetails = binding.photoDetails
        placeName = binding.placeName
        placeDescription = binding.placeDescription
        placeRating = binding.placeRating
        shareOptions = binding.shareOptions
        instagramButton = binding.instagramButton
        whatsappButton = binding.whatsappButton
        othersButton = binding.othersButton
        downloadButton = binding.downloadButton

        // Load memory data from Firebase
        loadMemoryData()

        // Set back button click listener
        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        return rootView
    }

    private fun loadMemoryData() {
        memoriesCollection.orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    return@addOnSuccessListener
                }

                memoryList = documents.map { doc ->
                    MemoryData(
                        doc.getString("placeName") ?: "",
                        doc.getString("description") ?: "",
                        doc.getString("imageUrl") ?: "",
                        doc.getDouble("rating")?.toFloat() ?: 0f,
                        doc.getString("timestamp") ?: ""
                    )
                }

                // Set the adapter with the loaded data
                val photoAdapter = PhotoAdapter(requireContext(), memoryList)
                photoViewer.adapter = photoAdapter

                // Set first memory details initially
                updateMemoryDetails(memoryList[0])

                // Set share button listeners
                instagramButton.setOnClickListener { shareToInstagram() }
                whatsappButton.setOnClickListener { shareToWhatsApp() }
                othersButton.setOnClickListener { shareToOthers() }
                downloadButton.setOnClickListener { downloadImage(memoryList[0].imageUrl) }

                // Listen for page change to update details
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

    private fun updateMemoryDetails(memory: MemoryData) {
        placeName.text = memory.placeName
        placeDescription.text = memory.description
        placeRating.rating = memory.rating
        photoDetails.text = formatTimestamp(memory.timestamp)
    }

    private fun formatTimestamp(timestamp: String): String? {
        return try {
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm:ss a 'UTC'Z", Locale.getDefault())
            val date = dateFormat.parse(timestamp)
            android.text.format.DateFormat.format("MMMM dd, yyyy\nhh:mm a", date).toString()
        } catch (e: ParseException) {
            null
        }
    }

    private fun shareToInstagram() {
        // Example logic for sharing to Instagram
        val imageUrl = memoryList[photoViewer.currentItem].imageUrl
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, Uri.parse(imageUrl)) // Adjust if needed
            type = "image/*"
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun shareToWhatsApp() {
        // Example logic for sharing to WhatsApp
        val imageUrl = memoryList[photoViewer.currentItem].imageUrl
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, imageUrl)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
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
}

data class MemoryData(
    val placeName: String,
    val description: String,
    val imageUrl: String,
    val rating: Float,
    val timestamp: String
)

