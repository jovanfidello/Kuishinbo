package com.example.kuishinbo

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
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
    private var imageUrlsByDate = mutableMapOf<String, String>()

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
                                            .error(R.drawable.error_image) // Add error image
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .skipMemoryCache(false)
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .apply(RequestOptions.bitmapTransform(RoundedCorners(20))) // Rounded corners
                                            .into(it)

                                        it.setOnClickListener {
                                            val memoriesFragment = MemoriesFragment().apply {
                                                arguments = Bundle().apply {
                                                    putString("selectedDate", null) // Pinned photo tidak terkait tanggal
                                                    putString("imageUrl", imageUrl)
                                                }
                                            }
                                            (activity as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                                                ?.replace(R.id.fragment_container, memoriesFragment)
                                                ?.addToBackStack(null)
                                                ?.commit()
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

            // Retrieve last 14 day memories
            val user = auth.currentUser
            val dateGridLayout = view.findViewById<GridLayout>(R.id.date_grid_layout)

            // Retrieve last 14 days memories
            if (user != null) {
                fetchLast14DaysMemories(user.uid) {
                    displayImagesInGridLayout(dateGridLayout)
                }
            }
        }

        return view
    }

    private fun fetchLast14DaysMemories(userId: String, onComplete: () -> Unit) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -14)
        val fourteenDaysAgo = calendar.time

        db.collection("memories")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", fourteenDaysAgo)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val timestamp = document.getTimestamp("timestamp")?.toDate()
                    val imageUrl = document.getString("imageUrl")

                    if (timestamp != null && !imageUrl.isNullOrEmpty()) {
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(timestamp)
                        imageUrlsByDate[date] = imageUrl
                    }
                }
                onComplete()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load memories.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayImagesInGridLayout(dateGridLayout: GridLayout) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Clear any existing views
        dateGridLayout.removeAllViews()

        for (i in 12 downTo -1) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dateString = dateFormat.format(calendar.time)

            val container = FrameLayout(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 150
                    height = 150
                    setMargins(8, 8, 8, 8)
                }

                background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_background)
                background = ContextCompat.getDrawable(requireContext(), R.drawable.border_colored)
            }

            val imageView = ImageView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            val textView = TextView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
                text = SimpleDateFormat("dd", Locale.getDefault()).format(calendar.time)
                textSize = 16f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                gravity = Gravity.CENTER
            }

            if (imageUrlsByDate.containsKey(dateString)) {
                Glide.with(this)
                    .load(imageUrlsByDate[dateString])
                    .placeholder(R.drawable.empty_pin_photo)
                    .error(R.drawable.error_image)
                    .apply(RequestOptions.bitmapTransform(RoundedCorners(150)))
                    .into(imageView)

                // Add click listener for images with URLs
                imageView.setOnClickListener {
                    val memoriesFragment = MemoriesFragment().apply {
                        arguments = Bundle().apply {
                            putString("selectedDate", dateString)
                            putString("imageUrl", imageUrlsByDate[dateString])
                        }
                    }
                    (activity as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                        ?.replace(R.id.fragment_container, memoriesFragment)
                        ?.addToBackStack(null)
                        ?.commit()
                }
            }

            container.addView(imageView)
            container.addView(textView)
            dateGridLayout.addView(container)
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
