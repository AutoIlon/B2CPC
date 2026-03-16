package com.b2c.b2cpc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.b2c.b2cpc.model.Category
import com.b2c.b2cpc.repository.CategoryRepository
import kotlinx.coroutines.launch

@Composable
fun CategoryManagementScreen() {
    val categoryRepository = remember { CategoryRepository() }
    val coroutineScope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 다이얼로그 상태
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Category?>(null) }

    // 스낵바
    val scaffoldState = rememberScaffoldState()

    fun refreshCategories() {
        isLoading = true
        coroutineScope.launch {
            categories = categoryRepository.getAllCategoriesWithAlertDays()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshCategories()
    }

    Scaffold(scaffoldState = scaffoldState) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp)
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("카테고리 관리", style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)
                    Text(
                        "모바일 B2Cmobile과 동기화됩니다",
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { refreshCategories() },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("새로고침")
                    }

                    Button(
                        onClick = {
                            editingCategory = null
                            showAddDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF3498DB),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "추가")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("카테고리 추가")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 카테고리 목록
            Card(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (categories.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("등록된 카테고리가 없습니다", color = Color.Gray, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("카테고리를 추가하거나 모바일에서 생성하세요", color = Color.LightGray, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(categories) { category ->
                            CategoryListItem(
                                category = category,
                                onEdit = {
                                    editingCategory = category
                                    showAddDialog = true
                                },
                                onDelete = {
                                    showDeleteDialog = category
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 카테고리 추가/수정 다이얼로그
    if (showAddDialog) {
        CategoryEditDialog(
            category = editingCategory,
            onDismiss = { showAddDialog = false },
            onSave = { name, alertDays ->
                coroutineScope.launch {
                    if (editingCategory != null) {
                        // 수정: 알림일만 업데이트
                        val success = categoryRepository.updateCategoryAlertDays(name, alertDays)
                        if (success) {
                            scaffoldState.snackbarHostState.showSnackbar("'$name' 카테고리가 수정되었습니다.")
                        } else {
                            scaffoldState.snackbarHostState.showSnackbar("수정 실패")
                        }
                    } else {
                        // 추가
                        val success = categoryRepository.addCategory(name, alertDays)
                        if (success) {
                            scaffoldState.snackbarHostState.showSnackbar("'$name' 카테고리가 추가되었습니다.")
                        } else {
                            scaffoldState.snackbarHostState.showSnackbar("추가 실패 (중복된 이름일 수 있습니다)")
                        }
                    }
                    showAddDialog = false
                    refreshCategories()
                }
            }
        )
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("카테고리 삭제", fontWeight = FontWeight.Bold) },
            text = {
                Text("'${showDeleteDialog!!.name}' 카테고리를 삭제하시겠습니까?\n이 카테고리에 속한 상품의 분류는 변경되지 않습니다.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val catName = showDeleteDialog!!.name
                        showDeleteDialog = null
                        coroutineScope.launch {
                            val success = categoryRepository.deleteCategory(catName)
                            if (success) {
                                scaffoldState.snackbarHostState.showSnackbar("'$catName' 카테고리가 삭제되었습니다.")
                            } else {
                                scaffoldState.snackbarHostState.showSnackbar("삭제 실패")
                            }
                            refreshCategories()
                        }
                    }
                ) {
                    Text("삭제", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun CategoryListItem(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color(0xFFF8F9FA),
        elevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 아이콘
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF3498DB).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Category,
                    contentDescription = null,
                    tint = Color(0xFF3498DB),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 카테고리 정보
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2C3E50)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    category.defaultAlertDays.sorted().forEach { day ->
                        val label = when {
                            day > 0 -> "${day}일 전"
                            day == 0 -> "당일"
                            else -> "D+${-day}"
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF3498DB).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = Color(0xFF3498DB),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // 액션 버튼
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "수정", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFFEF9A9A), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun CategoryEditDialog(
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (String, List<Int>) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var alertDays by remember { mutableStateOf(category?.defaultAlertDays ?: listOf(3)) }
    var showAlertDaysSelector by remember { mutableStateOf(false) }

    val isEditing = category != null

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = 8.dp,
            modifier = Modifier.width(420.dp).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isEditing) "카테고리 수정" else "카테고리 추가",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                // 카테고리 이름
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("카테고리 이름") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isEditing // 수정 시 이름 변경 불가
                )

                if (isEditing) {
                    Text(
                        "※ 카테고리 이름은 변경할 수 없습니다",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // 기본 알림일 선택
                Column {
                    Text("기본 알림일 (복수 선택 가능):", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    
                    OutlinedButton(
                        onClick = { showAlertDaysSelector = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(if (alertDays.isEmpty()) "알림일 설정 (선택 안됨)" else "알림일 설정: ${alertDays.size}개 선택됨")
                    }

                    if (showAlertDaysSelector) {
                        com.b2c.b2cpc.ui.components.AlertDaysSelectorDialog(
                            initialSelectedDays = alertDays,
                            onDismiss = { showAlertDaysSelector = false },
                            onConfirm = { 
                                alertDays = it
                                showAlertDaysSelector = false 
                            }
                        )
                    }

                    if (alertDays.isEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "⚠️ 최소 1개 이상의 알림일을 선택해주세요",
                            fontSize = 12.sp,
                            color = Color(0xFFE74C3C)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("취소")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && alertDays.isNotEmpty()) {
                                onSave(name, alertDays)
                            }
                        },
                        enabled = name.isNotBlank() && alertDays.isNotEmpty()
                    ) {
                        Text("저장")
                    }
                }
            }
        }
    }
}
