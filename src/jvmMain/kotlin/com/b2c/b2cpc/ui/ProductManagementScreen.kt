package com.b2c.b2cpc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.b2c.b2cpc.model.Product
import com.b2c.b2cpc.repository.CategoryRepository
import com.b2c.b2cpc.repository.ProductRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

// 정렬 옵션
enum class SortOption(val label: String) {
    NAME_ASC("이름순 ↑"),
    NAME_DESC("이름순 ↓"),
    EXPIRY_ASC("유통기한 가까운 순"),
    EXPIRY_DESC("유통기한 먼 순"),
    CATEGORY("카테고리순")
}

@Composable
fun ProductManagementScreen() {
    val productRepository = remember { ProductRepository() }
    val categoryRepository = remember { CategoryRepository() }
    val coroutineScope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // 카테고리 필터 상태
    var selectedCategory by remember { mutableStateOf("전체") }

    // 정렬 상태
    var sortOption by remember { mutableStateOf(SortOption.EXPIRY_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }

    // 다이얼로그 상태
    var showDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    // 데이터 새로고침
    fun refreshProducts() {
        isLoading = true
        coroutineScope.launch {
            products = productRepository.getProducts()
            categories = categoryRepository.getAllCategoriesWithAlertDays()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refreshProducts() }

    Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.Start
    ) {
        // 헤더: 제목 + 새 상품 등록 + 새로고침
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text("상품 관리", style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 새로고침 버튼
                OutlinedButton(onClick = { refreshProducts() }, shape = RoundedCornerShape(8.dp)) {
                    Icon(
                            Icons.Default.Refresh,
                            contentDescription = "새로고침",
                            modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("새로고침")
                }

                Button(
                        onClick = {
                            editingProduct = null
                            showDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors =
                                ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFF3498DB),
                                        contentColor = Color.White
                                )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("새 상품 등록")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 검색 + 정렬
        Card(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("상품명, 바코드, 카테고리 검색") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // 정렬 드롭다운
                Box {
                    OutlinedButton(
                            onClick = { showSortMenu = true },
                            shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                                Icons.Default.Sort,
                                contentDescription = "정렬",
                                modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(sortOption.label, fontSize = 13.sp)
                    }

                    DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOption.values().forEach { option ->
                            DropdownMenuItem(
                                    onClick = {
                                        sortOption = option
                                        showSortMenu = false
                                    }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (sortOption == option) {
                                        Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF3498DB),
                                                modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(option.label)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 카테고리 필터 칩
        Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "전체" 칩
            FilterChip(
                    label = "전체",
                    isSelected = selectedCategory == "전체",
                    count = products.size,
                    onClick = { selectedCategory = "전체" }
            )

            // 카테고리 칩들
            val categoryNames = categories.map { it.name }
            categoryNames.forEach { catName ->
                val count = products.count { it.category == catName }
                FilterChip(
                        label = catName,
                        isSelected = selectedCategory == catName,
                        count = count,
                        onClick = { selectedCategory = catName }
                )
            }

            // "미분류" 칩
            val uncategorizedCount =
                    products.count {
                        it.category.isBlank() || categoryNames.none { cat -> cat == it.category }
                    }
            if (uncategorizedCount > 0) {
                FilterChip(
                        label = "미분류",
                        isSelected = selectedCategory == "미분류",
                        count = uncategorizedCount,
                        onClick = { selectedCategory = "미분류" }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 상품 목록
        Card(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val categoryNames = categories.map { it.name }

                // 필터링 적용
                val filteredProducts =
                        products.filter { product ->
                            // 검색 필터
                            val matchesSearch =
                                    searchQuery.isBlank() ||
                                            product.name.contains(searchQuery, ignoreCase = true) ||
                                            product.barcode.contains(
                                                    searchQuery,
                                                    ignoreCase = true
                                            ) ||
                                            product.category.contains(
                                                    searchQuery,
                                                    ignoreCase = true
                                            )

                            // 카테고리 필터
                            val matchesCategory =
                                    when (selectedCategory) {
                                        "전체" -> true
                                        "미분류" ->
                                                product.category.isBlank() ||
                                                        categoryNames.none {
                                                            it == product.category
                                                        }
                                        else -> product.category == selectedCategory
                                    }

                            matchesSearch && matchesCategory
                        }

                // 정렬 적용
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val sortedProducts =
                        when (sortOption) {
                            SortOption.NAME_ASC -> filteredProducts.sortedBy { it.name }
                            SortOption.NAME_DESC -> filteredProducts.sortedByDescending { it.name }
                            SortOption.EXPIRY_ASC ->
                                    filteredProducts.sortedBy { product ->
                                        try {
                                            val dateStr =
                                                    if (product.expirationDate.contains(" "))
                                                            product.expirationDate.substringBefore(
                                                                    " "
                                                            )
                                                    else product.expirationDate
                                            LocalDate.parse(dateStr, formatter)
                                        } catch (e: Exception) {
                                            LocalDate.MAX
                                        }
                                    }
                            SortOption.EXPIRY_DESC ->
                                    filteredProducts.sortedByDescending { product ->
                                        try {
                                            val dateStr =
                                                    if (product.expirationDate.contains(" "))
                                                            product.expirationDate.substringBefore(
                                                                    " "
                                                            )
                                                    else product.expirationDate
                                            LocalDate.parse(dateStr, formatter)
                                        } catch (e: Exception) {
                                            LocalDate.MIN
                                        }
                                    }
                            SortOption.CATEGORY -> filteredProducts.sortedBy { it.category }
                        }

                Column(modifier = Modifier.fillMaxSize()) {
                    // 결과 카운트
                    Row(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                                "총 ${sortedProducts.size}개 상품",
                                style = MaterialTheme.typography.caption,
                                color = Color.Gray
                        )
                    }

                    LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(sortedProducts) { product ->
                            ProductCard(
                                    product = product,
                                    onEdit = {
                                        editingProduct = product
                                        showDialog = true
                                    },
                                    onDelete = {
                                        coroutineScope.launch {
                                            val success = productRepository.deleteProduct(it.id)
                                            if (success) {
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

    // 상품 등록/수정 다이얼로그
    if (showDialog) {
        ProductEditDialog(
                product = editingProduct,
                categories = categories,
                onDismiss = { showDialog = false },
                onSave = { newProduct ->
                    coroutineScope.launch {
                        val success =
                                if (newProduct.id.isEmpty()) {
                                    productRepository.addProduct(newProduct)
                                } else {
                                    productRepository.updateProduct(newProduct)
                                }
                        if (success) {
                            showDialog = false
                            refreshProducts()
                        }
                    }
                },
                onDelete = { productId ->
                    coroutineScope.launch {
                        val success = productRepository.deleteProduct(productId)
                        if (success) {
                            showDialog = false
                            refreshProducts()
                        }
                    }
                }
        )
    }
}

@Composable
fun FilterChip(label: String, isSelected: Boolean, count: Int, onClick: () -> Unit) {
    Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isSelected) Color(0xFF3498DB) else Color(0xFFECF0F1),
            modifier = Modifier.clickable { onClick() }
    ) {
        Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    text = label,
                    color = if (isSelected) Color.White else Color(0xFF2C3E50),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                    shape = RoundedCornerShape(10.dp),
                    color =
                            if (isSelected) Color.White.copy(alpha = 0.3f)
                            else Color(0xFFBDC3C7).copy(alpha = 0.4f)
            ) {
                Text(
                        text = "$count",
                        color = if (isSelected) Color.White else Color(0xFF7F8C8D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ProductEditDialog(
        product: Product?,
        categories: List<Category> = emptyList(),
        onDismiss: () -> Unit,
        onSave: (Product) -> Unit,
        onDelete: (String) -> Unit
) {
    var name by remember(product) { mutableStateOf(product?.name ?: "") }
    var barcode by remember(product) { mutableStateOf(product?.barcode ?: "") }
    var category by remember(product) { mutableStateOf(product?.category ?: "") }
    var expirationDate by remember(product) { mutableStateOf(product?.expirationDate ?: "") }
    var isVisible by remember(product) { mutableStateOf(product?.isVisible ?: true) }
    var type by remember(product) { mutableStateOf(product?.type ?: "상온") }
    var alertDays by remember(product) { mutableStateOf(product?.alertDays ?: listOf<Int>()) }
    var alertTime by remember(product) { mutableStateOf(product?.alertTime ?: "00:00") }

    // 카테고리 드롭다운 상태
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var categoryInput by remember(product) { mutableStateOf(product?.category ?: "") }

    var showAlertDaysSelector by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
                shape = RoundedCornerShape(12.dp),
                elevation = 8.dp,
                modifier = Modifier.width(480.dp).padding(16.dp).heightIn(max = 650.dp)
        ) {
            Column(
                    modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
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

                // 카테고리 드롭다운 + 직접 입력
                Column {
                    Text(
                            "분류",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Box {
                        OutlinedTextField(
                                value = categoryInput,
                                onValueChange = {
                                    categoryInput = it
                                    category = it
                                },
                                placeholder = { Text("카테고리 선택 또는 직접 입력") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(
                                            onClick = {
                                                showCategoryDropdown = !showCategoryDropdown
                                            }
                                    ) {
                                        Icon(
                                                if (showCategoryDropdown) Icons.Default.ArrowDropUp
                                                else Icons.Default.ArrowDropDown,
                                                contentDescription = "카테고리 선택"
                                        )
                                    }
                                }
                        )

                        DropdownMenu(
                                expanded = showCategoryDropdown,
                                onDismissRequest = { showCategoryDropdown = false }
                        ) {
                            if (categories.isEmpty()) {
                                DropdownMenuItem(onClick = {}) {
                                    Text("등록된 카테고리 없음", color = Color.Gray)
                                }
                            } else {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                            onClick = {
                                                categoryInput = cat.name
                                                category = cat.name
                                                // 카테고리 선택 시 기본 알림일 자동 적용 (신규 등록 시에만)
                                                if (product == null) {
                                                    alertDays = cat.defaultAlertDays
                                                }
                                                showCategoryDropdown = false
                                            }
                                    ) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(cat.name)
                                            Text(
                                                    "알림: ${cat.defaultAlertDays.joinToString(",")}일",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 보관 방식 선택
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("보관 방식:", fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = type == "상온", onClick = { type = "상온" })
                        Text("상온", modifier = Modifier.padding(start = 4.dp, end = 16.dp))
                        RadioButton(selected = type == "저온", onClick = { type = "저온" })
                        Text("저온 (냉장/냉동)", modifier = Modifier.padding(start = 4.dp))
                    }
                }

                // 유통기한 선택
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpirationDatePicker(
                            expirationDate = expirationDate.substringBefore(" "),
                            onDateSelected = { date ->
                                val timePart =
                                        if (expirationDate.contains(" "))
                                                expirationDate.substringAfter(" ")
                                        else "00:00"
                                expirationDate = if (type == "저온") "$date $timePart" else date
                            },
                            modifier = Modifier.weight(1f)
                    )

                    if (type == "저온") {
                        val timePart =
                                if (expirationDate.contains(" ")) expirationDate.substringAfter(" ")
                                else "00:00"
                        Box(modifier = Modifier.weight(1f)) {
                            CustomTimePickerContent(
                                    selectedTime = timePart,
                                    onTimeSelected = { time ->
                                        val datePart = expirationDate.substringBefore(" ")
                                        expirationDate = "$datePart $time"
                                    }
                            )
                        }
                    }
                }

                // 알림 설정
                Column {
                    Text(
                            "알림 설정 (복수 선택 가능):",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedButton(
                            onClick = { showAlertDaysSelector = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                                if (alertDays.isEmpty()) "알림일 설정 (선택 안됨)"
                                else "알림일 설정: ${alertDays.size}개 선택됨"
                        )
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // 알림 시간 설정
                    CustomTimePickerCard(
                            selectedTime = alertTime,
                            onTimeSelected = { alertTime = it }
                    )
                }

                // 표시 여부
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isVisible, onCheckedChange = { isVisible = it })
                    Text("목록에 숨기지 않고 표시 (정상 상태)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 버튼 영역
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (product != null && product.id.isNotEmpty()) {
                        TextButton(
                                onClick = { onDelete(product.id) },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) { Text("삭제") }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    TextButton(onClick = onDismiss) { Text("취소") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                            onClick = {
                                val newProduct =
                                        Product(
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
                                                timeZone = product?.timeZone
                                                                ?: java.util.TimeZone.getDefault()
                                                                        .id
                                        )
                                onSave(newProduct)
                            }
                    ) { Text("저장") }
                }
            }
        }
    }
}
