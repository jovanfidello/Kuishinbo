package com.example.kuishinbo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
        val emailTextView = view.findViewById<TextView>(R.id.email_text_view)
        val nameTextView = view.findViewById<TextView>(R.id.name_text_view)
        val countryTextView = view.findViewById<TextView>(R.id.country_text_view)
        val profilePictureView = view.findViewById<ImageView>(R.id.profile_photo_view)
        val updateInfoButton = view.findViewById<Button>(R.id.update_info_button)
        val changePasswordButton = view.findViewById<Button>(R.id.change_password_button)
        val logoutButton = view.findViewById<Button>(R.id.logout_button)

        if (user != null) {
            emailTextView.text = user.email

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
                                .placeholder(R.drawable.user_default_pp) // Default profile image
                                .into(profilePictureView)
                        } else {
                            profilePictureView.setImageResource(R.drawable.user_default_pp) // Default profile image
                        }

                        // Show name and country
                        if (name.isNullOrEmpty() || country.isNullOrEmpty()) {
                            nameTextView.visibility = View.GONE
                            countryTextView.visibility = View.GONE
                            updateInfoButton.visibility = View.GONE
                            changePasswordButton.visibility = View.GONE
                        } else {
                            nameTextView.text = name
                            nameTextView.visibility = View.VISIBLE
                            countryTextView.text = country
                            countryTextView.visibility = View.VISIBLE
                            updateInfoButton.visibility = View.VISIBLE
                            changePasswordButton.visibility = View.VISIBLE
                            logoutButton.visibility = View.VISIBLE
                        }
                    } else {
                        nameTextView.visibility = View.GONE
                        countryTextView.visibility = View.GONE
                        updateInfoButton.visibility = View.GONE
                        changePasswordButton.visibility = View.GONE
                    }
                }
        }

        updateInfoButton.setOnClickListener {
            startActivity(Intent(requireActivity(), UpdateUserInfoActivity::class.java))
        }

        changePasswordButton.setOnClickListener {
            startActivity(Intent(requireActivity(), ResetPasswordActivity::class.java))
        }

        logoutButton.setOnClickListener {
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

        return view
    }
}
