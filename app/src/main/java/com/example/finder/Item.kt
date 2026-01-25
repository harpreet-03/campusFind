package com.example.finder

import com.google.firebase.Timestamp

data class Item(
    var id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val zone: String = "",
    val type: String = "Lost",
    val contactInfo: String = "",
    val imageUrl: String = "",
    val timestamp: Timestamp? = null
)