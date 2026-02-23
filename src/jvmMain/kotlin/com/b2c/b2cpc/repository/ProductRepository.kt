package com.b2c.b2cpc.repository

import com.b2c.b2cpc.core.FirebaseManager
import com.b2c.b2cpc.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepository {
    private val db = FirebaseManager.db

    suspend fun getProducts(): List<Product> = withContext(Dispatchers.IO) {
        if (db == null) return@withContext emptyList()
        
        try {
            val storeId = com.b2c.b2cpc.core.SessionManager.storeId
            val querySnapshot = if (storeId.isNotBlank()) {
                db!!.collection("products").whereEqualTo("storeId", storeId).get().get()
            } else {
                db!!.collection("products").get().get() // fallback
            }
            
            querySnapshot.documents.map { doc ->
                val type = doc.getString("type") ?: "상온"
                val rawDate = doc.get("expirationDate")
                val expirationDateStr = when (rawDate) {
                    is String -> rawDate
                    is com.google.cloud.Timestamp -> {
                        val sdf = if (type == "저온") java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                        else java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        sdf.format(rawDate.toDate())
                    }
                    else -> ""
                }

                Product(
                    id = doc.id,
                    barcode = doc.getString("barcode") ?: "",
                    name = doc.getString("name") ?: "",
                    category = doc.getString("category") ?: "",
                    expirationDate = expirationDateStr,
                    alertDays = (doc.get("alertDays") as? List<*>)?.mapNotNull { it?.toString()?.toIntOrNull() } ?: listOf(),
                    imageUrl = doc.getString("imageUrl") ?: "",
                    imageFilename = doc.getString("imageFilename") ?: "",
                    alertTime = doc.getString("alertTime") ?: "",
                    type = type,
                    storeId = doc.getString("storeId") ?: "",
                    isVisible = doc.getBoolean("isVisible") ?: true,
                    timeZone = doc.getString("timeZone") ?: ""
                )
            }
        } catch (e: Exception) {
            java.io.File("C:\\Users\\saqdd\\Desktop\\firebase_error_log.txt").writeText(e.stackTraceToString())
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addProduct(product: Product): Boolean = withContext(Dispatchers.IO) {
        if (db == null) return@withContext false
        try {
            val expirationTimestamp: Any = try {
                val dateStr = product.expirationDate
                val date = if (dateStr.contains(":")) {
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).parse(dateStr)
                } else {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(dateStr)
                }
                if (date != null) com.google.cloud.Timestamp.of(date) else product.expirationDate
            } catch (e: Exception) {
                product.expirationDate
            }

            val currentStoreId = com.b2c.b2cpc.core.SessionManager.storeId
            val finalStoreId = if (product.storeId.isNotBlank()) product.storeId else currentStoreId
            
            val productMap = mapOf(
                "barcode" to product.barcode,
                "name" to product.name,
                "category" to product.category,
                "expirationDate" to expirationTimestamp,
                "alertDays" to product.alertDays,
                "imageUrl" to product.imageUrl,
                "imageFilename" to product.imageFilename,
                "alertTime" to product.alertTime,
                "type" to product.type,
                "storeId" to finalStoreId,
                "isVisible" to product.isVisible,
                "timeZone" to product.timeZone,
                "createdAt" to com.google.cloud.Timestamp.now(),
                "notifyCount" to 0
            )
            val docRef = db!!.collection("products").add(productMap).get()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateProduct(product: Product): Boolean = withContext(Dispatchers.IO) {
        if (db == null || product.id.isEmpty()) return@withContext false
        try {
            val expirationTimestamp: Any = try {
                val dateStr = product.expirationDate
                val date = if (dateStr.contains(":")) {
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).parse(dateStr)
                } else {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(dateStr)
                }
                if (date != null) com.google.cloud.Timestamp.of(date) else product.expirationDate
            } catch (e: Exception) {
                product.expirationDate
            }

            val currentStoreId = com.b2c.b2cpc.core.SessionManager.storeId
            val finalStoreId = if (product.storeId.isNotBlank()) product.storeId else currentStoreId
            
            val productMap = mapOf(
                "barcode" to product.barcode,
                "name" to product.name,
                "category" to product.category,
                "expirationDate" to expirationTimestamp,
                "alertDays" to product.alertDays,
                "imageUrl" to product.imageUrl,
                "imageFilename" to product.imageFilename,
                "alertTime" to product.alertTime,
                "type" to product.type,
                "storeId" to finalStoreId,
                "isVisible" to product.isVisible,
                "timeZone" to product.timeZone,
                "notifyCount" to 0
            )
            db!!.collection("products").document(product.id).set(productMap, com.google.cloud.firestore.SetOptions.merge()).get()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteProduct(productId: String): Boolean = withContext(Dispatchers.IO) {
        if (db == null || productId.isEmpty()) return@withContext false
        try {
            db.collection("products").document(productId).delete().get()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
