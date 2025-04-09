package com.example.login

data class AppInfo(
    val name: String,
    val packageName: String,
    val usageTime: Long = 0L,
    val isTopUsed: Boolean = false
)