package com.example.kuishinbo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kuishinbo.databinding.FragmentCalenderBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CalenderFragment : Fragment() {

    private lateinit var binding: FragmentCalenderBinding
    private lateinit var adapter: CalendarAdapter
    private val calendarData = mutableListOf<MonthModel>()
    private var userSignupTimestamp: Long? = null // To store the timestamp

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var currentMonthIndex = 0 // Keeps track of the current month being displayed

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

        // Set up RecyclerView with dynamic loading
        binding.calendarRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = CalendarAdapter(calendarData)
        binding.calendarRecyclerView.adapter = adapter

        binding.calendarRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                // Debugging log
                println("Total Item Count: $totalItemCount, Last Visible Item: $lastVisibleItemPosition")

                // If we have scrolled to the bottom, load more months (scrolling down)
                if (dy > 0 && lastVisibleItemPosition == totalItemCount - 1) {
                    loadMoreMonths(true) // Pass 'true' for scrolling down
                }

                // If we have scrolled to the top, load more months (scrolling up)
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                if (dy < 0 && firstVisibleItemPosition == 0) {
                    loadMoreMonths(false) // Pass 'false' for scrolling up
                }
            }
        })
    }

    // This method fetches the user's signup timestamp from Firestore
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
                    // Handle failure to fetch data
                }
        }
    }

    // This method sets up the calendar based on the user's signup timestamp
// This method sets up the calendar based on the user's signup timestamp
    private fun setupCalendar(signupTimestamp: Long) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = signupTimestamp * 1000  // Convert timestamp to milliseconds

        // Get the month and year from the timestamp
        val signupMonth = calendar.get(Calendar.MONTH)
        val signupYear = calendar.get(Calendar.YEAR)

        // Generate the calendar data starting from the user's signup month
        loadMoreMonths(true, signupMonth, signupYear)  // Pass 'true' to load months downwards
    }


    // This method loads more months dynamically
    private fun loadMoreMonths(isLoadingDown: Boolean, startMonth: Int = 0, startYear: Int = 0) {
        val calendar = Calendar.getInstance()
        var currentMonth = startMonth
        var currentYear = startYear

        // If scrolling down, load months forward
        if (isLoadingDown) {
            while (true) {
                currentMonth += 1
                if (currentMonth == 12) {
                    currentMonth = 0
                    currentYear += 1
                }

                val monthName = getMonthName(currentMonth) + " $currentYear"
                val daysInMonth = getDaysInMonth(currentMonth, currentYear)

                val days = (1..daysInMonth).mapIndexed { index, dayNumber ->
                    DayModel(
                        dayNumber = dayNumber,
                        dayOfWeek = DayOfWeek.values()[index % 7],
                        imageRes = R.drawable.sample_image,
                        isSelected = dayNumber == 13 || dayNumber == 28
                    )
                }

                calendarData.add(MonthModel(name = monthName, days = days))

                // Exit condition to prevent infinite loop
                if (calendarData.size >= 60) {
                    break
                }
            }
        } else { // If scrolling up, load months backward
            while (true) {
                currentMonth -= 1
                if (currentMonth == -1) {
                    currentMonth = 11
                    currentYear -= 1
                }

                val monthName = getMonthName(currentMonth) + " $currentYear"
                val daysInMonth = getDaysInMonth(currentMonth, currentYear)

                val days = (1..daysInMonth).mapIndexed { index, dayNumber ->
                    DayModel(
                        dayNumber = dayNumber,
                        dayOfWeek = DayOfWeek.values()[index % 7],
                        imageRes = R.drawable.sample_image,
                        isSelected = dayNumber == 13 || dayNumber == 28
                    )
                }

                calendarData.add(0, MonthModel(name = monthName, days = days)) // Add to the beginning

                // Exit condition to prevent infinite loop
                if (calendarData.size >= 60) {
                    break
                }
            }
        }

        // Notify the adapter that new data has been added
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
