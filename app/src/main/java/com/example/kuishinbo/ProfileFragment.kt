package com.example.kuishinbo

import android.content.Intent
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
        val cityTextView = view.findViewById<TextView>(R.id.city_text_view)
        val profilePictureView = view.findViewById<ImageView>(R.id.profile_picture_view) // Updated to ImageView
        val backButton = view.findViewById<ImageButton>(R.id.back_button)
        val settingsButton = view.findViewById<ImageButton>(R.id.settings_button)
        val memoriesButton = view.findViewById<Button>(R.id.view_all_memories_button)

        // Retrieve user information
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name")
                        val city = document.getString("city")
                        val photoProfileUrl = document.getString("photoProfileUrl") // Profile photo URL

                        // Display the profile photo or default if null
                        if (!photoProfileUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(photoProfileUrl)
                                .placeholder(R.drawable.user_default_pp) // Default profile image
                                .into(profilePictureView)
                        } else {
                            profilePictureView.setImageResource(R.drawable.user_default_pp) // Default profile image
                        }

                        nameTextView.visibility = if (!name.isNullOrEmpty()) {
                            nameTextView.text = name
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                        cityTextView.visibility = if (!city.isNullOrEmpty()) {
                            cityTextView.text = city
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                        backButton.visibility = View.VISIBLE
                        settingsButton.visibility = View.VISIBLE
                    } else {
                        nameTextView.visibility = View.GONE
                        cityTextView.visibility = View.GONE
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
}
