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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.kuishinbo.databinding.FragmentMemoriesBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*

class MemoriesFragment : Fragment() {

    private lateinit var backButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var photoViewer: ViewPager2
    private lateinit var imageList: List<String>
    private lateinit var photoDetails: TextView
    private lateinit var placeName: TextView
    private lateinit var placeDescription: TextView
    private lateinit var placeRating: RatingBar
    private lateinit var shareOptions: LinearLayout
    private lateinit var othersButton: ImageButton
    private lateinit var settingsModalCard: CardView
    private lateinit var addPinMemory: LinearLayout
    private lateinit var deleteMemory: LinearLayout
    private val db = FirebaseFirestore.getInstance()
    private val memoriesCollection = db.collection("memories")

    private lateinit var memoryList: List<MemoryData> // List to hold the memory data

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate layout dengan binding
        val binding = FragmentMemoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentMemoriesBinding.bind(view)
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
        settingsModalCard = binding.settingsModalCard
        addPinMemory = binding.addPinMemory
        deleteMemory = binding.deleteMemory

        // Reverse swipe direction
        photoViewer.layoutDirection = View.LAYOUT_DIRECTION_LTR

        loadMemoryData(
            arguments?.getString("selectedDate"),
            arguments?.getString("imageUrl"),
            arguments?.getString("selectedPlaceName")
        )

        // Toggle modal visibility when settings button is clicked
        settingsButton.setOnClickListener {
            if (settingsModalCard.visibility == View.GONE) {
                settingsModalCard.visibility = View.VISIBLE
            } else {
                settingsModalCard.visibility = View.GONE
            }
        }

        // Handle Add Pin Memory option
        addPinMemory.setOnClickListener {
            val memory = memoryList[photoViewer.currentItem]
            val currentIsPinned = memory.isPinned
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            // Get the count of pinned memories from Firebase
            db.collection("memories")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isPinned", true)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.size() >= 3 && !currentIsPinned) {
                        // If there are already 3 pinned photos and the current one is not pinned, show a message
                        Toast.makeText(requireContext(), "Anda hanya dapat mem-pinned hingga 3 foto.", Toast.LENGTH_SHORT).show()
                    } else {
                        // Proceed with updating the pin status
                        memoriesCollection.whereEqualTo("imageUrl", memory.imageUrl).get()
                            .addOnSuccessListener { documents ->
                                for (document in documents) {
                                    val newIsPinned = !currentIsPinned
                                    memoriesCollection.document(document.id)
                                        .update("isPinned", newIsPinned)
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                requireContext(),
                                                if (newIsPinned) "Memori berhasil di-pin." else "Pin memori berhasil dihapus.",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            // Update UI
                                            memory.isPinned = newIsPinned
                                            updatePinButtonUI(newIsPinned)
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(requireContext(), "Gagal memperbarui status pin memori.", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Terjadi kesalahan saat memuat data memori.", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Terjadi kesalahan saat memuat data pinned memori.", Toast.LENGTH_SHORT).show()
                }
            settingsModalCard.visibility = View.GONE // Hide modal after action
        }

        // Handle Delete Memory option
        deleteMemory.setOnClickListener {
            val memory = memoryList[photoViewer.currentItem]

            // Show confirmation dialog
            AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Hapus")
                .setMessage("Apakah Anda yakin ingin menghapus memori ini?")
                .setPositiveButton("Hapus") { _, _ ->
                    // Proceed to delete the memory
                    deleteMemoryFromFirebase(memory)
                }
                .setNegativeButton("Batal", null)
                .show()
            settingsModalCard.visibility = View.GONE // Hide modal after action
        }

        // Set back button click listener
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun loadMemoryData(selectedDate: String? = null, imageUrl: String? = null, selectedPlaceName: String? = null) {
        val passedPlaceName = arguments?.getString("selectedPlaceName") ?: selectedPlaceName
        val passedImageUrl = arguments?.getString("imageUrl") ?: imageUrl

        val query = memoriesCollection.orderBy("timestamp", Query.Direction.ASCENDING)

        query.get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(requireContext(), "No memories found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                memoryList = documents.map { doc ->
                    val timestamp = doc.getTimestamp("timestamp")
                    val timestampString = timestamp?.toDate()?.let { formatDate(it) } ?: "Unknown"

                    MemoryData(
                        doc.getString("placeName") ?: "",
                        doc.getString("description") ?: "",
                        doc.getString("imageUrl") ?: "",
                        doc.getDouble("rating")?.toFloat() ?: 0f,
                        timestamp ?: Timestamp.now(),
                        doc.getBoolean("isPinned") == true
                    )
                }

                val filteredMemories = when {
                    passedPlaceName != null -> {
                        memoryList.filter { it.placeName == passedPlaceName }
                    }
                    passedImageUrl != null -> {
                        memoryList.filter { it.imageUrl == passedImageUrl }
                    }
                    selectedDate != null -> {
                        memoryList.filter {
                            formatDate(it.timestamp.toDate()).contains(selectedDate)
                        }
                    }
                    else -> memoryList
                }

                if (filteredMemories.isEmpty()) {
                    Toast.makeText(requireContext(), "No memories found for the selected criteria", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Update memoryList with filtered results
                memoryList = filteredMemories

                // Set the first memory of the filtered list as the current view
                updateMemoryDetails(memoryList[0])
                updatePinButtonUI(memoryList[0].isPinned)

                val photoAdapter = PhotoAdapter(requireContext(), memoryList)
                photoViewer.adapter = photoAdapter

                othersButton.setOnClickListener { shareToOthers() }

                photoViewer.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        updateMemoryDetails(memoryList[position])
                        updatePinButtonUI(memoryList[position].isPinned)
                    }
                })
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error loading memories: ${exception.message}", Toast.LENGTH_SHORT).show()
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
        val placeName = memory.placeName ?: "Unknown Place"
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
                        addWatermarkAndTitle(bitmap, placeName)
                    }

                    // Save the image to external storage in background
                    val savedImageUri = withContext(Dispatchers.IO) {
                        saveImageToExternalStorage(watermarkedBitmap)
                    }

                    // Create share intent
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, savedImageUri)
                        putExtra(Intent.EXTRA_TEXT, "Hey, check out my memories! #KuishinboApp\n" +
                                "Download the app here: https://play.google.com/store/apps/details?id=com.example.kuishinbo")
                        type = "image/*"
                    }

                    // Check if WhatsApp is installed
                    val packageManager = requireContext().packageManager
                    val whatsappIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, savedImageUri)
                        putExtra(Intent.EXTRA_TEXT, "Hey, check out my memories! #KuishinboApp")
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

    private fun addWatermarkAndTitle(originalBitmap: Bitmap, placeName: String): Bitmap {
        val watermarkText = "#Kuishinbo"
        val width = originalBitmap.width
        val height = originalBitmap.height

        // Add Instax frame around the original image
        val framedBitmap = addInstaxFrame(originalBitmap)

        val canvas = Canvas(framedBitmap)
        val paint = Paint().apply {
            // Use custom font from res/font folder
            typeface = ResourcesCompat.getFont(requireContext(), R.font.my_font) // Replace with your font name
            color = Color.parseColor("#FF5733") // Example color
            textSize = 200f
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Draw the watermark at the top center
        val watermarkX = (framedBitmap.width - paint.measureText(watermarkText)) / 2
        val watermarkY = height - 20f // Position at the top
        canvas.drawText(watermarkText, watermarkX, watermarkY, paint)

        // Prepare to draw the place name at the bottom center
        paint.textSize = 250f // Initial size for the place name
        val maxWidth = framedBitmap.width - 200 // Reduce width to avoid cutting off near the edges
        val placeNameLines = breakTextToFitWidth(placeName, paint, maxWidth)

        // Adjust vertical position for multi-line text
        val lineHeight = paint.textSize + 20f // Add padding between lines
        val startY = framedBitmap.height - 200f - ((placeNameLines.size - 1) * lineHeight) // Adjust start position dynamically

        // Draw each line of the place name
        placeNameLines.forEachIndexed { index, line ->
            val lineX = (framedBitmap.width - paint.measureText(line)) / 2
            val lineY = startY + (index * lineHeight)
            canvas.drawText(line, lineX, lineY, paint)
        }

        return framedBitmap
    }

    // Function to break text into multiple lines to fit within a specific width
    private fun breakTextToFitWidth(text: String, paint: Paint, maxWidth: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }

    // Function to add Instax-style frame around the image
    private fun addInstaxFrame(originalBitmap: Bitmap): Bitmap {
        val frameColor = Color.WHITE
        val topPadding = 130 // Padding at the top of the frame
        val bottomPadding = 600 // Larger padding at the bottom for a more authentic Instax look

        val width = originalBitmap.width + 2 * 100 // Horizontal padding around the image
        val height = originalBitmap.height + topPadding + bottomPadding // Vertical padding for top and bottom

        // Create a new bitmap with extra space for the frame
        val framedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(framedBitmap)

        // Draw the frame (white background)
        canvas.drawColor(frameColor)

        // Calculate the position to draw the original image inside the frame
        val left = 100f  // Horizontal padding
        val top = topPadding.toFloat()  // Vertical padding at the top

        // Draw the original image inside the frame
        canvas.drawBitmap(originalBitmap, left, top, null)

        return framedBitmap
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

    private fun deleteMemoryFromFirebase(memory: MemoryData) {
        memoriesCollection.whereEqualTo("imageUrl", memory.imageUrl).get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    memoriesCollection.document(document.id).delete()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Memori berhasil dihapus.", Toast.LENGTH_SHORT).show()
                            // Refresh UI
                            loadMemoryData(null, null)
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Gagal menghapus memori.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Terjadi kesalahan.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePinButtonUI(isPinned: Boolean) {
        val pinText = if (isPinned) "Unpin Memories" else "Pin Memories"
        val pinIcon = if (isPinned) R.drawable.ic_unpin else R.drawable.ic_pin

        addPinMemory.findViewById<TextView>(R.id.pinTextView)?.text = pinText
        addPinMemory.findViewById<ImageView>(R.id.pinIconView)?.setImageResource(pinIcon)
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
    val timestamp: Timestamp,
    var isPinned: Boolean = false
)


