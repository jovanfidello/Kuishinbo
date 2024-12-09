package com.example.kuishinbo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val user = auth.currentUser
        val nameTextView = view.findViewById<TextView>(R.id.name_text_view)
        val countryTextView = view.findViewById<TextView>(R.id.country_text_view)
        val profilePictureView = view.findViewById<ImageView>(R.id.profile_picture_view)
        val backButton = view.findViewById<ImageButton>(R.id.back_button)
        val settingsButton = view.findViewById<ImageButton>(R.id.settings_button)
        val memoriesButton = view.findViewById<Button>(R.id.view_all_memories_button)
        val userJoinedDateTextView = view.findViewById<TextView>(R.id.user_joined_date)
        val pinsContainer = view.findViewById<LinearLayout>(R.id.pins_container)
        val pinsContain1 = view.findViewById<ImageView>(R.id.pins_contain1)
        val pinsContain2 = view.findViewById<ImageView>(R.id.pins_contain2)
        val pinsContain3 = view.findViewById<ImageView>(R.id.pins_contain3)

        // Retrieve user information
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name")
                        val country = document.getString("country")
                        val photoProfileUrl = document.getString("photoProfileUrl")
                        val timestamp = document.getTimestamp("timestamp")?.toDate()

                        // Display the profile photo or default if null
                        if (!photoProfileUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(photoProfileUrl)
                                .placeholder(R.drawable.user_default_pp)
                                .into(profilePictureView)
                        } else {
                            profilePictureView.setImageResource(R.drawable.user_default_pp)
                        }

                        nameTextView.visibility = if (!name.isNullOrEmpty()) {
                            nameTextView.text = name
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                        countryTextView.visibility = if (!country.isNullOrEmpty()) {
                            countryTextView.text = country
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                        // Show the time ago text
                        if (timestamp != null) {
                            val timeAgo = getTimeAgo(timestamp)
                            userJoinedDateTextView.text = "You joined $timeAgo"
                        }

                        backButton.visibility = View.VISIBLE
                        settingsButton.visibility = View.VISIBLE
                    } else {
                        nameTextView.visibility = View.GONE
                        countryTextView.visibility = View.GONE
                        backButton.visibility = View.VISIBLE
                        settingsButton.visibility = View.VISIBLE
                    }
                }

            // Retrieve pins photo
            // Ambil koleksi pins milik user dan filter berdasarkan isPinned == true
            db.collection("memories")
                .whereEqualTo("userId", user.uid)
                .whereEqualTo("isPinned", true)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    // Cek apakah ada dokumen dalam koleksi pins
                    if (!querySnapshot.isEmpty) {
                        val pinList = querySnapshot.documents.map { it.getString("imageUrl") }

                        // Tampilkan gambar-gambar yang dipin jika belum mencapai batas 3
                        if (pinList.size <= 3) {
                            // Loop untuk menambahkan gambar ke ImageView yang ada
                            pinList.forEachIndexed { index, imageUrl ->
                                if (imageUrl != null && index <= 3) {
                                    // Cari ImageView sesuai dengan urutan
                                    val imageView = when (index) {
                                        0 -> pinsContain1
                                        1 -> pinsContain2
                                        2 -> pinsContain3
                                        else -> null
                                    }

                                    // Set gambar ke ImageView yang ditemukan
                                    imageView?.let {
                                        Glide.with(this@ProfileFragment)
                                            .load(imageUrl)
                                            .placeholder(R.drawable.empty_pin_photo)
                                            .into(it)
                                        it.setOnClickListener {
                                            if (imageUrl.isNotEmpty()) {
                                                (activity as? MainActivity)?.navigateToMemoriesFragment()
                                            } else if (imageUrl.isNullOrEmpty()){
                                                (activity as? MainActivity)?.navigateToCalenderFragment()
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Tampilkan pesan atau placeholder jika sudah ada 3 gambar yang dipin
                            val maxPinImageView = ImageView(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams(0, 150).apply {
                                    weight = 1f
                                    marginStart = 8
                                }
                                setImageResource(R.drawable.empty_pin_photo)
                                background = requireContext().getDrawable(R.drawable.image_preview_background)
                                setContentDescription("Maximum pinned images reached.")
                            }
                            pinsContainer.addView(maxPinImageView)
                        }
                    } else {
                        // Tampilkan placeholder jika tidak ada foto yang dipin
                        val emptyPinImageView = ImageView(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(0, 150).apply {
                                weight = 1f
                                marginStart = 8
                            }
                            setImageResource(R.drawable.empty_pin_photo)
                            background = requireContext().getDrawable(R.drawable.image_preview_background)
                        }
                        pinsContainer.addView(emptyPinImageView)
                    }
                }
                .addOnFailureListener { e ->
                    // Tangani kegagalan
                    Toast.makeText(requireContext(), "Failed to load pinned images.", Toast.LENGTH_SHORT).show()
                }

            // Retrieve last 14 day memories
            // Get the current date and subtract 14 days
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -14)
            val fourteenDaysAgo = calendar.time

// Retrieve the last 14 days of memories
            db.collection("memories")
                .whereEqualTo("userId", user.uid) // Assuming userId is the field used to associate the memories with the user
                .whereGreaterThanOrEqualTo("timestamp", fourteenDaysAgo) // Filter based on timestamp
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val imageUrls = mutableListOf<String>()

                    // Iterate over the documents and collect the image URLs
                    for (document in querySnapshot.documents) {
                        val imageUrl = document.getString("imageUrl")
                        if (!imageUrl.isNullOrEmpty()) {
                            imageUrls.add(imageUrl)
                        }
                    }

                    // Add images to GridLayout
                    displayImagesInGridLayout(imageUrls)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to load memories.", Toast.LENGTH_SHORT).show()
                }
        }

        // Back button functionality
        backButton.setOnClickListener {
            (activity as? MainActivity)?.navigateToHomeFragment()
        }

        // Settings button functionality
        settingsButton.setOnClickListener {
            (activity as? MainActivity)?.navigateToSettingFragment()
        }

        // Memories button functionality
        memoriesButton.setOnClickListener {
            (activity as? MainActivity)?.navigateToCalenderFragment()
        }

        val dateGridLayout = view.findViewById<GridLayout>(R.id.date_grid_layout)

        // Format tanggal
        val dateFormat = SimpleDateFormat("dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Loop untuk menambahkan 14 hari terakhir
        for (i in 13 downTo 0) {
            // Hitung tanggal
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dateText = dateFormat.format(calendar.time)

            // Buat TextView untuk tanggal
            val dateTextView = TextView(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 120 // Lebar TextView
                    height = 120 // Tinggi TextView
                    setMargins(8, 8, 8, 8) // Margin antar elemen
                }
                gravity = android.view.Gravity.CENTER
                background = requireContext().getDrawable(R.drawable.border_background)
                text = dateText
                textSize = 14f
            }

            // Tambahkan TextView ke GridLayout
            dateGridLayout.addView(dateTextView)
        }

        return view
    }

    private fun displayImagesInGridLayout(imageUrls: List<String>) {
        val dateGridLayout = view?.findViewById<GridLayout>(R.id.date_grid_layout)
        imageUrls.forEach { imageUrl ->
            // Create an ImageView for each image URL
            val imageView = ImageView(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 120 // Set width as required
                    height = 120 // Set height as required
                    setMargins(8, 8, 8, 8) // Margin between images
                }
                setImageResource(R.drawable.empty_pin_photo) // Default image
                Glide.with(this@ProfileFragment)
                    .load(imageUrl)
                    .placeholder(R.drawable.empty_pin_photo)
                    .into(this) // Load image into the ImageView
            }

            // Add the ImageView to the GridLayout
            dateGridLayout?.addView(imageView)
        }
    }



    // Function to get time ago in human-readable format
    fun getTimeAgo(timestamp: Date): String {
        val now = System.currentTimeMillis()
        val diffInMillis = now - timestamp.time

        // Check if the user joined within the last minute
        if (diffInMillis < 24 * 60 * 60 * 1000) {  // Less than one day (24 hours)
            return "today"
        }

        val days = diffInMillis / (1000 * 60 * 60 * 24) // Convert milliseconds to days
        val weeks = days / 7
        val months = days / 30
        val years = days / 365

        return when {
            days <= 6 -> "$days day${if (days > 1) "s" else ""} ago"  // 1 day, 2 days, ..., 6 days
            weeks <= 3 -> "$weeks week${if (weeks > 1) "s" else ""} ago"  // 1 week, 2 weeks, ..., 3 weeks
            months < 12 -> "$months month${if (months > 1) "s" else ""} ago"  // 1 month, 2 months, ...
            years == 1L -> "1 year ago"  // 1 year ago
            else -> "$years years ago"  // 2 years ago, 3 years ago, ...
        }
    }

    // Hide bottom navigation when ProfileFragment is shown
    override fun onResume() {
        super.onResume()
        val bottomNavigationView =
            activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView?.visibility = View.GONE
    }

    // Show bottom navigation when leaving ProfileFragment
    override fun onPause() {
        super.onPause()
        val bottomNavigationView =
            activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView?.visibility = View.VISIBLE
    }
}
