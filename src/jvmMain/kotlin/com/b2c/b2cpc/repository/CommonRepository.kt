package com.b2c.b2cpc.repository

import com.b2c.b2cpc.core.FirebaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommonRepository {
    private val db = FirebaseManager.db

    suspend fun getCollections(): List<String> = withContext(Dispatchers.IO) {
        if (db == null) return@withContext emptyList()
        try {
            val collections = db.listCollections()
            collections.map { it.id }.toList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
