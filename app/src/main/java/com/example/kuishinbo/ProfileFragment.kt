package com.example.kuishinbo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.GridLayout
import androidx.fragment.app.Fragment
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
