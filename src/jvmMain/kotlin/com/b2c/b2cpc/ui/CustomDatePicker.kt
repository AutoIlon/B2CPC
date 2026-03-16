package com.b2c.b2cpc.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.DayOfWeek
import java.time.YearMonth

@Composable
fun ExpirationDatePicker(
    expirationDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    // 선택된 날짜 파싱 및 남은 일수 계산
    val selectedDate = try {
        if (expirationDate.isNotBlank()) {
            LocalDate.parse(expirationDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } else null
    } catch (e: Exception) { null }

    val remainingDays = selectedDate?.let {
        ChronoUnit.DAYS.between(LocalDate.now(), it).toInt()
    }

    // 상태에 따른 색상 결정
    val backgroundColor by animateColorAsState(
        targetValue = when {
            remainingDays == null -> Color(0xFFF5F5F5)
            remainingDays < 0 -> Color(0xFFFFEBEE) // 만료됨 - 빨간색
            remainingDays <= 3 -> Color(0xFFFFF3E0) // 임박 - 주황색
            remainingDays <= 7 -> Color(0xFFFFFDE7) // 주의 - 노란색
            else -> Color(0xFFE8F5E9) // 안전 - 초록색
        },
        animationSpec = tween(300), label = ""
    )

    val textColor = when {
        remainingDays == null -> Color.Gray
        remainingDays < 0 -> Color(0xFFD32F2F) // 빨간색
        remainingDays <= 3 -> Color(0xFFFF6F00) // 주황색
        remainingDays <= 7 -> Color(0xFFFF8F00) // 노란색
        else -> Color(0xFF2E7D32) // 초록색
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDatePicker = true },
        shape = RoundedCornerShape(12.dp),
        backgroundColor = backgroundColor,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 달력 아이콘
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "날짜 선택",
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "유통기한",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (expirationDate.isNotBlank()) {
                        selectedDate?.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))
                            ?: expirationDate
                    } else "날짜 선택",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (expirationDate.isNotBlank()) Color.Black else Color.Gray
                )

                // 남은 일수 표시
                if (remainingDays != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            remainingDays < 0 -> "유통기한 ${-remainingDays}일 지남"
                            remainingDays == 0 -> "오늘까지"
                            else -> "${remainingDays}일 남음"
                        },
                        fontSize = 12.sp,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 선택 표시
            if (expirationDate.isNotBlank()) {
                Surface(
                    color = textColor,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
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

    // 커스텀 DatePicker Dialog
    if (showDatePicker) {
        CustomDatePickerDialog(
            initialDate = selectedDate,
            onDateSelected = { date ->
                onDateSelected(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomDatePickerDialog(
    initialDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val startMonth = YearMonth.now().minusYears(1) // 1년 전부터
    val endMonth = YearMonth.now().plusYears(3)   // 3년 후까지
    val totalMonths = (endMonth.year - startMonth.year) * 12 + (endMonth.monthValue - startMonth.monthValue) + 1
    val currentMonthIndex = if (initialDate != null) {
        val targetMonth = YearMonth.from(initialDate)
        (targetMonth.year - startMonth.year) * 12 + (targetMonth.monthValue - startMonth.monthValue)
    } else {
        (YearMonth.now().year - startMonth.year) * 12 + (YearMonth.now().monthValue - startMonth.monthValue)
    }

    val pagerState = rememberPagerState(initialPage = currentMonthIndex.coerceIn(0, totalMonths - 1)) { totalMonths }
    val coroutineScope = rememberCoroutineScope()
    var selectedDate by remember { mutableStateOf(initialDate) }
    var showYearPicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

    // 현재 표시되고 있는 월 계산
    val currentDisplayMonth = startMonth.plusMonths(pagerState.currentPage.toLong())

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
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "날짜를 선택해주세요",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 월/년 선택 헤더 + 화살표 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage > 0) {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                            }
                        },
                        enabled = pagerState.currentPage > 0
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "이전 달",
                            tint = if (pagerState.currentPage > 0) Color(0xFF2E7D32) else Color.Gray
                        )
                    }

                    // 클릭 가능한 년도/월 표시
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // 년도 클릭
                        Text(
                            text = "${currentDisplayMonth.year}년",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showYearPicker = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // 월 클릭
                        Text(
                            text = "${currentDisplayMonth.monthValue}월",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showMonthPicker = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (pagerState.currentPage < totalMonths - 1) {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        },
                        enabled = pagerState.currentPage < totalMonths - 1
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "다음 달",
                            tint = if (pagerState.currentPage < totalMonths - 1) Color(0xFF2E7D32) else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 요일 헤더
                val dayHeaders = listOf("일", "월", "화", "수", "목", "금", "토")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    dayHeaders.forEach { day ->
                        Text(
                            text = day,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (day == dayHeaders[0]) Color.Red else Color(0xFF2E7D32),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 스와이프 가능한 달력 페이저
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.height(240.dp)
                ) { page ->
                    val monthToShow = startMonth.plusMonths(page.toLong())
                    MonthCalendar(
                        yearMonth = monthToShow,
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it }
                    )
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
                            selectedDate?.let { onDateSelected(it) }
                        },
                        enabled = selectedDate != null,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32))
                    ) {
                        Text("선택", fontWeight = FontWeight.Medium, color = Color.White)
                    }
                }
            }
        }
    }

    // 년도 선택 다이얼로그
    if (showYearPicker) {
        YearPickerDialog(
            currentYear = currentDisplayMonth.year,
            onYearSelected = { selectedYear ->
                val targetMonth = YearMonth.of(selectedYear, currentDisplayMonth.monthValue)
                val targetIndex = (targetMonth.year - startMonth.year) * 12 + (targetMonth.monthValue - startMonth.monthValue)
                coroutineScope.launch {
                    pagerState.animateScrollToPage(targetIndex.coerceIn(0, totalMonths - 1))
                }
                showYearPicker = false
            },
            onDismiss = { showYearPicker = false }
        )
    }

    // 월 선택 다이얼로그
    if (showMonthPicker) {
        MonthPickerDialog(
            currentMonth = currentDisplayMonth.monthValue,
            onMonthSelected = { selectedMonth ->
                val targetMonth = YearMonth.of(currentDisplayMonth.year, selectedMonth)
                val targetIndex = (targetMonth.year - startMonth.year) * 12 + (targetMonth.monthValue - startMonth.monthValue)
                coroutineScope.launch {
                    pagerState.animateScrollToPage(targetIndex.coerceIn(0, totalMonths - 1))
                }
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }
}

@Composable
private fun YearPickerDialog(
    currentYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(300.dp)
                .height(400.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "연도 선택",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val years = (currentYear - 10..currentYear + 10).toList()
                    items(years) { year ->
                        val isCurrentYear = year == currentYear

                        Button(
                            onClick = { onYearSelected(year) },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (isCurrentYear) Color(0xFF2E7D32) else Color(0xFFF5F5F5)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$year",
                                color = if (isCurrentYear) Color.White else Color.Black,
                                fontSize = 16.sp,
                                fontWeight = if (isCurrentYear) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("취소", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun MonthPickerDialog(
    currentMonth: Int,
    onMonthSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(300.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "월 선택",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.wrapContentHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items((1..12).toList()) { month ->
                        val isCurrentMonth = month == currentMonth

                        Button(
                            onClick = { onMonthSelected(month) },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (isCurrentMonth) Color(0xFF2E7D32) else Color(0xFFF5F5F5)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "${month}월",
                                color = if (isCurrentMonth) Color.White else Color.Black,
                                fontSize = 16.sp,
                                fontWeight = if (isCurrentMonth) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("취소", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val lastDayOfMonth = yearMonth.atEndOfMonth()
    val firstWeekDay = firstDayOfMonth.dayOfWeek.value % 7 // 일요일을 0으로

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 빈 셀들 (이전 달)
        items(firstWeekDay) {
            Spacer(modifier = Modifier.height(40.dp))
        }

        // 현재 달의 날짜들
        items((1..lastDayOfMonth.dayOfMonth).toList()) { day ->
            val date = yearMonth.atDay(day)
            val isSelected = selectedDate == date
            val isToday = date == LocalDate.now()
            val isSunday = date.dayOfWeek == DayOfWeek.SUNDAY
            val isPast = date.isBefore(LocalDate.now())

            val backgroundColor = when {
                isSelected -> Color(0xFF2E7D32)
                isToday -> Color(0xFFE3F2FD)
                else -> Color.Transparent
            }

            val textColor = when {
                isSelected -> Color.White
                isPast -> Color.Gray
                isSunday -> Color.Red
                else -> Color.Black
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .clickable { onDateSelected(date) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
