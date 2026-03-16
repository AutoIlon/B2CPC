package com.b2c.b2cpc.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private data class AlertCategory(
    val emoji: String,
    val name: String,
    val range: String,
    val days: List<Int>,
    val color: Color
)

@Composable
fun AlertDaysSelectorDialog(
    initialSelectedDays: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit
) {
    var tempSelected by remember { mutableStateOf(initialSelectedDays) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxHeight()
            ) {
                // Header
                Surface(
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "알림일 설정",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "유통기한 며칠 전에 알림을 받을지 선택해주세요.",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Divider(color = Color(0xFFE0E0E0))

                // Scrollable Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 8.dp)
                    ) {
                        AlertDaysSelector(
                            selectedDays = tempSelected,
                            onSelectionChange = { tempSelected = it }
                        )
                    }
                }

                Divider(color = Color(0xFFE0E0E0))

                // Buttons
                Surface(
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("취소", color = Color.Gray, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onConfirm(tempSelected) },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32)),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                tint = Color.White,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("확인", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertDaysSelector(
    selectedDays: List<Int>,
    onSelectionChange: (List<Int>) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(0) }
    var isCategoryExpanded by remember { mutableStateOf(false) }

    val categories = listOf(
        AlertCategory("🚨", "초임박", "0~3일", (0..3).toList(), Color(0xFFFF5722)),
        AlertCategory("⚡", "임박", "4~7일", (4..7).toList(), Color(0xFFFF9800)),
        AlertCategory("📅", "일반", "8~30일", (8..30).toList(), Color(0xFF2196F3)),
        AlertCategory("📦", "장기보관", "31~100일", (31..100).toList(), Color(0xFF4CAF50))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        SelectedDaysSummary(
            selectedDays = selectedDays,
            onClearSelection = { onSelectionChange(emptyList()) },
            onRemoveDay = { dayToRemove ->
                val newSelection = selectedDays - dayToRemove
                onSelectionChange(newSelection.sorted())
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        CategoryTabs(
            categories = categories,
            selectedCategory = selectedCategory,
            isExpanded = isCategoryExpanded,
            onCategorySelected = { newCategory ->
                if (selectedCategory == newCategory && isCategoryExpanded) {
                    isCategoryExpanded = false
                } else {
                    selectedCategory = newCategory
                    isCategoryExpanded = true
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(
            visible = isCategoryExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            DaysGrid(
                category = categories[selectedCategory],
                selectedDays = selectedDays,
                onSelectionChange = onSelectionChange
            )
        }
    }
}

@Composable
private fun SelectedDaysSummary(
    selectedDays: List<Int>,
    onClearSelection: () -> Unit,
    onRemoveDay: (Int) -> Unit = {}
) {
    Card(
        backgroundColor = if (selectedDays.isNotEmpty()) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .height(140.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = if (selectedDays.isNotEmpty()) Color(0xFF2E7D32) else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (selectedDays.isNotEmpty()) "총 ${selectedDays.size}개의 알림일이 선택됨" else "알림을 받을 날짜를 선택해주세요",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selectedDays.isNotEmpty()) Color(0xFF2E7D32) else Color.Gray
                )
                Spacer(modifier = Modifier.weight(1f))
                if (selectedDays.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "모두 취소",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onClearSelection() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (selectedDays.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(selectedDays.sorted().size) { index ->
                            val day = selectedDays.sorted()[index]
                            Surface(
                                color = Color(0xFF2E7D32),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .height(54.dp)
                                    .widthIn(min = 100.dp)
                                    .clickable { onRemoveDay(day) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = if (day == 0) "당일" else "D-$day",
                                        fontSize = 17.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "선택 취소",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "하단의 일수를 탭하여 추가하세요",
                            fontSize = 15.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTabs(
    categories: List<AlertCategory>,
    selectedCategory: Int,
    isExpanded: Boolean,
    onCategorySelected: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(categories) { index, category ->
            val isSelected = selectedCategory == index && isExpanded
            val backgroundColor = if (isSelected) category.color else Color(0xFFF5F5F5)

            Card(
                backgroundColor = backgroundColor,
                modifier = Modifier
                    .size(width = 90.dp, height = 80.dp)
                    .clickable { onCategorySelected(index) }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(6.dp)
                ) {
                    Text(
                        text = category.emoji,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color.Black
                    )
                    Text(
                        text = category.range,
                        fontSize = 12.sp,
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun DaysGrid(
    category: AlertCategory,
    selectedDays: List<Int>,
    onSelectionChange: (List<Int>) -> Unit
) {
    Card(
        backgroundColor = Color.White,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "${category.emoji} ${category.name} (${category.range})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = category.color
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 85.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 250.dp)
            ) {
                items(category.days.size) { index ->
                    val day = category.days[index]
                    DayChip(
                        day = day,
                        isSelected = selectedDays.contains(day),
                        color = category.color,
                        onClick = {
                            val newSelection = if (selectedDays.contains(day)) {
                                selectedDays - day
                            } else {
                                selectedDays + day
                            }
                            onSelectionChange(newSelection.sorted())
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayChip(
    day: Int,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) color else Color(0xFFF8F9FA)
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(200), label = ""
    )

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .scale(scale)
            .clickable { onClick() }
            .height(56.dp)
            .widthIn(min = 80.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$day",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Color.Black
                )
                Text(
                    text = if (day == 0) "당일" else "일 전",
                    fontSize = 11.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp)
                )
            }
        }
    }
}
