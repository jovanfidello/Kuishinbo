package com.example.kuishinbo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val user = auth.currentUser
        val nameTextView = view.findViewById<TextView>(R.id.username_text_view)
        val profilePictureView = view.findViewById<ImageView>(R.id.profile_photo_view)
        val notificationsSwitch = view.findViewById<Switch>(R.id.notifications_switch)
        val locationSubtext = view.findViewById<TextView>(R.id.location_subtext)
        val otherCard = view.findViewById<LinearLayout>(R.id.other_card)
        val shareCard = view.findViewById<LinearLayout>(R.id.share_card)
        val rateCard = view.findViewById<LinearLayout>(R.id.rate_card)
        val aboutCard = view.findViewById<LinearLayout>(R.id.about_card)
        val logoutButton = view.findViewById<Button>(R.id.logout_button)

        // Default switch state: Off
        notificationsSwitch.isChecked = false

        // Check if notifications are enabled when fragment is created
        if (areNotificationsEnabled()) {
            notificationsSwitch.isChecked = true // Turn on the switch if notifications are enabled
        }

        if (user != null) {
            // Listen for changes in the user's data in real-time
            db.collection("users").document(user.uid)
                .addSnapshotListener { document, e ->
                    if (e != null) {
                        // Handle error
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (document != null && document.exists()) {
                        val name = document.getString("name")
                        val country = document.getString("country")
                        val photoProfileUrl = document.getString("photoProfileUrl")

                        // Display the profile photo or default if null
                        if (!photoProfileUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(photoProfileUrl)
                                .placeholder(R.drawable.user_default_pp)
                                .into(profilePictureView)
                        } else {
                            profilePictureView.setImageResource(R.drawable.user_default_pp)
                        }

                        // Show name and country
                        if (name.isNullOrEmpty() || country.isNullOrEmpty()) {
                            nameTextView.visibility = View.GONE
                            locationSubtext.visibility = View.GONE
                        } else {
                            nameTextView.text = name
                            nameTextView.visibility = View.VISIBLE
                            locationSubtext.text = country
                            locationSubtext.visibility = View.VISIBLE
                            logoutButton.visibility = View.VISIBLE
                        }
                    } else {
                        nameTextView.visibility = View.GONE
                        locationSubtext.visibility = View.GONE
                    }
                }
        }

        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Check if notifications are allowed
                if (areNotificationsEnabled()) {
                    // Notifications are allowed, proceed with enabling the switch
                    Toast.makeText(context, "Notifications Enabled", Toast.LENGTH_SHORT).show()
                } else {
                    // Request notification permission
                    requestNotificationPermission()
                    // Reset switch state to off if permission is not granted
                    notificationsSwitch.isChecked = false
                }
            }
        }

        // Set click listeners for each card
        otherCard.setOnClickListener {
            (activity as? MainActivity)?.navigateToOtherSettingFragment()
        }

        shareCard.setOnClickListener {
            // Example: share the app using an Intent
            val sendIntent = Intent(Intent.ACTION_SEND)
            sendIntent.type = "text/plain"
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this app!")
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Here’s the link to the app: [App URL]")
            startActivity(Intent.createChooser(sendIntent, "Share via"))
        }

        rateCard.setOnClickListener {
            // Example: direct user to the app's rating page on the Google Play Store
            val rateIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.example.kuishinbo"))
            startActivity(rateIntent)
        }

        aboutCard.setOnClickListener {
            (activity as? MainActivity)?.navigateToAboutFragment()
        }

        logoutButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    // Perform logout
                    auth.signOut()
                    val sharedPreferences = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
                    with(sharedPreferences.edit()) {
                        putBoolean("isLoggedIn", false)
                        apply()
                    }

                    val intent = Intent(requireActivity(), LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss() // Close dialog on "No"
                }
                .setCancelable(false) // Prevent dialog from being dismissed by tapping outside
                .show()
        }

        return view
    }

    // Function to check if notifications are enabled
    private fun areNotificationsEnabled(): Boolean {
        val notificationManager = NotificationManagerCompat.from(requireContext())
        return notificationManager.areNotificationsEnabled()
    }

    // Function to request notification permission
    private fun requestNotificationPermission() {
        AlertDialog.Builder(requireContext())
            .setTitle("Notification Permission")
            .setMessage("This app needs notification permission to enable notifications. Do you want to allow it?")
            .setPositiveButton("Yes") { _, _ ->
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().packageName)
                startActivity(intent)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Update the switch status when the fragment is resumed
        val notificationsSwitch = view?.findViewById<Switch>(R.id.notifications_switch)
        if (notificationsSwitch != null && areNotificationsEnabled()) {
            notificationsSwitch.isChecked = true // Turn on the switch if notifications are enabled
        }
    }
}
