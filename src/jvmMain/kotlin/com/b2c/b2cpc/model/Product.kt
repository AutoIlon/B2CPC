package com.b2c.b2cpc.model

import java.util.TimeZone

data class Product(
    val id: String = "",
    val barcode: String = "",
    val name: String = "",
    val category: String = "",
    val expirationDate: String = "",  // ✅ 문자열 형태 유지
    val alertDays: List<Int> = listOf(),
    val imageUrl: String = "",
    val imageFilename: String = "",
    val alertTime: String = "",
    val type: String = "상온",
    val storeId: String = "",
    val isVisible: Boolean = true,
    val timeZone: String = TimeZone.getDefault().id // ✅ 기기 시간대 자동 저장
)
