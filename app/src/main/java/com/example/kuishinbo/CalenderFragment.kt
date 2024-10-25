package com.example.kuishinbo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kuishinbo.databinding.FragmentCalenderBinding

class CalenderFragment : Fragment() {

    private lateinit var binding: FragmentCalenderBinding
    private lateinit var adapter: CalendarAdapter
    private lateinit var calendarData: List<MonthModel>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCalenderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Prepare sample data for 6 months
        calendarData = prepareCalendarData()

        // Set up the RecyclerView
        adapter = CalendarAdapter(calendarData)
        binding.calendarRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.calendarRecyclerView.adapter = adapter

        // Add item decoration for spacing between items
        binding.calendarRecyclerView.addItemDecoration(SpacesItemDecoration(8)) // 8dp spacing
    }

    // This method generates the calendar data
    private fun prepareCalendarData(): List<MonthModel> {
        val months = listOf("August 2024", "September 2024", "October 2024", "November 2024", "December 2024", "January 2025")

        return months.map { month ->
            val days = (1..30).map { day ->
                DayModel(dayNumber = day, imageRes = R.drawable.sample_image) // Placeholder image
            }
            MonthModel(name = month, days = days)
        }
    }
}
