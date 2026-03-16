package com.b2c.b2cpc.core

import androidx.compose.runtime.mutableStateListOf
import com.b2c.b2cpc.repository.ProductRepository
import com.google.cloud.firestore.DocumentChange
import com.google.cloud.firestore.EventListener
import com.google.cloud.firestore.ListenerRegistration
import com.google.cloud.firestore.QuerySnapshot
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.sound.sampled.AudioSystem

data class CustomNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val isError: Boolean = false,
    val durationMs: Long = 5000L
)

object SystemNotificationManager {
    // 큐 방식으로 알림 관리 (한 번에 여러 개가 아닌 순차 표시도 가능, 여기서는 상태 리스트 제공)
    val activeNotifications = androidx.compose.runtime.mutableStateListOf<CustomNotification>()
    private val notifiedProductIds = mutableSetOf<String>()
    // 로컬 PC에서 등록/수정 직후 즉시 알림이 울리는 것을 방지하기 위한 임시 억제 맵 (해시 → 등록 시간)
    private val suppressedProducts = mutableMapOf<String, Long>()
    private const val SUPPRESS_DURATION_MS = 60_000L // 등록 후 60초 동안만 알림 억제
    
    var trayState: androidx.compose.ui.window.TrayState? = null
    
    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ✅ Firestore 실시간 리스너 관련
    private var firestoreListener: ListenerRegistration? = null
    // FCM 리스너로 이미 알림을 띄운 상품의 lastNotifiedAt 값을 기록 (중복 방지)
    private val fcmNotifiedMap = mutableMapOf<String, Long>() // productId → lastNotifiedAt millis

    fun init() {
        // 더 이상 SystemTray를 사용하지 않으므로 초기화 불필요
    }

    fun showNotification(title: String, message: String, isError: Boolean = false, playSound: Boolean = true) {
        val duration = com.b2c.b2cpc.core.SessionManager.notifDurationSec * 1000L
        val notification = CustomNotification(title = title, message = message, isError = isError, durationMs = duration)
        
        if (playSound) {
            playCustomSound()
        }
        
        // Compose 상태 변경은 EDT에서 실행하며 Snapshot을 사용해 UI 갱신을 바로 트리거함
        javax.swing.SwingUtilities.invokeLater {
            androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                activeNotifications.add(0, notification)
            }
        }
        
        // 지정된 시간 후 자동 삭제
        scope.launch {
            delay(notification.durationMs)
            javax.swing.SwingUtilities.invokeLater {
                androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                    activeNotifications.remove(notification)
                }
            }
        }
    }
    fun dismiss(id: String) {
        javax.swing.SwingUtilities.invokeLater {
            androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                activeNotifications.removeAll { it.id == id }
            }
        }
    }

    // 로컬 PC에서 막 추가/수정한 상품이 직후 폴링에서 바로 울리는 것만 방지 (60초 경과 후에는 정상 알림)
    fun markAsNotified(product: com.b2c.b2cpc.model.Product) {
        val productHash = "${product.id}_${product.expirationDate}_${product.alertDays.joinToString()}_${product.alertTime}"
        suppressedProducts[productHash] = System.currentTimeMillis()
    }
    
    private fun isSuppressed(productHash: String): Boolean {
        val suppressedTime = suppressedProducts[productHash] ?: return false
        if (System.currentTimeMillis() - suppressedTime > SUPPRESS_DURATION_MS) {
            // 억제 시간 경과 → 더 이상 억제하지 않음
            suppressedProducts.remove(productHash)
            return false
        }
        return true
    }

    private fun playCustomSound() {
        if (!com.b2c.b2cpc.core.SessionManager.notifSoundEnabled) return
        val volumePercent = com.b2c.b2cpc.core.SessionManager.notifSoundVolume.coerceIn(0, 100)
        if (volumePercent == 0) return // 볼륨 0이면 무음
        
        Thread {
            try {
                val soundPath = com.b2c.b2cpc.core.SessionManager.notifSoundPath
                var filePlayed = false
                
                if (soundPath.isNotBlank()) {
                    val file = java.io.File(soundPath)
                    if (file.exists() && file.isFile) {
                        if (soundPath.lowercase().endsWith(".mp3")) {
                            // MP3 재생 (javazoom.jl.player.Player 사용)
                            val fileInputStream = java.io.FileInputStream(file)
                            val player = javazoom.jl.player.Player(fileInputStream)
                            player.play()
                            player.close()
                            filePlayed = true
                        } else {
                            // WAV 및 다른 기본 포맷 재생 (볼륨 적용)
                            val audioIn = AudioSystem.getAudioInputStream(file)
                            val clip = AudioSystem.getClip()
                            clip.open(audioIn)
                            
                            // 볼륨 조절 (MASTER_GAIN을 dB 단위로 설정)
                            try {
                                val gainControl = clip.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN) as javax.sound.sampled.FloatControl
                                val volumeDb = if (volumePercent >= 100) {
                                    gainControl.maximum
                                } else {
                                    // 0% → -80dB(무음), 100% → 0dB(최대)
                                    val minDb = gainControl.minimum.coerceAtLeast(-80f)
                                    minDb + (gainControl.maximum - minDb) * (volumePercent / 100f)
                                }
                                gainControl.value = volumeDb.coerceIn(gainControl.minimum, gainControl.maximum)
                            } catch (_: Exception) { /* 볼륨 컨트롤 미지원 시 무시 */ }
                            
                            clip.start()
                            Thread.sleep((clip.microsecondLength / 1000) + 100)
                            clip.close()
                            audioIn.close()
                            filePlayed = true
                        }
                    }
                }
                
                // 설정된 경로가 없거나 파일이 존재하지 않는 경우 → 리소스 내장 note.mp3 재생
                if (!filePlayed) {
                    filePlayed = playResourceSound()
                }
                // 리소스 사운드도 실패한 경우 기본 비프음
                if (!filePlayed) {
                    java.awt.Toolkit.getDefaultToolkit().beep()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 예외 발생 시 리소스 사운드 시도 후 실패하면 비프음
                if (!playResourceSound()) {
                    java.awt.Toolkit.getDefaultToolkit().beep()
                }
            }
        }.start()
    }

    /**
     * 리소스에 내장된 note.mp3를 기본 알림 사운드로 재생
     */
    private fun playResourceSound(): Boolean {
        return try {
            val inputStream = object {}.javaClass.getResourceAsStream("/note.mp3")
            if (inputStream != null) {
                val player = javazoom.jl.player.Player(inputStream)
                player.play()
                player.close()
                true
            } else {
                println("⚠️ 리소스 note.mp3를 찾을 수 없습니다.")
                false
            }
        } catch (e: Exception) {
            println("⚠️ 리소스 사운드 재생 실패: ${e.message}")
            false
        }
    }

    /**
     * ✅ FCM 알림 수신을 위한 Firestore 실시간 리스너 시작
     * Cloud Functions가 FCM 발신 시 products 문서의 lastNotifiedAt 필드를 갱신하는데,
     * PC에서는 이 변경을 실시간으로 감지하여 동일한 알림을 데스크톱에 표시합니다.
     */
    private fun startFirestoreListener() {
        val db = FirebaseManager.db ?: return
        val storeId = SessionManager.storeId
        if (storeId.isBlank()) return

        // 이전 리스너가 있으면 해제
        firestoreListener?.remove()
        firestoreListener = null

        println("🔔 [FCM-PC] Firestore 실시간 리스너 시작 (storeId: $storeId)")

        val query = db.collection("products")
            .whereEqualTo("storeId", storeId)
            .whereEqualTo("isVisible", true)

        firestoreListener = query.addSnapshotListener(EventListener<QuerySnapshot> { snapshots, error ->
            if (error != null) {
                println("❌ [FCM-PC] Firestore 리스너 오류: ${error.message}")
                return@EventListener
            }

            if (snapshots == null) return@EventListener

            // 최초 스냅샷에서는 알림을 띄우지 않고, 기존 lastNotifiedAt 값만 캐싱
            for (change in snapshots.documentChanges) {
                val doc = change.document
                val productId = doc.id

                when (change.type) {
                    DocumentChange.Type.MODIFIED -> {
                        // lastNotifiedAt 필드가 갱신된 경우 = Cloud Functions가 FCM 발신한 경우
                        val lastNotifiedAt = doc.getTimestamp("lastNotifiedAt")
                        if (lastNotifiedAt != null) {
                            val notifiedMillis = lastNotifiedAt.toDate().time
                            val previousMillis = fcmNotifiedMap[productId]

                            // 이전에 기록된 값과 다르면 새 알림으로 간주
                            if (previousMillis == null || notifiedMillis > previousMillis) {
                                fcmNotifiedMap[productId] = notifiedMillis

                                // 최초 로드 시에는 알림을 띄우지 않음 (앱 시작 시 1차 로드 구분)
                                if (previousMillis != null) {
                                    // ✅ 새로운 FCM 알림 감지! PC 알림 발송
                                    val productName = doc.getString("name") ?: "알 수 없는 상품"
                                    val urgencyLevel = doc.getString("lastUrgencyLevel") ?: "normal"
                                    val alertType = doc.getString("lastAlertType") ?: "scheduled"

                                    // 유통기한 정보 가져오기
                                    val rawExpDate = doc.get("expirationDate")
                                    val expirationDateStr = when (rawExpDate) {
                                        is String -> rawExpDate
                                        is com.google.cloud.Timestamp -> {
                                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                            sdf.format(rawExpDate.toDate())
                                        }
                                        else -> ""
                                    }

                                    // D-Day 계산
                                    val diffDays = try {
                                        val dateStr = if (expirationDateStr.contains(" ")) expirationDateStr.substringBefore(" ") else expirationDateStr
                                        val expDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                        ChronoUnit.DAYS.between(LocalDate.now(), expDate).toInt()
                                    } catch (e: Exception) { 0 }

                                    // 긴급도에 따른 알림 메시지 생성 (모바일과 동일)
                                    val (title, message, isError) = createFcmNotificationMessage(
                                        productName, urgencyLevel, diffDays, expirationDateStr, alertType
                                    )

                                    // 억제 체크 (로컬에서 방금 등록/수정한 경우)
                                    val alertDays = (doc.get("alertDays") as? List<*>)?.mapNotNull { it?.toString()?.toIntOrNull() } ?: listOf()
                                    val alertTimeStr = doc.getString("alertTime") ?: ""
                                    val productHash = "${productId}_${expirationDateStr}_${alertDays.joinToString()}_$alertTimeStr"
                                    if (!isSuppressed(productHash)) {
                                        println("🔔 [FCM-PC] 알림 수신! [$productName] $urgencyLevel (D-$diffDays)")
                                        showNotification(title, message, isError)

                                        // 로컬 폴링과 중복 방지를 위해 notifiedProductIds에도 추가
                                        notifiedProductIds.add(productHash)
                                        SessionManager.notifiedHistoryIds = notifiedProductIds.joinToString(",")
                                    }
                                }
                            }
                        }
                    }
                    DocumentChange.Type.ADDED -> {
                        // 최초 로드 시 기존 lastNotifiedAt 값 캐싱 (알림 X)
                        val lastNotifiedAt = doc.getTimestamp("lastNotifiedAt")
                        if (lastNotifiedAt != null) {
                            fcmNotifiedMap[productId] = lastNotifiedAt.toDate().time
                        }
                    }
                    DocumentChange.Type.REMOVED -> {
                        // 삭제된 문서는 캐시에서 제거
                        fcmNotifiedMap.remove(productId)
                    }
                }
            }
        })
    }

    /**
     * ✅ FCM 알림 메시지 생성 (모바일 MyFirebaseMessagingService의 로직과 동일)
     */
    private fun createFcmNotificationMessage(
        productName: String,
        urgencyLevel: String,
        diffDays: Int,
        expirationDate: String,
        alertType: String
    ): Triple<String, String, Boolean> {
        val dDayText = when {
            diffDays < 0 -> "${Math.abs(diffDays)}일 경과"
            diffDays == 0 -> "오늘 만료"
            diffDays == 1 -> "내일 만료"
            else -> "${diffDays}일 남음"
        }

        return when (urgencyLevel) {
            "expired" -> Triple(
                "🚨 유통기한 경고",
                "[$productName] 유통기한을 확인해주세요. $dDayText [$expirationDate]",
                true
            )
            "today" -> Triple(
                "⚠️ 유통기한 당일",
                "[$productName] 유통기한을 확인해주세요. $dDayText [$expirationDate]",
                true
            )
            "tomorrow" -> Triple(
                "⏰ 유통기한 임박",
                "[$productName] 유통기한을 확인해주세요. $dDayText [$expirationDate]",
                false
            )
            "urgent" -> {
                if (alertType == "midnight") {
                    Triple(
                        "📅 알림일 시작",
                        "[$productName] 오늘이 알림일입니다. $dDayText [$expirationDate]",
                        false
                    )
                } else {
                    Triple(
                        "📅 유통기한 알림",
                        "[$productName] 유통기한을 확인해주세요. $dDayText [$expirationDate]",
                        false
                    )
                }
            }
            else -> {
                if (alertType == "midnight") {
                    Triple(
                        "📅 알림일 시작",
                        "[$productName] 오늘이 알림일입니다. $dDayText [$expirationDate]",
                        false
                    )
                } else {
                    Triple(
                        "📋 유통기한 알림",
                        "[$productName] 유통기한을 확인해주세요. $dDayText [$expirationDate]",
                        false
                    )
                }
            }
        }
    }

    fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        
        // ✅ Firestore 실시간 리스너 시작 (FCM 알림 수신)
        startFirestoreListener()
        
        monitorJob = scope.launch {
            val repository = ProductRepository()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            var lastCheckDay = LocalDate.now().dayOfYear
            
            delay(5000) // 초기화 안정화 대기

            while (isActive) {
                try {
                    val products = repository.getProducts()
                    val today = LocalDate.now()
                    var soundPlayedInThisCycle = false
                    val strategy = com.b2c.b2cpc.core.SessionManager.notifSoundStrategy
                    
                    val todayStr = today.toString()
                    
                    // 날짜가 바뀌면 알림 이력 초기화 (매일 다시 알림 가능)
                    val currentDay = today.dayOfYear
                    if (currentDay != lastCheckDay) {
                        notifiedProductIds.clear()
                        fcmNotifiedMap.clear() // FCM 캐시도 초기화
                        lastCheckDay = currentDay
                    }

                    // 앱 시작 시 (혹은 날짜가 바뀐 후) 이력 복원
                    if (notifiedProductIds.isEmpty()) {
                        val savedDate = com.b2c.b2cpc.core.SessionManager.notifiedHistoryDate
                        if (savedDate == todayStr) {
                            val savedIds = com.b2c.b2cpc.core.SessionManager.notifiedHistoryIds
                            if (savedIds.isNotBlank()) {
                                notifiedProductIds.addAll(savedIds.split(","))
                            }
                        } else {
                            // 날짜가 다르면 기존 저장된 내역 클리어
                            com.b2c.b2cpc.core.SessionManager.notifiedHistoryDate = todayStr
                            com.b2c.b2cpc.core.SessionManager.notifiedHistoryIds = ""
                        }
                    }
                    
                    for (product in products) {
                        if (!product.isVisible) continue
                        
                        // 수정사항 발생시 다시 알림이 울릴 수 있도록, 알림조건 속성을 조합한 해시를 사용
                        val productHash = "${product.id}_${product.expirationDate}_${product.alertDays.joinToString()}_${product.alertTime}"
                        if (notifiedProductIds.contains(productHash)) continue
                        // 로컬에서 방금 등록/수정한 상품은 60초 동안만 알림 억제
                        if (isSuppressed(productHash)) continue

                        try {
                            if (product.expirationDate.isNotBlank()) {
                                val dateStr = if (product.expirationDate.contains(" ")) product.expirationDate.substringBefore(" ") else product.expirationDate
                                val expDate = LocalDate.parse(dateStr, formatter)
                                val daysLeft = ChronoUnit.DAYS.between(today, expDate).toInt()

                                if (daysLeft < 0) {
                                    // 유통기한 경과된 상품 알림 X 
                                    notifiedProductIds.add(productHash)
                                    com.b2c.b2cpc.core.SessionManager.notifiedHistoryIds = notifiedProductIds.joinToString(",")
                                } else if (daysLeft == 0 || product.alertDays.contains(daysLeft)) {
                                    
                                    // ✅ 사용자가 설정한 알림 시간(alertTime) 대조
                                    var isTimeReached = true
                                    if (product.alertTime.isNotBlank() && product.alertTime != "00:00") {
                                        try {
                                            val timeParts = product.alertTime.split(":")
                                            if (timeParts.size >= 2) {
                                                val alertHour = timeParts[0].toIntOrNull() ?: 0
                                                val alertMin = timeParts[1].toIntOrNull() ?: 0
                                                val nowTime = LocalTime.now()
                                                val alertTargetTime = LocalTime.of(alertHour, alertMin)
                                                
                                                if (nowTime.isBefore(alertTargetTime)) {
                                                    isTimeReached = false // 아직 지정한 시간이 지나지 않음
                                                }
                                            }
                                        } catch (e: Exception) {
                                            // 파싱 에러 시 기본적으로 알림 발생 허용     
                                        }
                                    }
                                    
                                    if (isTimeReached) {
                                        // 당일이거나 설정된 알림일이고 지정한 시간이 된 경우 알림 처리 시작
                                        val shouldPlaySound = if (strategy == "동시 발생 시 1번만 재생") !soundPlayedInThisCycle else true
                                        
                                        if (daysLeft == 0) {
                                            showNotification("유통기한 당일 ⚠️", "상품 '${product.name}'의 유통기한이 오늘까지입니다.", isError = true, playSound = shouldPlaySound)
                                        } else {
                                            showNotification("유통기한 임박 ⏰", "상품 '${product.name}'의 유통기한이 ${daysLeft}일 남았습니다.", isError = false, playSound = shouldPlaySound)
                                        }
                                        
                                        if (shouldPlaySound) {
                                            soundPlayedInThisCycle = true
                                        }
                                        
                                        notifiedProductIds.add(productHash)
                                        com.b2c.b2cpc.core.SessionManager.notifiedHistoryIds = notifiedProductIds.joinToString(",")
                                        delay(1000)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // 날짜 파싱 오류 무시
                        }
                    }
                    
                    // 30초마다 반복 체크 (새 상품 등록 시 최대 30초 이내 알림 발생)
                    delay(30_000L)
                } catch (e: Exception) {
                    delay(30_000L) // 오류 시 30초 후 재시도
                }
            }
        }
    }
    
    fun stopMonitoring() {
        // ✅ Firestore 리스너 해제
        firestoreListener?.remove()
        firestoreListener = null
        fcmNotifiedMap.clear()
        println("🔕 [FCM-PC] Firestore 실시간 리스너 중지")

        monitorJob?.cancel()
        monitorJob = null
        javax.swing.SwingUtilities.invokeLater {
            androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                activeNotifications.clear()
            }
        }
    }

    fun removeTrayIcon() {
        // 더 이상 트레이 아이콘을 쓰지 않음
    }
}
