package com.example.kuishinbo

data class Entry(
    val email: String = "",
    val date: String = "",
    val time: String = "",
    val image: String = "",
    val entryType: String = "",
    val timestamp: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val imageRef: String = ""
)
