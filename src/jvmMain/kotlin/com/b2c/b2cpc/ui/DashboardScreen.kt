package com.b2c.b2cpc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.b2c.b2cpc.model.Category
import com.b2c.b2cpc.model.Product
import com.b2c.b2cpc.repository.CategoryRepository
import com.b2c.b2cpc.repository.ProductRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class AppScreen {
    DASHBOARD, PRODUCTS, CATEGORIES, SETTINGS
}

@Composable
fun MainScreen(onLogout: () -> Unit, onMinimizeToTray: () -> Unit = {}) {
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left Navigation Sidebar
        Column(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .background(Color(0xFF2C3E50))
                .padding(16.dp)
        ) {
            Text("B2CPC", color = Color.White, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            NavigationItem("대시보드", Icons.Default.Dashboard, currentScreen == AppScreen.DASHBOARD) {
                currentScreen = AppScreen.DASHBOARD
            }
            Spacer(modifier = Modifier.height(16.dp))
            NavigationItem("상품 관리", Icons.Default.Inventory, currentScreen == AppScreen.PRODUCTS) {
                currentScreen = AppScreen.PRODUCTS
            }
            Spacer(modifier = Modifier.height(16.dp))
            NavigationItem("카테고리 관리", Icons.Default.Category, currentScreen == AppScreen.CATEGORIES) {
                currentScreen = AppScreen.CATEGORIES
            }
            Spacer(modifier = Modifier.height(16.dp))
            NavigationItem("설정", Icons.Default.Settings, currentScreen == AppScreen.SETTINGS) {
                currentScreen = AppScreen.SETTINGS
            }

            Spacer(modifier = Modifier.weight(1f))
            NavigationItem("트레이로 최소화", Icons.Default.SystemUpdateAlt, false) {
                onMinimizeToTray()
            }
            Spacer(modifier = Modifier.height(8.dp))
            NavigationItem("PC 연동 해제", Icons.Default.Logout, false) {
                onLogout()
            }
        }

        // Right Content Area
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF5F6FA))) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> DashboardContent()
                AppScreen.PRODUCTS -> ProductManagementScreen()
                AppScreen.CATEGORIES -> CategoryManagementScreen()
                AppScreen.SETTINGS -> SettingsScreen()
            }
        }
    }
}

@Composable
fun NavigationItem(title: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                if (isSelected) Color(0xFF34495E) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else Color.LightGray)
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, color = if (isSelected) Color.White else Color.LightGray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun DashboardContent() {
    val repository = remember { ProductRepository() }
    val categoryRepository = remember { CategoryRepository() }
    val commonRepo = remember { com.b2c.b2cpc.repository.CommonRepository() }
    val coroutineScope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var collections by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 필터 관리 상태
    var filterMode by remember { mutableStateOf("ALL") }
    // 대시보드 내 검색
    var dashboardSearchQuery by remember { mutableStateOf("") }
    // 정렬 관리 상태
    var sortMode by remember { mutableStateOf("최신순") }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            products = repository.getProducts()
            categories = categoryRepository.getAllCategoriesWithAlertDays()
            collections = commonRepo.getCollections()
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // 통계 계산
    val totalProducts = products.size
    val visibleProducts = products.count { it.isVisible }
    val hiddenProducts = totalProducts - visibleProducts

    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val imminentProducts = products.count { product ->
        if (!product.isVisible) return@count false
        try {
            if (product.expirationDate.isBlank()) return@count false
            val dateStr = if (product.expirationDate.contains(" ")) product.expirationDate.substringBefore(" ") else product.expirationDate
            val expDate = LocalDate.parse(dateStr, formatter)
            !expDate.isBefore(today) && expDate.isBefore(today.plusDays(7))
        } catch (e: Exception) {
            false
        }
    }

    val expiredProducts = products.count { product ->
        if (!product.isVisible) return@count false
        try {
            if (product.expirationDate.isBlank()) return@count false
            val expDateStr = if (product.expirationDate.contains(" ")) product.expirationDate.substringBefore(" ") else product.expirationDate
            val expDate = LocalDate.parse(expDateStr, formatter)
            expDate.isBefore(today)
        } catch (e: Exception) {
            false
        }
    }

    // 카테고리별 통계 계산
    val categoryStats = categories.map { cat ->
        val catProducts = products.filter { it.category == cat.name && it.isVisible }
        val catImminent = catProducts.count { p ->
            try {
                if (p.expirationDate.isBlank()) return@count false
                val dateStr = if (p.expirationDate.contains(" ")) p.expirationDate.substringBefore(" ") else p.expirationDate
                val expDate = LocalDate.parse(dateStr, formatter)
                !expDate.isBefore(today) && expDate.isBefore(today.plusDays(7))
            } catch (e: Exception) { false }
        }
        val catExpired = catProducts.count { p ->
            try {
                if (p.expirationDate.isBlank()) return@count false
                val dateStr = if (p.expirationDate.contains(" ")) p.expirationDate.substringBefore(" ") else p.expirationDate
                val expDate = LocalDate.parse(dateStr, formatter)
                expDate.isBefore(today)
            } catch (e: Exception) { false }
        }
        Triple(cat, catProducts.size, Pair(catImminent, catExpired))
    }

    // 필터링 적용
    val filteredProducts = when (filterMode) {
        "ALL" -> products.filter { it.isVisible }
        "IMMINENT" -> products.filter { p ->
            if (!p.isVisible) return@filter false
            try {
                if (p.expirationDate.isBlank()) false
                else {
                    val expDateStr = if (p.expirationDate.contains(" ")) p.expirationDate.substringBefore(" ") else p.expirationDate
                    val expDate = LocalDate.parse(expDateStr, formatter)
                    !expDate.isBefore(today) && expDate.isBefore(today.plusDays(7))
                }
            } catch (e: Exception) { false }
        }
        "EXPIRED" -> products.filter { p ->
            if (!p.isVisible) return@filter false
            try {
                if (p.expirationDate.isBlank()) false
                else {
                    val expDateStr = if (p.expirationDate.contains(" ")) p.expirationDate.substringBefore(" ") else p.expirationDate
                    val expDate = LocalDate.parse(expDateStr, formatter)
                    expDate.isBefore(today)
                }
            } catch (e: Exception) { false }
        }
        "HIDDEN" -> products.filter { !it.isVisible }
        else -> {
            // 카테고리 필터 (filterMode가 카테고리 이름인 경우)
            products.filter { it.category == filterMode && it.isVisible }
        }
    }

    // 편집 다이얼로그
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        if (filterMode == "ALL") {
            Text("대시보드 요약", style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            // 메인 통계 카드
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardStatCard("총 관리 상품", "$totalProducts 개", Icons.Default.Inventory, Color(0xFF3498DB), Modifier.weight(1f)) { filterMode = "ALL" }
                DashboardStatCard("임박 상품 (7일 내)", "$imminentProducts 개", Icons.Default.Warning, Color(0xFFE67E22), Modifier.weight(1f)) { filterMode = "IMMINENT" }
                DashboardStatCard("유통기한 경과", "$expiredProducts 개", Icons.Default.Error, Color(0xFFE74C3C), Modifier.weight(1f)) { filterMode = "EXPIRED" }
                DashboardStatCard("숨김 처리", "$hiddenProducts 개", Icons.Default.VisibilityOff, Color(0xFF95A5A6), Modifier.weight(1f)) { filterMode = "HIDDEN" }
            }

            // 카테고리별 통계
            if (categoryStats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("카테고리별 현황", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                val scrollState = rememberScrollState()
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        categoryStats.forEach { (cat, total, imminentExpired) ->
                            CategoryStatCard(
                                categoryName = cat.name,
                                totalCount = total,
                                imminentCount = imminentExpired.first,
                                expiredCount = imminentExpired.second,
                                onClick = { filterMode = cat.name }
                            )
                        }
                    }
                    
                    androidx.compose.foundation.HorizontalScrollbar(
                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 4.dp),
                        adapter = androidx.compose.foundation.rememberScrollbarAdapter(scrollState)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("최근 등록 상품", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    var sortExpanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { sortExpanded = true }) {
                            Text(sortMode, color = Color(0xFF2C3E50), fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF2C3E50))
                        }
                        DropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false }
                        ) {
                            listOf("최신순", "이름순", "유통기한 임박순").forEach { sortOption ->
                                DropdownMenuItem(onClick = {
                                    sortMode = sortOption
                                    sortExpanded = false
                                }) {
                                    Text(sortOption)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))

                    OutlinedTextField(
                        value = dashboardSearchQuery,
                        onValueChange = { dashboardSearchQuery = it },
                        placeholder = { Text("상품명, 바코드 검색") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        singleLine = true,
                        modifier = Modifier.height(50.dp).width(250.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF3498DB),
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            // 필터링 모드 뷰 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { filterMode = "ALL" },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF2C3E50))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("메인으로 돌아가기", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                val filterTitle = when (filterMode) {
                    "IMMINENT" -> "임박 상품 목록 (7일 내)"
                    "EXPIRED" -> "유통기한 경과 목록"
                    "HIDDEN" -> "숨김 처리된 상품"
                    else -> "카테고리: $filterMode"
                }
                Text(filterTitle, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 상품 카드 목록 (검색 및 정렬 필터 적용)
        val sortedProducts = when (sortMode) {
            "이름순" -> filteredProducts.sortedBy { it.name }
            "유통기한 임박순" -> filteredProducts.sortedBy { it.expirationDate }
            "최신순" -> filteredProducts.reversed()
            else -> filteredProducts
        }

        val finalProducts = if (dashboardSearchQuery.isNotBlank()) {
            sortedProducts.filter {
                it.name.contains(dashboardSearchQuery, ignoreCase = true) ||
                it.barcode.contains(dashboardSearchQuery, ignoreCase = true)
            }
        } else {
            sortedProducts
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(if (filterMode == "ALL" && dashboardSearchQuery.isBlank()) finalProducts.take(15) else finalProducts) { product ->
                ProductCard(
                    product = product,
                    onEdit = {
                        selectedProduct = it
                        showEditDialog = true
                    },
                    onDelete = {
                        coroutineScope.launch {
                            repository.deleteProduct(it.id)
                            products = repository.getProducts()
                        }
                    }
                )
            }
        }
    }

    // 편집 다이얼로그
    if (showEditDialog && selectedProduct != null) {
        ProductEditDialog(
            product = selectedProduct,
            categories = categories,
            onDismiss = { showEditDialog = false },
            onSave = { updatedProduct ->
                coroutineScope.launch {
                    if (updatedProduct.id.isEmpty()) {
                        repository.addProduct(updatedProduct)
                    } else {
                        repository.updateProduct(updatedProduct)
                    }
                    products = repository.getProducts()
                    showEditDialog = false
                }
            },
            onDelete = { productId ->
                coroutineScope.launch {
                    repository.deleteProduct(productId)
                    products = repository.getProducts()
                    showEditDialog = false
                }
            }
        )
    }
}

@Composable
fun CategoryStatCard(
    categoryName: String,
    totalCount: Int,
    imminentCount: Int,
    expiredCount: Int,
    onClick: () -> Unit
) {
    Card(
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(180.dp)
            .clickable { onClick() },
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = categoryName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF2C3E50)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${totalCount}개 상품",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3498DB)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (imminentCount > 0) {
                    Text(
                        text = "임박 ${imminentCount}",
                        fontSize = 11.sp,
                        color = Color(0xFFE67E22),
                        fontWeight = FontWeight.Medium
                    )
                }
                if (expiredCount > 0) {
                    Text(
                        text = "경과 ${expiredCount}",
                        fontSize = 11.sp,
                        color = Color(0xFFE74C3C),
                        fontWeight = FontWeight.Medium
                    )
                }
                if (imminentCount == 0 && expiredCount == 0) {
                    Text(
                        text = "✅ 양호",
                        fontSize = 11.sp,
                        color = Color(0xFF27AE60),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardStatCard(title: String, value: String, icon: ImageVector, iconColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(120.dp).clickable { onClick() },
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title, color = Color.Gray, style = MaterialTheme.typography.subtitle1)
                Icon(icon, contentDescription = null, tint = iconColor)
            }
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h5)
        }
    }
}
