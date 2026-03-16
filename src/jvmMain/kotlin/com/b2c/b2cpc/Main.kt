package com.b2c.b2cpc

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.b2c.b2cpc.core.FirebaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame

@Composable
fun FrameWindowScope.CustomTitleBar(
    windowState: WindowState,
    isMaximized: Boolean,
    onMinimizeToTray: () -> Unit,
    onMinimize: () -> Unit,
    onMaximizeRestore: () -> Unit,
    onClose: () -> Unit
) {
    WindowDraggableArea(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(Color(0xFF1A252F)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(10.dp))
            Text("🏪", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "B2CPC",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 🔽 트레이로 최소화 버튼
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onMinimizeToTray() }
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text("🔽", fontSize = 13.sp)
            }
            
            // — 최소화 버튼
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onMinimize() }
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text("─", color = Color.White, fontSize = 14.sp)
            }
            
            // □ 최대화/복원 버튼
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onMaximizeRestore() }
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isMaximized) "❐" else "□", color = Color.White, fontSize = 14.sp)
            }
            
            // × 닫기 버튼
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onClose() }
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

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
fun App(onMinimizeToTray: () -> Unit = {}) {
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
        val currentVersion = try {
            object {}.javaClass.getResourceAsStream("/version.txt")?.bufferedReader()?.readText()?.trim() ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
        val cp = System.getProperty("java.class.path") ?: ""
        val isDevMode = cp.contains("idea_rt") || cp.contains("build") || cp.contains("gradle")
        val updateInfo = if (isDevMode) null else com.b2c.b2cpc.core.AutoUpdater.checkForUpdates(currentVersion)
        
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
                com.b2c.b2cpc.ui.QrSyncScreen(
                    onStoreSynced = { scannedStoreId ->
                        com.b2c.b2cpc.core.SessionManager.storeId = scannedStoreId
                        currentStoreId = scannedStoreId
                    },
                    onCancel = {
                        com.b2c.b2cpc.core.SessionManager.clearSession()
                        isInitialized = false
                        currentStoreId = ""
                        keyPath = ""
                        statusMessage = "파이어베이스 초기화가 필요합니다."
                    }
                )
            } else {
                // Main Dashboard Screen with Sidebar (Store Sync Completed)
                LaunchedEffect(currentStoreId) {
                    if (currentStoreId.isNotBlank()) {
                        com.b2c.b2cpc.core.SystemNotificationManager.init()
                        com.b2c.b2cpc.core.SystemNotificationManager.startMonitoring()
                    }
                }
                
                com.b2c.b2cpc.ui.MainScreen(
                    onLogout = {
                        com.b2c.b2cpc.core.SystemNotificationManager.stopMonitoring()
                        com.b2c.b2cpc.core.SessionManager.clearSession()
                        isInitialized = false
                        currentStoreId = ""
                        keyPath = ""
                        statusMessage = "파이어베이스 초기화가 필요합니다."
                    },
                    onMinimizeToTray = onMinimizeToTray
                )
            }
        }
    }
}

fun main() {
    System.setProperty("skiko.renderApi", "OPENGL")
    val toolkit = java.awt.Toolkit.getDefaultToolkit()
    val screenSize = toolkit.screenSize
    val savedWidth = com.b2c.b2cpc.core.SessionManager.windowWidth.let { if (it <= 0) screenSize.width else it }
    val savedHeight = com.b2c.b2cpc.core.SessionManager.windowHeight.let { if (it <= 0) screenSize.height else it }

    application {
        val windowState = rememberWindowState(
            width = savedWidth.dp, 
            height = savedHeight.dp,
            placement = if (com.b2c.b2cpc.core.SessionManager.windowWidth <= 0) WindowPlacement.Maximized else WindowPlacement.Floating
        )
        
        // 창 크기가 변할 때마다 환경설정에 저장력
        LaunchedEffect(windowState.size) {
            com.b2c.b2cpc.core.SessionManager.windowWidth = windowState.size.width.value.toInt()
            com.b2c.b2cpc.core.SessionManager.windowHeight = windowState.size.height.value.toInt()
        }

        val trayState = androidx.compose.ui.window.rememberTrayState()
        com.b2c.b2cpc.core.SystemNotificationManager.trayState = trayState

        var isVisible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
        val appIcon = androidx.compose.ui.res.painterResource("ic_launcher.png")

        Tray(
            state = trayState,
            icon = appIcon,
            tooltip = "B2CPC",
            onAction = { isVisible = !isVisible },
            menu = {
                Item("열기", onClick = { isVisible = true })
                Item("알림 테스트", onClick = {
                    com.b2c.b2cpc.core.SystemNotificationManager.showNotification(
                        title = "트레이 알림 테스트",
                        message = "트레이 모드에서도 정상적으로 알림이 울립니다.",
                        isError = false
                    )
                })
                Item("종료", onClick = {
                    com.b2c.b2cpc.core.SystemNotificationManager.removeTrayIcon()
                    exitApplication()
                })
            }
        )

        Window(
            onCloseRequest = {
                com.b2c.b2cpc.core.SystemNotificationManager.stopMonitoring()
                com.b2c.b2cpc.core.SystemNotificationManager.removeTrayIcon()
                exitApplication()
            }, 
            visible = isVisible,
            title = "B2CPC",
            icon = appIcon,
            state = windowState,
            undecorated = true,
            resizable = true
        ) {
            val frameScope = this
            var isMaximized by remember { mutableStateOf(false) }
            
            Column(modifier = Modifier.fillMaxSize()) {
                with(frameScope) {
                    CustomTitleBar(
                        windowState = windowState,
                        isMaximized = isMaximized,
                        onMinimizeToTray = { isVisible = false },
                        onMinimize = { windowState.isMinimized = true },
                        onMaximizeRestore = {
                            if (isMaximized) {
                                windowState.placement = WindowPlacement.Floating
                            } else {
                                windowState.placement = WindowPlacement.Maximized
                            }
                            isMaximized = !isMaximized
                        },
                        onClose = {
                            com.b2c.b2cpc.core.SystemNotificationManager.stopMonitoring()
                            com.b2c.b2cpc.core.SystemNotificationManager.removeTrayIcon()
                            exitApplication()
                        }
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    App(onMinimizeToTray = { isVisible = false })
                }
            }
        }

    // 커스텀 알림 오버레이 창
    val notifications = com.b2c.b2cpc.core.SystemNotificationManager.activeNotifications
    val isNotifVisible = notifications.isNotEmpty()

    val positionStr = com.b2c.b2cpc.core.SessionManager.notifPosition
        
        val windowPos = when (positionStr) {
            "상단 왼쪽" -> WindowPosition(Alignment.TopStart)
            "상단 중앙" -> WindowPosition(Alignment.TopCenter)
            "상단 오른쪽" -> WindowPosition(Alignment.TopEnd)
            "중단 왼쪽" -> WindowPosition(Alignment.CenterStart)
            "중단 오른쪽" -> WindowPosition(Alignment.CenterEnd)
            "하단 왼쪽" -> WindowPosition(Alignment.BottomStart)
            "하단 중앙" -> WindowPosition(Alignment.BottomCenter)
            else -> WindowPosition(Alignment.BottomEnd) // 기본
        }

        val boxAlign = when (positionStr) {
            "상단 왼쪽" -> androidx.compose.ui.Alignment.TopStart
            "상단 중앙" -> androidx.compose.ui.Alignment.TopCenter
            "상단 오른쪽" -> androidx.compose.ui.Alignment.TopEnd
            "중단 왼쪽" -> androidx.compose.ui.Alignment.CenterStart
            "중단 오른쪽" -> androidx.compose.ui.Alignment.CenterEnd
            "하단 왼쪽" -> androidx.compose.ui.Alignment.BottomStart
            "하단 중앙" -> androidx.compose.ui.Alignment.BottomCenter
            else -> androidx.compose.ui.Alignment.BottomEnd
        }

        val colAlign = when (positionStr) {
            "상단 왼쪽", "중단 왼쪽", "하단 왼쪽" -> androidx.compose.ui.Alignment.Start
            "상단 중앙", "하단 중앙" -> androidx.compose.ui.Alignment.CenterHorizontally
            else -> androidx.compose.ui.Alignment.End
        }

        val titleSize = com.b2c.b2cpc.core.SessionManager.notifTitleSize.sp
        val msgSize = com.b2c.b2cpc.core.SessionManager.notifMessageSize.sp

        val targetHeight = (notifications.size * 110 + 40).coerceAtMost(800).dp
        val overlayState = androidx.compose.ui.window.rememberWindowState(
            position = windowPos,
            width = 380.dp,
            height = targetHeight
        )
        
        LaunchedEffect(targetHeight, windowPos) {
            overlayState.size = androidx.compose.ui.unit.DpSize(380.dp, targetHeight)
            overlayState.position = windowPos // 위치를 항상 가장자리에 맞게 갱신 (짤림 방지)
        }

        androidx.compose.ui.window.Window(
            onCloseRequest = { /* No-op */ },
            visible = isNotifVisible,
            state = overlayState,
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            resizable = false,
            focusable = false // 클릭 시 포커스 뺏어오지 않음
        ) {
            if (isNotifVisible) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = boxAlign
                ) {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = colAlign
                ) {
                    items(
                        count = notifications.size,
                        key = { notifications[it].id }
                    ) { index ->
                        val notif = notifications[index]
                        
                        val enterAnim = when (positionStr) {
                            "상단 왼쪽", "중단 왼쪽", "하단 왼쪽" -> androidx.compose.animation.slideInHorizontally { -it } + androidx.compose.animation.fadeIn()
                            "상단 오른쪽", "우측 하단 (Windows 기본)", "중단 오른쪽", "하단 오른쪽" -> androidx.compose.animation.slideInHorizontally { it } + androidx.compose.animation.fadeIn()
                            "상단 중앙" -> androidx.compose.animation.slideInVertically { -it } + androidx.compose.animation.fadeIn()
                            "하단 중앙" -> androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn()
                            else -> androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn()
                        }

                        // 애니메이션 효과
                        androidx.compose.animation.AnimatedVisibility(
                            visible = true,
                            enter = enterAnim,
                            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                elevation = 12.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .clickable { com.b2c.b2cpc.core.SystemNotificationManager.dismiss(notif.id) }
                            ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(if (notif.isError) Color(0xFFFDEDEC) else Color.White)
                                    .padding(16.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                // 아이콘 영역
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (notif.isError) Color(0xFFE74C3C).copy(alpha = 0.1f) else Color(0xFF3498DB).copy(alpha = 0.1f),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    Text(
                                        text = if (notif.isError) "🚨" else "🔔",
                                        fontSize = 20.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                // 텍스트 영역
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = notif.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = titleSize,
                                        color = if (notif.isError) Color(0xFFC0392B) else Color(0xFF2C3E50)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = notif.message,
                                        fontSize = msgSize,
                                        color = Color.DarkGray,
                                        maxLines = 3,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                // 닫기 버튼
                                IconButton(
                                    onClick = { com.b2c.b2cpc.core.SystemNotificationManager.dismiss(notif.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                        contentDescription = "닫기",
                                        tint = Color.Gray
                                    )
                                }
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}
