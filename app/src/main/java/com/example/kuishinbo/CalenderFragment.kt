package com.example.kuishinbo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kuishinbo.databinding.FragmentCalenderBinding
import java.util.Calendar

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
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val months = mutableListOf<MonthModel>()

        for (i in 0..5) { // Prepare 6 months
            val month = (currentMonth + i) % 12
            val year = currentYear + (currentMonth + i) / 12
            val monthName = getMonthName(month) + " $year"
            val daysInMonth = getDaysInMonth(month, year)

            val days = (1..daysInMonth).mapIndexed { index, dayNumber ->
                DayModel(
                    dayNumber = dayNumber,
                    dayOfWeek = DayOfWeek.values()[index % 7],
                    imageRes = R.drawable.sample_image,
                    isSelected = dayNumber == 13 || dayNumber == 28
                )
            }

            months.add(MonthModel(name = monthName, days = days))
        }

        return months
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
