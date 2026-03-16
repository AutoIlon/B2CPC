package com.b2c.b2cpc.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.b2c.b2cpc.model.Product
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun ProductCard(
    product: Product,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 배경색: 연한 민트 (Comfortable Product Card 스타일 참조)
    val comfortableBg = Color(0xFFF1F8E9)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit(product) },
        shape = RoundedCornerShape(16.dp),
        backgroundColor = comfortableBg,
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 왼쪽: 아이콘/이미지 영역 (원형)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32).copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 중간: 정보
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF37474F) // 차분한 슬레이트 그레이
                    )
                    if (!product.isVisible) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFEBEE),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                "숨김", 
                                color = Color(0xFFD32F2F), 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "바코드: ${product.barcode}",
                        fontSize = 12.sp,
                        color = Color(0xFF78909C)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (product.expirationDate.isNotBlank()) "${product.expirationDate} 까지" else "유통기한 없음",
                        fontSize = 12.sp,
                        color = Color(0xFF78909C) 
                    )
                }
            }

            // 오른쪽: 상태 및 액션
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // D-Day (텍스트만 깔끔하게)
                val remainingDaysInt = calculateRemainingDays(product.expirationDate)
                
                if (remainingDaysInt != null) {
                    val dDayColor = when {
                        remainingDaysInt < 0 -> Color(0xFFD32F2F) // 지남 - 빨간색
                        remainingDaysInt <= 3 -> Color(0xFFFF7043) // 임박 - 주황
                        else -> Color(0xFF4DB6AC) // 차분한 틸 색상
                    }
                    
                    val dDayText = when {
                        remainingDaysInt < 0 -> "${-remainingDaysInt}일 지남"
                        remainingDaysInt == 0 -> "오늘 마감"
                        else -> "${remainingDaysInt}일 남음"
                    }

                    Text(
                        text = dDayText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = dDayColor
                    )
                } else {
                    Text(
                        text = "-",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 액션 아이콘 
                Row {
                    IconButton(
                        onClick = { onEdit(product) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "수정", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFFEF9A9A), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
    
    // 삭제 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("상품 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("'${product.name}' 상품을 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = { 
                    showDeleteDialog = false
                    onDelete(product) 
                }) { 
                    Text("삭제", color = Color.Red, fontWeight = FontWeight.Bold) 
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { 
                    Text("취소", color = Color.Gray) 
                }
            }
        )
    }
}

// ── 컴팩트 카드 (테이블 형태, 얇은 한 줄) ──
@Composable
fun CompactProductCard(
    product: Product,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val remainingDaysInt = calculateRemainingDays(product.expirationDate)

    val dDayColor = when {
        remainingDaysInt == null -> Color.Gray
        remainingDaysInt < 0 -> Color(0xFFD32F2F)
        remainingDaysInt <= 3 -> Color(0xFFFF7043)
        else -> Color(0xFF4DB6AC)
    }
    val dDayText = when {
        remainingDaysInt == null -> "-"
        remainingDaysInt < 0 -> "${-remainingDaysInt}일 지남"
        remainingDaysInt == 0 -> "오늘 마감"
        else -> "${remainingDaysInt}일 남음"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit(product) },
        shape = RoundedCornerShape(8.dp),
        color = if (!product.isVisible) Color(0xFFFAFAFA) else Color.White,
        border = BorderStroke(0.5.dp, Color(0xFFE0E0E0)),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 상품명
            Text(
                text = product.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF37474F),
                modifier = Modifier.weight(2f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (!product.isVisible) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color(0xFFFFEBEE),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text("숨김", color = Color(0xFFD32F2F), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                }
            }
            // 바코드
            Text(
                text = product.barcode,
                fontSize = 12.sp,
                color = Color(0xFF90A4AE),
                modifier = Modifier.weight(1.5f),
                maxLines = 1
            )
            // 유통기한
            Text(
                text = if (product.expirationDate.isNotBlank()) product.expirationDate.substringBefore(" ") else "-",
                fontSize = 12.sp,
                color = Color(0xFF78909C),
                modifier = Modifier.weight(1f)
            )
            // D-Day
            Text(
                text = dDayText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = dDayColor,
                modifier = Modifier.width(80.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
            // 액션
            IconButton(onClick = { onEdit(product) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "수정", tint = Color.Gray, modifier = Modifier.size(14.dp))
            }
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFFEF9A9A), modifier = Modifier.size(14.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("상품 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("'${product.name}' 상품을 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete(product) }) {
                    Text("삭제", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소", color = Color.Gray) }
            }
        )
    }
}

// ── 그리드 카드 (정사각형에 가까운 카드) ──
@Composable
fun GridProductCard(
    product: Product,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val remainingDaysInt = calculateRemainingDays(product.expirationDate)

    val dDayColor = when {
        remainingDaysInt == null -> Color.Gray
        remainingDaysInt < 0 -> Color(0xFFD32F2F)
        remainingDaysInt <= 3 -> Color(0xFFFF7043)
        else -> Color(0xFF4DB6AC)
    }
    val dDayText = when {
        remainingDaysInt == null -> "-"
        remainingDaysInt < 0 -> "${-remainingDaysInt}일 지남"
        remainingDaysInt == 0 -> "오늘 마감"
        else -> "${remainingDaysInt}일 남음"
    }

    val statusBg = when {
        remainingDaysInt == null -> Color(0xFFF5F5F5)
        remainingDaysInt < 0 -> Color(0xFFFFEBEE)
        remainingDaysInt <= 3 -> Color(0xFFFFF3E0)
        remainingDaysInt <= 7 -> Color(0xFFFFFDE7)
        else -> Color(0xFFE8F5E9)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit(product) },
        shape = RoundedCornerShape(14.dp),
        backgroundColor = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // 상단: 아이콘 + D-Day 뱃지
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F8E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32).copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        text = dDayText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = dDayColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 상품명
            Text(
                text = product.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF37474F),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (!product.isVisible) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFFEBEE),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("숨김", color = Color(0xFFD32F2F), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 바코드
            Text(
                text = product.barcode,
                fontSize = 11.sp,
                color = Color(0xFF90A4AE),
                maxLines = 1
            )
            // 유통기한
            Text(
                text = if (product.expirationDate.isNotBlank()) product.expirationDate.substringBefore(" ") else "유통기한 없음",
                fontSize = 11.sp,
                color = Color(0xFF78909C)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 하단 액션
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { onEdit(product) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "수정", tint = Color.Gray, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFFEF9A9A), modifier = Modifier.size(14.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("상품 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("'${product.name}' 상품을 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete(product) }) {
                    Text("삭제", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소", color = Color.Gray) }
            }
        )
    }
}

// 남은 일자 계산 함수
internal fun calculateRemainingDays(expirationDateStr: String): Int? {
    if (expirationDateStr.isBlank()) return null
    return try {
        // "yyyy-MM-dd HH:mm" 이나 "yyyy-MM-dd" 양쪽 처리
        val dateOnlyStr = if (expirationDateStr.contains(" ")) expirationDateStr.substringBefore(" ") else expirationDateStr
        val expDate = LocalDate.parse(dateOnlyStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val today = LocalDate.now()
        ChronoUnit.DAYS.between(today, expDate).toInt()
    } catch (e: Exception) {
        null
    }
}
