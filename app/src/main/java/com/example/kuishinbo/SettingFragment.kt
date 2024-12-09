package com.example.kuishinbo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class SettingFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var listenerRegistration: ListenerRegistration? = null

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
            listenerRegistration = db.collection("users").document(user.uid)
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
                        if (isAdded && activity != null) {
                            if (!photoProfileUrl.isNullOrEmpty()) {
                                Glide.with(this)
                                    .load(photoProfileUrl)
                                    .placeholder(R.drawable.user_default_pp)
                                    .into(profilePictureView)
                            } else {
                                profilePictureView.setImageResource(R.drawable.user_default_pp)
                            }
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
            } else {
                // Handle notification toggle off
                if (areNotificationsEnabled()) {
                    // Show dialog to inform user to disable in settings
                    showNotificationSettingsDialog(notificationsSwitch)
                } else {
                    Toast.makeText(context, "Notifications are already disabled", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Set click listeners for each card
        otherCard.setOnClickListener {
            (activity as? MainActivity)?.navigateToOtherSettingFragment()
        }

        shareCard.setOnClickListener {
            val sendIntent = Intent(Intent.ACTION_SEND)
            sendIntent.type = "text/plain"
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Discover Kuishinbo: The Ultimate Snap Foodie App!")
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Calling all food lovers! 🌟 Download Kuishinbo and embark on a delicious journey. Explore new eateries, share your foodie moments, and connect with fellow enthusiasts. Get personalized recommendations, stay ahead of the latest food trends, and enjoy seamless food delivery. Don’t miss out on the foodie fun! 🍽️\n\nDownload now: https://play.google.com/store/apps/details?id=com.example.kuishinbo")
            startActivity(Intent.createChooser(sendIntent, "Share with friends"))
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and above, request notification permission directly
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
            }
        } else {
            // For older versions, notifications are enabled by default
            Toast.makeText(context, "Notifications are enabled by default", Toast.LENGTH_SHORT).show()
        }
    }

    // Updated method with switch reference as a parameter
    private fun showNotificationSettingsDialog(switch: Switch) {
        AlertDialog.Builder(requireContext())
            .setTitle("Notification Permission")
            .setMessage("To fully disable notifications, you need to turn them off in the app settings. Do you want to open settings now?")
            .setPositiveButton("Yes") { _, _ ->
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().packageName)
                startActivity(intent)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                // Reset switch state to ON
                switch.isChecked = true
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        val notificationsSwitch = view?.findViewById<Switch>(R.id.notifications_switch)
        notificationsSwitch?.isChecked = areNotificationsEnabled()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove() // Remove listener to avoid memory leaks
    }
}
