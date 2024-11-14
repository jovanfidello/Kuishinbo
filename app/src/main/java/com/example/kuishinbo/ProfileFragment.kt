package com.example.kuishinbo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

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

        return view
    }

    // Hide bottom navigation when ProfileFragment is shown
    override fun onResume() {
        super.onResume()
        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView?.visibility = View.GONE
    }

    // Show bottom navigation when leaving ProfileFragment
    override fun onPause() {
        super.onPause()
        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView?.visibility = View.VISIBLE
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

}
