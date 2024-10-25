package com.example.kuishinbo

data class MonthModel(
    val name: String,
    val days: List<DayModel>
)

data class DayModel(
    val dayNumber: Int,
    val imageRes: Int? = null // Optional image for the day
)
