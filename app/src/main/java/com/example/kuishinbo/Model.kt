package com.example.kuishinbo

import java.util.Date

data class MonthModel(
    val name: String,
    val days: List<DayModel>
)

data class DayModel(
    val dayNumber: Int,
    val dayOfWeek: DayOfWeek,
    val date: Date?,
    val imageRes: Int?,
    val isSelected: Boolean = false
)

enum class DayOfWeek {
    SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
}
