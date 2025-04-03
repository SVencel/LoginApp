package com.example.login

data class Section(
    val name: String = "",
    val apps: List<String> = emptyList(),
    val startHour: Int = 0,
    val startMinute: Int = 0,
    val endHour: Int = 0,
    val endMinute: Int = 0
)
