package com.example.kuishinbo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kuishinbo.databinding.FragmentCalenderBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CalenderFragment : Fragment() {

    private lateinit var binding: FragmentCalenderBinding
    private lateinit var adapter: CalendarAdapter
    private val calendarData = mutableListOf<MonthModel>()
    private var userSignupTimestamp: Long? = null

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCalenderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fetch the user's signup timestamp from Firestore
        fetchUserSignupTimestamp()

        // Set up RecyclerView
        binding.calendarRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = CalendarAdapter(calendarData)
        binding.calendarRecyclerView.adapter = adapter
    }

    // Fetch the user's signup timestamp from Firestore
    private fun fetchUserSignupTimestamp() {
        val user = auth.currentUser
        user?.let {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val timestamp = document.getTimestamp("timestamp")?.seconds
                        userSignupTimestamp = timestamp
                        userSignupTimestamp?.let { setupCalendar(it) }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("CalenderFragment", "Error fetching signup timestamp: ${e.message}")
                }
        }
    }

    // Set up the calendar from the signup date to the current date
    private fun setupCalendar(signupTimestamp: Long) {
        val signupCalendar = Calendar.getInstance().apply {
            timeInMillis = signupTimestamp * 1000 // Convert seconds to milliseconds
        }
        val currentCalendar = Calendar.getInstance()

        val signupMonth = signupCalendar.get(Calendar.MONTH)
        val signupYear = signupCalendar.get(Calendar.YEAR)
        val currentMonth = currentCalendar.get(Calendar.MONTH)
        val currentYear = currentCalendar.get(Calendar.YEAR)

        // Load calendar data from signup month/year to current month/year
        loadCalendarData(signupMonth, signupYear, currentMonth, currentYear)
    }

    // Load calendar data for the given range
    private fun loadCalendarData(
        startMonth: Int,
        startYear: Int,
        endMonth: Int,
        endYear: Int
    ) {
        val calendar = Calendar.getInstance()

        // Clear the existing data in the list before loading new data
        calendarData.clear()

        var currentMonth = startMonth
        var currentYear = startYear

        while (true) {
            calendar.set(currentYear, currentMonth, 1) // Set to the 1st day of the current month
            val monthName = getMonthName(currentMonth) + " $currentYear"
            val daysInMonth = getDaysInMonth(currentMonth, currentYear)
            val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Day of the week of the 1st day

            val days = mutableListOf<DayModel>()
            for (i in 1 until firstDayOfWeek) {
                days.add(
                    DayModel(
                        dayNumber = 0,
                        dayOfWeek = DayOfWeek.values()[(i - 1) % 7],
                        date = null, // Tambahkan nilai null untuk tanggal
                        imageRes = null
                    )
                )
            }

            // Add actual days of the month
            for (dayNumber in 1..daysInMonth) {
                val dayOfWeekIndex = (firstDayOfWeek - 1 + (dayNumber - 1)) % 7

                // Hitung tanggal berdasarkan tahun, bulan, dan hari
                calendar.set(currentYear, currentMonth, dayNumber)
                val date = calendar.time // Objek Date untuk tanggal spesifik

                days.add(
                    DayModel(
                        dayNumber = dayNumber,
                        dayOfWeek = DayOfWeek.values()[dayOfWeekIndex],
                        date = date, // Tambahkan nilai date
                        imageRes = R.drawable.sample_image,
                        isSelected = false
                    )
                )
            }

            // Add the month to the calendarData list
            calendarData.add(MonthModel(name = monthName, days = days))

            // Stop if we reach the end month and year
            if (currentMonth == endMonth && currentYear == endYear) break

            // Move to the next month
            currentMonth += 1
            if (currentMonth == 12) {
                currentMonth = 0
                currentYear += 1
            }
        }

        adapter.notifyDataSetChanged()
    }


    private fun getMonthName(month: Int): String {
        return listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )[month]
    }

    private fun getDaysInMonth(month: Int, year: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
}
