package com.b2c.b2cpc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.b2c.b2cpc.core.StartupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame

fun openSoundFileDialog(): String? {
    val dialog = FileDialog(null as Frame?, "알림 사운드 파일 (.wav, .mp3) 선택", FileDialog.LOAD)
    dialog.file = "*.wav;*.mp3"
    dialog.isVisible = true
    return if (dialog.directory != null && dialog.file != null) {
        dialog.directory + dialog.file
    } else {
        null
    }
}

@Composable
fun SettingsScreen() {
    val coroutineScope = rememberCoroutineScope()
    var isStartupOn by remember { mutableStateOf(StartupManager.isStartupEnabled()) }
    val isPackaged = StartupManager.isPackagedEnvironment()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("설정", style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("모바일 B2Cmobile의 사용자 관리, 필터 설정, 초대 등의 기능이 이 화면에서 구현될 예정입니다.", 
            style = MaterialTheme.typography.body1)
        
        Spacer(modifier = Modifier.height(32.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("PC 시스템 설정", style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(16.dp))

        // 시작프로그램 등록 토글
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("컴퓨터 부팅 시 자동 실행", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Medium)
                Text("Windows 시스템이 켜질 때 B2CPC를 백그라운드에서 자동으로 시작합니다.", style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                if (!isPackaged) {
                    Text("⚠️ (현재 개발 IDE 모드에서는 테스트할 수 없습니다)", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.error)
                }
            }
            Switch(
                checked = isStartupOn,
                onCheckedChange = { checked ->
                    isStartupOn = checked
                    coroutineScope.launch(Dispatchers.IO) {
                        StartupManager.setStartupEnabled(checked)
                    }
                },
                enabled = isPackaged
            )
        }
        
        // 알림 설정 섹션
        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))
        Text("커스텀 알림 설정", style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(16.dp))

        var notifPosition by remember { mutableStateOf(com.b2c.b2cpc.core.SessionManager.notifPosition) }
        var notifDurationSec by remember { mutableStateOf(com.b2c.b2cpc.core.SessionManager.notifDurationSec.toFloat()) }
        var notifTitleSize by remember { mutableStateOf(com.b2c.b2cpc.core.SessionManager.notifTitleSize.toFloat()) }
        var notifMessageSize by remember { mutableStateOf(com.b2c.b2cpc.core.SessionManager.notifMessageSize.toFloat()) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("표시 위치: ", fontWeight = FontWeight.Medium, modifier = Modifier.width(100.dp))
            val positions = listOf(
                "상단 왼쪽", "상단 중앙", "상단 오른쪽",
                "중단 왼쪽", "중단 오른쪽",
                "하단 왼쪽", "하단 중앙", "하단 오른쪽"
            )
            var expanded by remember { mutableStateOf(false) }

            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(notifPosition)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    positions.forEach { pos ->
                        DropdownMenuItem(onClick = {
                            notifPosition = pos
                            com.b2c.b2cpc.core.SessionManager.notifPosition = pos
                            expanded = false
                        }) {
                            Text(pos)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("표시 시간 (${notifDurationSec.toInt()}초):", fontWeight = FontWeight.Medium, modifier = Modifier.width(150.dp))
            Slider(
                value = notifDurationSec,
                onValueChange = { 
                    notifDurationSec = it 
                    com.b2c.b2cpc.core.SessionManager.notifDurationSec = it.toInt()
                },
                valueRange = 1f..15f,
                steps = 14,
                modifier = Modifier.weight(1f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("제목 크기 (${notifTitleSize.toInt()}pt):", fontWeight = FontWeight.Medium, modifier = Modifier.width(150.dp))
            Slider(
                value = notifTitleSize,
                onValueChange = { 
                    notifTitleSize = it 
                    com.b2c.b2cpc.core.SessionManager.notifTitleSize = it.toInt()
                },
                valueRange = 10f..24f,
                steps = 14,
                modifier = Modifier.weight(1f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("내용 크기 (${notifMessageSize.toInt()}pt):", fontWeight = FontWeight.Medium, modifier = Modifier.width(150.dp))
            Slider(
                value = notifMessageSize,
                onValueChange = { 
                    notifMessageSize = it
                    com.b2c.b2cpc.core.SessionManager.notifMessageSize = it.toInt()
                },
                valueRange = 10f..20f,
                steps = 10,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        var notifSoundEnabled by remember { mutableStateOf(com.b2c.b2cpc.core.SessionManager.notifSoundEnabled) }
        var notifSoundPath by remember { mutableStateOf(com.b2c.b2cpc.core.SessionManager.notifSoundPath) }
        
        var notifSoundStrategy by remember { mutableStateOf(com.b2c.b2cpc.core.SessionManager.notifSoundStrategy) }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("알림음 설정:", fontWeight = FontWeight.Medium, modifier = Modifier.width(150.dp))
            Checkbox(
                checked = notifSoundEnabled,
                onCheckedChange = { 
                    notifSoundEnabled = it
                    com.b2c.b2cpc.core.SessionManager.notifSoundEnabled = it
                }
            )
            Text("알림 시 소리 켜기")
        }

        if (notifSoundEnabled) {
            var notifSoundVolume by remember { mutableStateOf(com.b2c.b2cpc.core.SessionManager.notifSoundVolume.toFloat()) }
            
            // 볼륨 조절 슬라이더
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 150.dp)) {
                Text("🔊 볼륨 (${notifSoundVolume.toInt()}%):", fontWeight = FontWeight.Medium, modifier = Modifier.width(160.dp))
                Slider(
                    value = notifSoundVolume,
                    onValueChange = {
                        notifSoundVolume = it
                        com.b2c.b2cpc.core.SessionManager.notifSoundVolume = it.toInt()
                    },
                    valueRange = 0f..100f,
                    steps = 9,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 150.dp)) {
                RadioButton(selected = notifSoundStrategy == "개별 알림마다 재생", onClick = { 
                    notifSoundStrategy = "개별 알림마다 재생"
                    com.b2c.b2cpc.core.SessionManager.notifSoundStrategy = "개별 알림마다 재생"
                })
                Text("알림 1개당 사운드 각각 재생", modifier = Modifier.padding(start = 4.dp, end = 16.dp))
                RadioButton(selected = notifSoundStrategy == "동시 발생 시 1번만 재생", onClick = { 
                    notifSoundStrategy = "동시 발생 시 1번만 재생"
                    com.b2c.b2cpc.core.SessionManager.notifSoundStrategy = "동시 발생 시 1번만 재생"
                })
                Text("동시에 알릴 항목이 많아도 1번만 재생", modifier = Modifier.padding(start = 4.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 150.dp)) {
                OutlinedTextField(
                    value = if (notifSoundPath.isBlank()) "시스템 기본 알림(비프음)" else notifSoundPath,
                    onValueChange = {},
                    readOnly = true,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.body2
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    val path = openSoundFileDialog()
                    if (path != null) {
                        notifSoundPath = path
                        com.b2c.b2cpc.core.SessionManager.notifSoundPath = path
                    }
                }) {
                    Text("커스텀 사운드 찾기")
                }
                if (notifSoundPath.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        notifSoundPath = ""
                        com.b2c.b2cpc.core.SessionManager.notifSoundPath = ""
                    }) {
                        Text("기본음으로 초기화")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                com.b2c.b2cpc.core.SystemNotificationManager.showNotification(
                    title = "테스트 알림",
                    message = "설정하신 알림 스타일과 위치가 다음과 같이 표시됩니다.",
                    isError = false
                )
            },
            colors = ButtonDefaults.buttonColors(backgroundColor = androidx.compose.ui.graphics.Color(0xFF3498DB), contentColor = androidx.compose.ui.graphics.Color.White)
        ) {
            Text("테스트 알림 띄우기 (위치 및 크기 적용 확인)")
        }
    }
}
