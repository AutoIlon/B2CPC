package com.b2c.b2cpc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.b2c.b2cpc.model.Product
import com.b2c.b2cpc.repository.ProductRepository
import kotlinx.coroutines.launch

@Composable
fun ProductManagementScreen() {
    val repository = remember { ProductRepository() }
    val coroutineScope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // 다이얼로그 상태 변수
    var showDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    // 데이터 새로고침 함수
    fun refreshProducts() {
        isLoading = true
        coroutineScope.launch {
            products = repository.getProducts()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshProducts()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("상품 관리", style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)
            
            Button(
                onClick = { 
                    editingProduct = null
                    showDialog = true 
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF3498DB), contentColor = Color.White)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(8.dp))
                Text("새 상품 등록 (수동)")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        // 검색 및 바코드 입력 카드 영역
        Card(
            elevation = 2.dp,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().background(Color.White)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("상품명 또는 바코드 검색") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedTextField(
                    value = "",
                    onValueChange = { /* TODO: 바코드 스캐너 연동 */ },
                    label = { Text("바코드 스캐너 입력 대기창") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 상품 목록 카드 영역
        Card(
            elevation = 2.dp,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().weight(1f).background(Color.White)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val filteredProducts = products.filter {
                    it.name.contains(searchQuery, ignoreCase = true) || it.barcode.contains(searchQuery)
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // 테이블 헤더 (제거) 및 내용 목록(카드형) 교체
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredProducts) { product ->
                            ProductCard(
                                product = product,
                                onEdit = { 
                                    editingProduct = product
                                    showDialog = true 
                                },
                                onDelete = {
                                    coroutineScope.launch {
                                        val success = repository.deleteProduct(it.id)
                                        if (success) {
                                            showDialog = false
                                            refreshProducts()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 상품 등록 및 수정 다이얼로그
    if (showDialog) {
        ProductEditDialog(
            product = editingProduct,
            onDismiss = { showDialog = false },
            onSave = { newProduct ->
                coroutineScope.launch {
                    val success = if (newProduct.id.isEmpty()) {
                        // 새 상품 등록 (Firebase가 문서 ID 자동 생성할 것이므로 id는 빈 문자열 유지)
                        repository.addProduct(newProduct)
                    } else {
                        // 기존 상품 수정
                        repository.updateProduct(newProduct)
                    }
                    if (success) {
                        showDialog = false
                        refreshProducts()
                    } else {
                        // TODO: 에러 처리
                    }
                }
            },
            onDelete = { productId ->
                coroutineScope.launch {
                    val success = repository.deleteProduct(productId)
                    if (success) {
                        showDialog = false
                        refreshProducts()
                    } else {
                        // TODO: 에러 처리
                    }
                }
            }
        )
    }
}

@Composable
fun ProductEditDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "") }
    var expirationDate by remember { mutableStateOf(product?.expirationDate ?: "") }
    var isVisible by remember { mutableStateOf(product?.isVisible ?: true) }
    var type by remember { mutableStateOf(product?.type ?: "상온") }
    var alertDays by remember { mutableStateOf(product?.alertDays ?: listOf<Int>()) }
    var alertTime by remember { mutableStateOf(product?.alertTime ?: "00:00") }

    val alertOptions = listOf(7 to "7일 전", 3 to "3일 전", 1 to "1일 전", 0 to "당일", -1 to "D+1")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = 8.dp,
            modifier = Modifier.width(450.dp).padding(16.dp).heightIn(max = 600.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (product == null) "새 상품 등록" else "상품 수정",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("바코드") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("상품명") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("분류") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 보관 방식 (상온 / 저온) 선택
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("보관 방식:", fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = type == "상온", onClick = { type = "상온" })
                        Text("상온", modifier = Modifier.padding(start = 4.dp, end = 16.dp))
                        RadioButton(selected = type == "저온", onClick = { type = "저온" })
                        Text("저온 (냉장/냉동)", modifier = Modifier.padding(start = 4.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpirationDatePicker(
                        expirationDate = expirationDate.substringBefore(" "), // 날짜 부분만
                        onDateSelected = { date ->
                            val timePart = if (expirationDate.contains(" ")) expirationDate.substringAfter(" ") else "00:00"
                            expirationDate = if (type == "저온") "$date $timePart" else date
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (type == "저온") {
                        val timePart = if (expirationDate.contains(" ")) expirationDate.substringAfter(" ") else "00:00"
                        Box(modifier = Modifier.weight(1f)) {
                            CustomTimePickerContent(
                                selectedTime = timePart,
                                onTimeSelected = { time ->
                                    val datePart = expirationDate.substringBefore(" ")
                                    expirationDate = if (datePart.isNotBlank()) "$datePart $time" else "$datePart $time"
                                }
                            )
                        }
                    }
                }

                // 알림 일자 (Alert Days) 선택 칩들
                Column {
                    Text("알림 설정 (복수 선택 가능):", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        alertOptions.forEach { (optionValue, optionLabel) ->
                            val isSelected = alertDays.contains(optionValue)
                            Button(
                                onClick = {
                                    alertDays = if (isSelected) alertDays - optionValue else alertDays + optionValue
                                },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (isSelected) Color(0xFF3498DB) else Color.LightGray,
                                    contentColor = if (isSelected) Color.White else Color.Black
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).weight(1f)
                            ) {
                                Text(optionLabel, style = MaterialTheme.typography.caption)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 알림 시간 설정
                    CustomTimePickerCard(
                        selectedTime = alertTime,
                        onTimeSelected = { alertTime = it }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isVisible, onCheckedChange = { isVisible = it })
                    Text("목록에 숨기지 않고 표시 (정상 상태)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (product != null && product.id.isNotEmpty()) {
                        TextButton(
                            onClick = { onDelete(product.id) },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) {
                            Text("삭제")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    TextButton(onClick = onDismiss) {
                        Text("취소")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val newProduct = Product(
                            id = product?.id ?: "",
                            barcode = barcode,
                            name = name,
                            category = category,
                            expirationDate = expirationDate,
                            alertDays = alertDays,
                            imageUrl = product?.imageUrl ?: "",
                            imageFilename = product?.imageFilename ?: "",
                            alertTime = alertTime,
                            type = type,
                            storeId = product?.storeId ?: "",
                            isVisible = isVisible,
                            timeZone = product?.timeZone ?: java.util.TimeZone.getDefault().id
                        )
                        onSave(newProduct)
                    }) {
                        Text("저장")
                    }
                }
            }
        }
    }
}
