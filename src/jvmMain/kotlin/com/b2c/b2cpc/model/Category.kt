package com.b2c.b2cpc.model

data class Category(
    val id: String = "",
    val name: String = "",
    val defaultAlertDays: List<Int> = listOf(3),
    val iconKey: String = "category"
)
