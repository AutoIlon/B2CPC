package com.b2c.b2cpc

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.b2c.b2cpc.core.FirebaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame

fun openFileDialog(): String? {
    val dialog = FileDialog(null as Frame?, "Service Account Key (.json) 선택", FileDialog.LOAD)
    dialog.file = "*.json"
    dialog.isVisible = true
    return if (dialog.directory != null && dialog.file != null) {
        dialog.directory + dialog.file
    } else {
        null
    }
}

@Composable
@Preview
fun App() {
    var isInitialized by remember { mutableStateOf(false) }
    var keyPath by remember { mutableStateOf(com.b2c.b2cpc.core.SessionManager.keyPath) }
    var currentStoreId by remember { mutableStateOf(com.b2c.b2cpc.core.SessionManager.storeId) }
    var statusMessage by remember { mutableStateOf(if (keyPath.isBlank()) "파이어베이스 초기화가 필요합니다." else "자동 로그인 시도 중...") }
    val coroutineScope = rememberCoroutineScope()

    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateVersionTag by remember { mutableStateOf("") }
    var updateDownloadUrl by remember { mutableStateOf("") }
    var downloadProgress by remember { mutableStateOf(-1) }
    var isCheckingUpdate by remember { mutableStateOf(true) }

    // 버전 체크 및 자동 로그인
    LaunchedEffect(Unit) {
        val currentVersion = "1.0.0" // TODO: build.gradle 의 설정값 등과 연동
        val updateInfo = com.b2c.b2cpc.core.AutoUpdater.checkForUpdates(currentVersion)
        
        if (updateInfo != null) {
            updateVersionTag = updateInfo.first
            updateDownloadUrl = updateInfo.second
            showUpdateDialog = true
        } else {
            isCheckingUpdate = false
            // 업데이트가 없거나 실패한 경우 자동 로그인 진행
            if (keyPath.isNotBlank()) {
            val success = kotlinx.coroutines.withContext(Dispatchers.IO) {
                FirebaseManager.initialize(keyPath)
            }
            if (success) {
                isInitialized = true
            } else {
                statusMessage = "자동 로그인 실패. 올바른 인증 파일인지 확인해주세요."
                com.b2c.b2cpc.core.SessionManager.clearSession()
            }
            }
        }
    }

    MaterialTheme {
        if (showUpdateDialog) {
            AlertDialog(
                onDismissRequest = { /* 강제 업데이트이거나 단순 닫기 방지 */ },
                title = { Text("업데이트 가능") },
                text = {
                    Column {
                        Text("새로운 버전(v$updateVersionTag)이 있습니다. 업데이트하시겠습니까?")
                        if (downloadProgress >= 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(progress = downloadProgress / 100f, modifier = Modifier.fillMaxWidth())
                            Text("다운로드 중... $downloadProgress%", modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        coroutineScope.launch {
                            downloadProgress = 0
                            com.b2c.b2cpc.core.AutoUpdater.downloadAndInstallUpdate(updateDownloadUrl) { progress ->
                                downloadProgress = progress
                            }
                        }
                    }, enabled = downloadProgress < 0) {
                        Text(if (downloadProgress < 0) "업데이트 및 재시작" else "다운로드 중...")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showUpdateDialog = false
                        isCheckingUpdate = false
                        // 업데이트 무시 시 자동 로그인 로직 재실행
                        coroutineScope.launch {
                            if (keyPath.isNotBlank()) {
                                statusMessage = "자동 로그인 시도 중..."
                                val success = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                    FirebaseManager.initialize(keyPath)
                                }
                                if (success) {
                                    isInitialized = true
                                } else {
                                    statusMessage = "자동 로그인 실패. 올바른 인증 파일인지 확인해주세요."
                                    com.b2c.b2cpc.core.SessionManager.clearSession()
                                }
                            }
                        }
                    }, enabled = downloadProgress < 0) {
                        Text("나중에")
                    }
                }
            )
        } else if (isCheckingUpdate) {
            // 업데이트 검사 중 로딩 화면
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (!isInitialized) {
            // Login / Initialization Screen
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("B2CPC 관리자 로그인", style = MaterialTheme.typography.h5)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = keyPath,
                        onValueChange = { keyPath = it },
                        label = { Text("Service Account Key JSON 경로") },
                        modifier = Modifier.weight(1f),
                        readOnly = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val path = openFileDialog()
                        if (path != null) {
                            keyPath = path
                        }
                    }, modifier = Modifier.height(56.dp).padding(top = 8.dp)) {
                        Text("파일 찾기")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    if (keyPath.isBlank()) {
                        statusMessage = "JSON 키 파일을 먼저 선택해주세요."
                        return@Button
                    }
                    coroutineScope.launch {
                        statusMessage = "초기화 중..."
                        val success = withContext(Dispatchers.IO) {
                            FirebaseManager.initialize(keyPath)
                        }
                        if (success) {
                            com.b2c.b2cpc.core.SessionManager.keyPath = keyPath
                            isInitialized = true
                        } else {
                            statusMessage = "초기화 실패. 선택한 파일이 올바른 구글 인증 파일인지 확인해주세요."
                        }
                    }
                }) {
                    Text("초기화 (로그인)")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(statusMessage, color = MaterialTheme.colors.error)
            }
        } else {
            if (currentStoreId.isBlank()) {
                com.b2c.b2cpc.ui.QrSyncScreen(onStoreSynced = { scannedStoreId ->
                    com.b2c.b2cpc.core.SessionManager.storeId = scannedStoreId
                    currentStoreId = scannedStoreId
                })
            } else {
                // Main Dashboard Screen with Sidebar (Store Sync Completed)
                com.b2c.b2cpc.ui.MainScreen(
                    onLogout = {
                        com.b2c.b2cpc.core.SessionManager.clearSession()
                        isInitialized = false
                        currentStoreId = ""
                        keyPath = ""
                        statusMessage = "파이어베이스 초기화가 필요합니다."
                    }
                )
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "B2CPC") {
        App()
    }
}
