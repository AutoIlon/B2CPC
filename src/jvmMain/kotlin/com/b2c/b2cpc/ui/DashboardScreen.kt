package com.b2c.b2cpc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
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
import com.b2c.b2cpc.model.Product
import com.b2c.b2cpc.repository.ProductRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class AppScreen {
    DASHBOARD, PRODUCTS, SETTINGS
}

@Composable
fun MainScreen(onLogout: () -> Unit) {
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
            Spacer(modifier = Modifier.height(16.dp))
            NavigationItem("설정", Icons.Default.Settings, currentScreen == AppScreen.SETTINGS) {
                currentScreen = AppScreen.SETTINGS
            }
            
            Spacer(modifier = Modifier.weight(1f))
            NavigationItem("PC 연동 해제", Icons.Default.Logout, false) {
                onLogout()
            }
        }

        // Right Content Area
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF5F6FA))) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> DashboardContent()
                AppScreen.PRODUCTS -> ProductManagementScreen()
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
    val commonRepo = remember { com.b2c.b2cpc.repository.CommonRepository() }
    val coroutineScope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var collections by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // 필터 관리 상태 ("ALL", "IMMINENT", "EXPIRED", "HIDDEN")
    var filterMode by remember { mutableStateOf("ALL") }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            products = repository.getProducts()
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

    // Statistics calculation
    val totalProducts = products.size
    val visibleProducts = products.count { it.isVisible }
    val hiddenProducts = totalProducts - visibleProducts
    
    // Calculate imminent products (e.g., within 7 days)
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val imminentProducts = products.count { product ->
        try {
            if (product.expirationDate.isBlank()) return@count false
            val expDate = LocalDate.parse(product.expirationDate, formatter)
            !expDate.isBefore(today) && expDate.isBefore(today.plusDays(7))
        } catch (e: Exception) {
            false
        }
    }

    val expiredProducts = products.count { product ->
        try {
            if (product.expirationDate.isBlank()) return@count false
            val expDateStr = if (product.expirationDate.contains(" ")) product.expirationDate.substringBefore(" ") else product.expirationDate
            val expDate = LocalDate.parse(expDateStr, formatter)
            expDate.isBefore(today)
        } catch (e: Exception) {
            false
        }
    }
    
    // 필터링 적용 상품 목록 도출
    val filteredProducts = when(filterMode) {
        "ALL" -> products
        "IMMINENT" -> products.filter { p -> 
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
        else -> products
    }
    
    // 상품 편집 관련 상태 (기능 연동용)
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }


    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        if (filterMode == "ALL") {
            Text("대시보드 요약", style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            // Cards Row 
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardStatCard("총 관리 상품", "$totalProducts 개", Icons.Default.Inventory, Color(0xFF3498DB), Modifier.weight(1f)) { filterMode = "ALL" }
                DashboardStatCard("임박 상품 (7일 내)", "$imminentProducts 개", Icons.Default.Warning, Color(0xFFE67E22), Modifier.weight(1f)) { filterMode = "IMMINENT" }
                DashboardStatCard("유통기한 경과", "$expiredProducts 개", Icons.Default.Error, Color(0xFFE74C3C), Modifier.weight(1f)) { filterMode = "EXPIRED" }
                DashboardStatCard("숨김 처리", "$hiddenProducts 개", Icons.Default.VisibilityOff, Color(0xFF95A5A6), Modifier.weight(1f)) { filterMode = "HIDDEN" }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("최근 등록 상품", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            // 필터링 목록 뷰 헤더
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
                val filterTitle = when(filterMode) {
                    "IMMINENT" -> "임박 상품 목록 (7일 내)"
                    "EXPIRED" -> "유통기한 경과 목록"
                    "HIDDEN" -> "숨김 처리된 상품"
                    else -> "상품 목록"
                }
                Text(filterTitle, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Product Cards List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(if (filterMode == "ALL") filteredProducts.take(15) else filteredProducts) { product ->
                ProductCard(
                    product = product,
                    onEdit = { 
                        selectedProduct = it
                        showEditDialog = true
                    },
                    onDelete = {
                        coroutineScope.launch {
                            repository.deleteProduct(it.id)
                            products = repository.getProducts() // 새로고침
                        }
                    }
                )
            }
        }
    }
    
    // 상품 편집 다이얼로그 연동
    if (showEditDialog && selectedProduct != null) {
        ProductEditDialog(
            product = selectedProduct,
            onDismiss = { showEditDialog = false },
            onSave = { updatedProduct ->
                coroutineScope.launch {
                    if (updatedProduct.id.isEmpty()) {
                        repository.addProduct(updatedProduct)
                    } else {
                        repository.updateProduct(updatedProduct)
                    }
                    products = repository.getProducts() // 데이터 리로드
                    showEditDialog = false
                }
            },
            onDelete = { productId ->
                coroutineScope.launch {
                    repository.deleteProduct(productId)
                    products = repository.getProducts() // 데이터 리로드
                    showEditDialog = false
                }
            }
        )
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
