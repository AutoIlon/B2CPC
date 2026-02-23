package com.b2c.b2cpc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun CustomTimePickerCard(
    selectedTime: String,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showTimePicker = true },
        shape = RoundedCornerShape(12.dp),
        backgroundColor = if (selectedTime != "00:00") Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 시간 아이콘
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "알림 시간 설정",
                tint = if (selectedTime != "00:00") Color(0xFF2E7D32) else Color.Gray,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "알림 설정 시간",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                val amText = "오전"
                val pmText = "오후"

                Text(
                    text = if (selectedTime != "00:00") {
                       "${convert24To12Hour(selectedTime, amText, pmText)} 알림"
                    } else "시간 설정",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selectedTime != "00:00") Color.Black else Color.Gray
                )
            }

            // 선택 표시
            if (selectedTime != "00:00") {
                Surface(
                    color = Color(0xFF2E7D32),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        CustomTimePickerDialog(
            initialTime = selectedTime,
            onTimeSelected = { time ->
                onTimeSelected(time)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
fun CustomTimePickerContent(
    selectedTime: String,
    onTimeSelected: (String) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showTimePicker = true }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = "알림 시간 설정",
            tint = if (selectedTime != "00:00") Color(0xFF2E7D32) else Color.Gray,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "알림 시간 설정",
            fontSize = 10.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(2.dp))

        val amText = "오전"
        val pmText = "오후"

        Text(
            text = if (selectedTime != "00:00") convert24To12Hour(selectedTime, amText, pmText) else "설정 안함",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (selectedTime != "00:00") Color.Black else Color.Gray,
            textAlign = TextAlign.Center
        )
    }

    if (showTimePicker) {
        CustomTimePickerDialog(
            initialTime = selectedTime,
            onTimeSelected = { time ->
                onTimeSelected(time)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

// 24시간 형식을 12시간 형식으로 변환
internal fun convert24To12Hour(time24: String, amText: String, pmText: String): String {
    val parts = time24.split(":")
    if (parts.size != 2) return time24

    val hour24 = parts[0].toIntOrNull() ?: return time24
    val minute = parts[1]

    val (hour12, amPm) = when {
        hour24 == 0 -> 12 to amText
        hour24 < 12 -> hour24 to amText
        hour24 == 12 -> 12 to pmText
        else -> (hour24 - 12) to pmText
    }

    return "${amPm} ${hour12}:${minute}"
}

// 12시간 형식을 24시간 형식으로 변환
internal fun convert12To24Hour(hour12: Int, minute: Int, isAM: Boolean): String {
    val hour24 = when {
        isAM && hour12 == 12 -> 0
        !isAM && hour12 != 12 -> hour12 + 12
        else -> hour12
    }
    return "%02d:%02d".format(hour24, minute)
}

@Composable
fun CustomTimePickerDialog(
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // 현재 시간 가져오기
    val currentTime = remember {
        val calendar = java.util.Calendar.getInstance()
        val currentHour24 = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)
        Triple(currentHour24, currentMinute, currentHour24 < 12)
    }

    // 초기값 설정: 기존에 설정된 시간이 있으면 그것을, 없으면 현재 시간을 사용
    val (initial24Hour, initialMinute, initialIsAM) = if (initialTime != "00:00") {
        // 기존 설정 시간 사용
        val timeParts = initialTime.split(":")
        val hour24 = if (timeParts.size >= 2 && timeParts[0].isNotBlank()) {
            timeParts[0].toIntOrNull()?.coerceIn(0, 23) ?: currentTime.first
        } else currentTime.first
        val minute = if (timeParts.size >= 2 && timeParts[1].isNotBlank()) {
            timeParts[1].toIntOrNull()?.coerceIn(0, 59) ?: currentTime.second
        } else currentTime.second
        Triple(hour24, minute, hour24 < 12)
    } else {
        // 현재 시간 사용
        Triple(currentTime.first, currentTime.second, currentTime.third)
    }

    // 12시간 형식으로 변환
    val initial12Hour = if (initial24Hour == 0) 12 else if (initial24Hour > 12) initial24Hour - 12 else initial24Hour

    var selectedHour12 by remember { mutableStateOf(initial12Hour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    var isAM by remember { mutableStateOf(initialIsAM) }

    val coroutineScope = rememberCoroutineScope()

    // 시간/분 리스트 생성
    val hours12 = (1..12).toList()
    val minutes = (0..59).toList() // 1분 간격

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(400.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // 헤더
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = "알림을 받을 시간을 선택해주세요",
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF2E7D32)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 현재 선택된 시간 표시
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFF2E7D32)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isAM) "오전" else "오후",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "%d:%02d".format(selectedHour12, selectedMinute),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 시간 선택 휠
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // AM/PM 선택
                    Column(
                        modifier = Modifier.weight(0.8f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "오전/오후",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            backgroundColor = Color(0xFFF8F8F8)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // 오전
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clickable { isAM = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .background(
                                            if (isAM) Color(0xFF2E7D32) else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "오전",
                                        fontSize = if (isAM) 16.sp else 14.sp,
                                        fontWeight = if (isAM) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isAM) Color.White else Color.Black
                                    )
                                }

                                // 오후
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clickable { isAM = false }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .background(
                                            if (!isAM) Color(0xFF2E7D32) else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "오후",
                                        fontSize = if (!isAM) 16.sp else 14.sp,
                                        fontWeight = if (!isAM) FontWeight.Bold else FontWeight.Normal,
                                        color = if (!isAM) Color.White else Color.Black
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 시 선택 (1-12)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "시",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            backgroundColor = Color(0xFFF8F8F8)
                        ) {
                            val hourListState = rememberLazyListState()

                            // 선택된 시간으로 스크롤
                            LaunchedEffect(selectedHour12) {
                                val selectedIndex = hours12.indexOf(selectedHour12)
                                if (selectedIndex >= 0) {
                                    hourListState.animateScrollToItem(
                                        index = selectedIndex,
                                        scrollOffset = -30
                                    )
                                }
                            }

                            LazyColumn(
                                state = hourListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                items(hours12) { hour ->
                                    val isSelected = hour == selectedHour12

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedHour12 = hour }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .background(
                                                if (isSelected) Color(0xFF2E7D32) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "%d".format(hour),
                                            fontSize = if (isSelected) 18.sp else 16.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 분 선택 (0-59)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "분",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            backgroundColor = Color(0xFFF8F8F8)
                        ) {
                            val minuteListState = rememberLazyListState()

                            // 선택된 분으로 스크롤
                            LaunchedEffect(selectedMinute) {
                                val selectedIndex = minutes.indexOf(selectedMinute)
                                if (selectedIndex >= 0) {
                                    minuteListState.animateScrollToItem(
                                        index = selectedIndex,
                                        scrollOffset = -30
                                    )
                                }
                            }

                            LazyColumn(
                                state = minuteListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                items(minutes) { minute ->
                                    val isSelected = minute == selectedMinute

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedMinute = minute }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .background(
                                                if (isSelected) Color(0xFF2E7D32) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "%02d".format(minute),
                                            fontSize = if (isSelected) 18.sp else 16.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("취소", color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val timeString24 = convert12To24Hour(selectedHour12, selectedMinute, isAM)
                            onTimeSelected(timeString24)
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32))
                    ) {
                        Text("선택", fontWeight = FontWeight.Medium, color = Color.White)
                    }
                }
            }
        }
    }
}
