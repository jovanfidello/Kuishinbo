package com.example.kuishinbo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
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
        val emailTextView = view.findViewById<TextView>(R.id.email_text_view)
        val nameTextView = view.findViewById<TextView>(R.id.name_text_view)
        val nameEditText = view.findViewById<EditText>(R.id.name_edit_text)
        val cityTextView = view.findViewById<TextView>(R.id.city_text_view)
        val cityEditText = view.findViewById<EditText>(R.id.city_edit_text)


        val saveButton = view.findViewById<Button>(R.id.save_button)
        val updateInfoButton = view.findViewById<Button>(R.id.update_info_button)
        val changePasswordButton = view.findViewById<Button>(R.id.change_password_button)
        val logoutButton = view.findViewById<Button>(R.id.logout_button)

        if (user != null) {
            emailTextView.text = user.email

            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name")
                        val city = document.getString("city")

                        if (name.isNullOrEmpty() || city.isNullOrEmpty()) {
                            nameTextView.visibility = View.GONE
                            nameEditText.visibility = View.VISIBLE
                            cityTextView.visibility = View.GONE
                            cityEditText.visibility = View.VISIBLE
                            saveButton.visibility = View.VISIBLE
                            updateInfoButton.visibility = View.GONE
                            changePasswordButton.visibility = View.GONE
                        } else {
                            nameTextView.text = name
                            nameTextView.visibility = View.VISIBLE
                            nameEditText.visibility = View.GONE
                            cityTextView.text = city
                            cityTextView.visibility = View.VISIBLE
                            cityEditText.visibility = View.GONE
                            saveButton.visibility = View.GONE
                            updateInfoButton.visibility = View.VISIBLE
                            changePasswordButton.visibility = View.VISIBLE
                            logoutButton.visibility = View.VISIBLE
                        }
                    } else {
                        // If no user data exists
                        nameTextView.visibility = View.GONE
                        nameEditText.visibility = View.VISIBLE
                        cityTextView.visibility = View.GONE
                        cityEditText.visibility = View.VISIBLE


                        saveButton.visibility = View.VISIBLE
                        updateInfoButton.visibility = View.GONE
                        changePasswordButton.visibility = View.GONE
                    }
                }
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val city = cityEditText.text.toString()

            if (name.isNotEmpty() && city.isNotEmpty()) {
                val userData = hashMapOf(
                    "name" to name,
                    "city" to city
                )

                db.collection("users").document(user!!.uid).set(userData)
                    .addOnSuccessListener {
                        Toast.makeText(activity, "Information saved", Toast.LENGTH_SHORT).show()
                        nameTextView.text = name
                        nameTextView.visibility = View.VISIBLE
                        nameEditText.visibility = View.GONE
                        cityTextView.text = city
                        cityTextView.visibility = View.VISIBLE
                        cityEditText.visibility = View.GONE
                        saveButton.visibility = View.GONE
                        updateInfoButton.visibility = View.VISIBLE
                        changePasswordButton.visibility = View.VISIBLE
                        logoutButton.visibility = View.VISIBLE
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(activity, "Error saving information: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(activity, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        updateInfoButton.setOnClickListener {
            startActivity(Intent(activity, UpdateUserInfoActivity::class.java))
        }

        changePasswordButton.setOnClickListener {
            startActivity(Intent(activity, ResetPasswordActivity::class.java))
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            val sharedPreferences = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putBoolean("isLoggedIn", false)
                apply()
            }

            // Clear the activity's back stack and start LoginActivity
            val intent = Intent(activity, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            activity?.finish()
        }

        return view
    }
}