package com.b2c.b2cpc.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.b2c.b2cpc.core.FirebaseManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun QrSyncScreen(onStoreSynced: (String) -> Unit, onCancel: () -> Unit) {
    var sessionId by remember { mutableStateOf("") }
    var qrMatrix by remember { mutableStateOf<Array<BooleanArray>?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // 1. Session ID 생성
        val sid = UUID.randomUUID().toString()
        sessionId = sid

        // 2. QR 코드 비트 매트릭스 생성
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(sid, BarcodeFormat.QR_CODE, 256, 256)
            val matrix = Array(bitMatrix.height) { y ->
                BooleanArray(bitMatrix.width) { x ->
                    bitMatrix.get(x, y)
                }
            }
            qrMatrix = matrix
        } catch (e: Exception) {
            errorMessage = "QR 코드를 생성할 수 없습니다."
        }
        
        // 3. Firestore auth_sessions 문서 실시간 구독을 통해 모바일 응답 대기
        val db = FirebaseManager.db
        if (db != null) {
            println("QrSyncScreen: listening to session $sid")
            val listener = db.collection("auth_sessions").document(sid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        println("QrSyncScreen: Firestore error -> ${error.message}")
                        errorMessage = "데이터베이스 연결 오류: ${error.message}"
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val storeId = snapshot.getString("storeId")
                        println("QrSyncScreen: document exists! storeId=$storeId")
                        if (!storeId.isNullOrBlank()) {
                            // 스캔 완료 및 storeId 수신 성공
                            println("QrSyncScreen: success, synced storeId=$storeId")
                            onStoreSynced(storeId)
                        } else {
                            errorMessage = "인증 실패: 유효하지 않은 계정 (storeId 없음)"
                        }
                    } else {
                        println("QrSyncScreen: waiting for document to be created...")
                    }
                }

            // Snapshot listener fallback (Polling) just in case
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                while(true) {
                    try {
                        val doc = db.collection("auth_sessions").document(sid).get().get()
                        if (doc.exists()) {
                            val storeId = doc.getString("storeId")
                            if (!storeId.isNullOrBlank()) {
                                println("QrSyncScreen: Polling found document! storeId=$storeId")
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    onStoreSynced(storeId)
                                }
                                break
                            }
                        }
                    } catch (e: Exception) {
                        println("QrSyncScreen Polling Error: ${e.message}")
                    }
                    kotlinx.coroutines.delay(2000)
                }
            }
        } else {
            errorMessage = "Firebase가 연결되지 않았습니다."
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F6FA)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            elevation = 6.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(32.dp).width(450.dp),
            backgroundColor = Color.White
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.QrCode, contentDescription = "QR Connect", tint = Color(0xFF3498DB), modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("휴대폰 앱에서 스캔하세요", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "B2Cmobile 앱 설정 메뉴의 'PC 연결 (QR 스캔)'을 열어\n아래의 코드를 스캔해 주세요.", 
                    color = Color.Gray, 
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                if (qrMatrix != null) {
                    Canvas(modifier = Modifier.size(256.dp)) {
                        val cellSize = size.width / qrMatrix!!.size
                        qrMatrix!!.forEachIndexed { y, row ->
                            row.forEachIndexed { x, isBlack ->
                                if (isBlack) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(x * cellSize, y * cellSize),
                                        size = Size(cellSize, cellSize)
                                    )
                                }
                            }
                        }
                    }
                    if (errorMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(errorMessage, color = Color.Red, textAlign = TextAlign.Center)
                    }
                } else if (errorMessage.isNotBlank()) {
                    Text(errorMessage, color = Color.Red, textAlign = TextAlign.Center)
                } else {
                    CircularProgressIndicator()
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                if (sessionId.isNotBlank()) {
                    Text("Session ID: $sessionId", color = Color.LightGray, style = MaterialTheme.typography.caption)
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onCancel) {
                    Text("초기화 (다른 키 파일 선택하기)", color = Color.Gray)
                }
            }
        }
    }
}
