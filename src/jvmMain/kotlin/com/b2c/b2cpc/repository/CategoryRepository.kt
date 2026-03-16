package com.b2c.b2cpc.repository

import com.b2c.b2cpc.core.FirebaseManager
import com.b2c.b2cpc.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CategoryRepository {
    private val db get() = FirebaseManager.db

    /**
     * 모든 카테고리 이름 목록 가져오기
     */
    suspend fun getAllCategories(): List<String> = withContext(Dispatchers.IO) {
        if (db == null) return@withContext emptyList()
        try {
            val snapshot = db!!.collection("categories").get().get()
            snapshot.documents.mapNotNull { it.getString("name") }.distinct()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 모든 카테고리 (알림일 포함) 가져오기
     */
    suspend fun getAllCategoriesWithAlertDays(): List<Category> = withContext(Dispatchers.IO) {
        if (db == null) return@withContext emptyList()
        try {
            val snapshot = db!!.collection("categories").get().get()
            snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null

                val alertDaysData = doc.get("defaultAlertDays")
                val alertDays = when (alertDaysData) {
                    is List<*> -> {
                        alertDaysData.mapNotNull {
                            when (it) {
                                is Long -> it.toInt()
                                is Int -> it
                                is Double -> it.toInt()
                                else -> null
                            }
                        }.takeIf { it.isNotEmpty() } ?: listOf(3)
                    }
                    is Long -> listOf(alertDaysData.toInt())
                    is Int -> listOf(alertDaysData)
                    else -> listOf(3)
                }

                val iconKey = doc.getString("iconKey") ?: "category"

                Category(
                    id = doc.id,
                    name = name,
                    defaultAlertDays = alertDays,
                    iconKey = iconKey
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 특정 카테고리의 알림일 가져오기
     */
    suspend fun getCategoryAlertDays(categoryName: String): List<Int> = withContext(Dispatchers.IO) {
        if (db == null) return@withContext listOf(3)
        try {
            val snapshot = db!!.collection("categories")
                .whereEqualTo("name", categoryName)
                .get().get()

            if (snapshot.documents.isNotEmpty()) {
                val alertDaysData = snapshot.documents.first().get("defaultAlertDays")
                when (alertDaysData) {
                    is List<*> -> {
                        alertDaysData.mapNotNull {
                            when (it) {
                                is Long -> it.toInt()
                                is Int -> it
                                is Double -> it.toInt()
                                else -> null
                            }
                        }.takeIf { it.isNotEmpty() } ?: listOf(3)
                    }
                    is Long -> listOf(alertDaysData.toInt())
                    is Int -> listOf(alertDaysData)
                    else -> listOf(3)
                }
            } else {
                listOf(3)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(3)
        }
    }

    /**
     * 카테고리 추가
     */
    suspend fun addCategory(name: String, defaultAlertDays: List<Int> = listOf(3), iconKey: String = "category"): Boolean = withContext(Dispatchers.IO) {
        if (db == null) return@withContext false
        try {
            // 중복 체크
            val existing = db!!.collection("categories")
                .whereEqualTo("name", name)
                .get().get()

            if (existing.documents.isNotEmpty()) {
                return@withContext false // 중복
            }

            db!!.collection("categories").add(
                mapOf(
                    "name" to name,
                    "defaultAlertDays" to defaultAlertDays,
                    "iconKey" to iconKey
                )
            ).get()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 카테고리 알림일 수정
     */
    suspend fun updateCategoryAlertDays(categoryName: String, alertDays: List<Int>): Boolean = withContext(Dispatchers.IO) {
        if (db == null) return@withContext false
        try {
            val snapshot = db!!.collection("categories")
                .whereEqualTo("name", categoryName)
                .get().get()

            if (snapshot.documents.isNotEmpty()) {
                val docId = snapshot.documents.first().id
                db!!.collection("categories").document(docId)
                    .update("defaultAlertDays", alertDays).get()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 카테고리 삭제
     */
    suspend fun deleteCategory(name: String): Boolean = withContext(Dispatchers.IO) {
        if (db == null) return@withContext false
        try {
            val snapshot = db!!.collection("categories")
                .whereEqualTo("name", name)
                .get().get()

            if (snapshot.documents.isNotEmpty()) {
                snapshot.documents.forEach { doc ->
                    db!!.collection("categories").document(doc.id).delete().get()
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
