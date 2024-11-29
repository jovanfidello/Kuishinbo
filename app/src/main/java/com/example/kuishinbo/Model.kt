package com.example.kuishinbo

data class MonthModel(
    val name: String,
    val days: List<DayModel>
)

data class DayModel(
    val dayNumber: Int,
    val dayOfWeek: DayOfWeek,
    val imageRes: Int?,
    val isSelected: Boolean = false
)

enum class DayOfWeek {
    SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
}
