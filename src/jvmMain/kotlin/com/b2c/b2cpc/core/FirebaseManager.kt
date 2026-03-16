package com.b2c.b2cpc.core

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.google.cloud.firestore.Firestore
import java.io.FileInputStream
import java.io.File

object FirebaseManager {
    var db: Firestore? = null
        private set

    fun initialize(serviceAccountPath: String): Boolean {
        return try {
            val file = File(serviceAccountPath)
            if (!file.exists()) {
                println("Service account key file not found: $serviceAccountPath")
                return false
            }

            val serviceAccount = FileInputStream(file)

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()

            if (FirebaseApp.getApps().isNotEmpty()) {
                // 기존 연결 인스턴스 초기화 (기존 캐시된 프로젝트 연결 해제)
                FirebaseApp.getInstance().delete()
                // Firestore도 닫기
                db?.close()
                db = null
            }
            FirebaseApp.initializeApp(options)

            db = FirestoreClient.getFirestore()
            println("Firebase initialized successfully.")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
