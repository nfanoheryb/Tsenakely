package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ProductCategories
import com.example.data.local.ProductEntity
import com.example.data.local.ProductUnits
import com.example.ui.model.Formatters
import com.example.ui.viewmodel.InventoryFilter
import com.example.ui.viewmodel.ShopUiState

@Composable
fun InventoryScreen(
    uiState: ShopUiState,
    openAddDialogSignal: Int,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onFilterChange: (InventoryFilter) -> Unit,
    onSaveProduct: (
        existingId: Long,
        name: String,
        category: String,
        unit: String,
        costPriceAr: Long,
        sellPriceAr: Long,
        stockQty: Double,
        minStockAlert: Double
    ) -> Unit,
    onRestockProduct: (productId: Long, addedQty: Double) -> Unit,
    onDeleteProduct: (productId: Long) -> Unit,
    onLoadSampleCatalog: () -> Unit,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var showRestockSheet by remember { mutableStateOf(false) }
    var restockTarget by remember { mutableStateOf<ProductEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<ProductEntity?>(null) }

    LaunchedEffect(openAddDialogSignal) {
        if (openAddDialogSignal > 0) {
            editingProduct = null
            showAddEditDialog = true
        }
    }

    val filteredProducts = remember(
        uiState.products,
        uiState.inventorySearchQuery,
        uiState.inventorySelectedCategory,
        uiState.inventoryFilter
    ) {
        uiState.products.filter { product ->
            val matchesQuery = uiState.inventorySearchQuery.isBlank() ||
                product.name.contains(uiState.inventorySearchQuery, ignoreCase = true)
            val matchesCategory = uiState.inventorySelectedCategory == ProductCategories.ALL ||
                product.category == uiState.inventorySelectedCategory
            val matchesStatus = when (uiState.inventoryFilter) {
                InventoryFilter.ALL -> true
                InventoryFilter.LOW -> product.stockQty > 0.0 && product.stockQty <= product.minStockAlert
                InventoryFilter.OUT -> product.stockQty <= 0.0
            }
            matchesQuery && matchesCategory && matchesStatus
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tahiry & Entana",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = { showRestockSheet = true },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("open_out_of_stock_list_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = if (uiState.lowStockCount > 0) {
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAddCheck,
                        contentDescription = stringResource(R.string.desc_out_of_stock_list),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.lowStockCount > 0) {
                            "Lany (${uiState.lowStockCount})"
                        } else {
                            "Lisitry ny lany"
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // KPI Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiMetricCard(
                    label = "Karazany",
                    value = "${uiState.products.size}",
                    modifier = Modifier.weight(1f),
                    accentColor = MaterialTheme.colorScheme.primary
                )
                KpiMetricCard(
                    label = "Kely / Lany",
                    value = "${uiState.lowStockCount}",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inventory_low_stock_kpi"),
                    accentColor = if (uiState.lowStockCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    trailingHint = if (uiState.lowStockCount > 0) "Sokafy" else null,
                    onClick = { showRestockSheet = true }
                )
                KpiMetricCard(
                    label = "Sandan'ny tahiry",
                    value = Formatters.formatAriary(uiState.totalInventoryValueAr),
                    modifier = Modifier.weight(1.4f),
                    accentColor = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            TsenakelySearchField(
                value = uiState.inventorySearchQuery,
                onValueChange = onSearchChange,
                placeholder = "Hitady entana ao amin'ny tahiry...",
                testTag = "inventory_search_input"
            )

            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(InventoryFilter.entries) { filter ->
                    val labelText = when (filter) {
                        InventoryFilter.OUT -> if (uiState.outOfStockCount > 0) "Lany (${uiState.outOfStockCount})" else filter.label
                        InventoryFilter.LOW -> {
                            val onlyLow = (uiState.lowStockCount - uiState.outOfStockCount).coerceAtLeast(0)
                            if (onlyLow > 0) "Kely sisa ($onlyLow)" else filter.label
                        }
                        InventoryFilter.ALL -> filter.label
                    }
                    FilterChip(
                        selected = uiState.inventoryFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(labelText) }
                    )
                }
                val categories = listOf(ProductCategories.ALL) + ProductCategories.LIST
                items(categories) { category ->
                    if (category != ProductCategories.ALL) {
                        FilterChip(
                            selected = uiState.inventorySelectedCategory == category,
                            onClick = {
                                val next = if (uiState.inventorySelectedCategory == category) {
                                    ProductCategories.ALL
                                } else {
                                    category
                                }
                                onCategoryChange(next)
                            },
                            label = { Text(category) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.products.isEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    item {
                        TsenakelyEmptyState(
                            title = "Mbola foana ny tahiry",
                            subtitle = "Soraty ireo entana amidinao mba hanaraha-maso ny vidiny, ny tombony, ary ny tahiry sisa.",
                            primaryLabel = "Hanampy entana vaovao",
                            onPrimaryClick = {
                                editingProduct = null
                                showAddEditDialog = true
                            },
                            primaryTestTag = "empty_inventory_add",
                            secondaryLabel = "Andramo amin'ny entana modely",
                            onSecondaryClick = onLoadSampleCatalog,
                            secondaryTestTag = "empty_inventory_sample"
                        )
                    }
                }
            } else if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tsy misy entana mifanaraka amin'ny sivana.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        InventoryProductCard(
                            product = product,
                            onRestockClick = { restockTarget = product },
                            onEditClick = {
                                editingProduct = product
                                showAddEditDialog = true
                            },
                            onDeleteClick = { deleteTarget = product }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingProduct = null
                showAddEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_product_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.desc_add_product)
            )
        }
    }

    if (showAddEditDialog) {
        AddEditProductDialog(
            initial = editingProduct,
            onDismiss = { showAddEditDialog = false },
            onConfirm = { name, category, unit, cost, sell, qty, minAlert ->
                onSaveProduct(
                    editingProduct?.id ?: 0L,
                    name,
                    category,
                    unit,
                    cost,
                    sell,
                    qty,
                    minAlert
                )
                showAddEditDialog = false
            }
        )
    }

    restockTarget?.let { target ->
        QuickRestockDialog(
            product = target,
            onDismiss = { restockTarget = null },
            onConfirm = { added ->
                onRestockProduct(target.id, added)
                restockTarget = null
            }
        )
    }

    if (showRestockSheet) {
        OutOfStockListSheet(
            products = uiState.products,
            onDismiss = { showRestockSheet = false },
            onRestockProduct = onRestockProduct,
            onNotify = onNotify
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hamafa entana?") },
            text = { Text("Tena hofafana ao anaty lisitra ve ny \"${target.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(target.id)
                        deleteTarget = null
                    }
                ) {
                    Text("Eny, fafao")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Tsia")
                }
            }
        )
    }
}

@Composable
private fun InventoryProductCard(
    product: ProductEntity,
    onRestockClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isOut = product.stockQty <= 0.0
    val isLow = !isOut && product.stockQty <= product.minStockAlert
    val marginAr = product.sellPriceAr - product.costPriceAr

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = categoryIcon(product.category),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${product.category} • / ${product.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.desc_more_options)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Hanova") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hamafa") },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Amidy: ${Formatters.formatAriary(product.sellPriceAr)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Nividianana: ${Formatters.formatAriary(product.costPriceAr)}  •  Tombony: ${Formatters.formatAriary(marginAr)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val badgeBg = when {
                    isOut -> MaterialTheme.colorScheme.errorContainer
                    isLow -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
                val badgeFg = when {
                    isOut -> MaterialTheme.colorScheme.onErrorContainer
                    isLow -> MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                }
                val stockStatus = when {
                    isOut -> "Lany tahiry"
                    isLow -> "Kely sisa: ${Formatters.formatQty(product.stockQty, product.unit)}"
                    else -> "Tahiry: ${Formatters.formatQty(product.stockQty, product.unit)}"
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = stockStatus,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                FilledTonalButton(
                    onClick = onRestockClick,
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddBox,
                        contentDescription = stringResource(R.string.desc_restock),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Tahiry", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun AddEditProductDialog(
    initial: ProductEntity?,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        category: String,
        unit: String,
        cost: Long,
        sell: Long,
        qty: Double,
        minAlert: Double
    ) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: ProductCategories.LIST.first()) }
    var unit by remember { mutableStateOf(initial?.unit ?: ProductUnits.LIST.first()) }
    var costText by remember { mutableStateOf(initial?.costPriceAr?.toString() ?: "") }
    var sellText by remember { mutableStateOf(initial?.sellPriceAr?.toString() ?: "") }
    var stockText by remember {
        mutableStateOf(initial?.let { Formatters.formatQty(it.stockQty) } ?: "")
    }
    var alertText by remember {
        mutableStateOf(initial?.let { Formatters.formatQty(it.minStockAlert) } ?: "5")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initial == null) "Entana vaovao" else "Hanova entana")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Anaran'ny entana") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                Text("Sokajy", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ProductCategories.LIST) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Text("Fandrefesana", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ProductUnits.LIST) { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { raw -> costText = raw.filter { it.isDigit() } },
                        label = { Text("Nividianana") },
                        suffix = { Text("Ar") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellText,
                        onValueChange = { raw -> sellText = raw.filter { it.isDigit() } },
                        label = { Text("Amidy") },
                        suffix = { Text("Ar") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_sell_price_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { raw ->
                            stockText = raw.filter { it.isDigit() || it == '.' }
                        },
                        label = { Text("Tahiry ($unit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_stock_input")
                    )
                    OutlinedTextField(
                        value = alertText,
                        onValueChange = { raw ->
                            alertText = raw.filter { it.isDigit() || it == '.' }
                        },
                        label = { Text("Fampitandremana") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            val sell = sellText.toLongOrNull() ?: 0L
            val cost = costText.toLongOrNull() ?: 0L
            val qty = stockText.toDoubleOrNull() ?: 0.0
            val alert = alertText.toDoubleOrNull() ?: 5.0
            Button(
                onClick = { onConfirm(name, category, unit, cost, sell, qty, alert) },
                enabled = name.isNotBlank() && sell > 0L,
                modifier = Modifier.testTag("save_product_confirm")
            ) {
                Text("Tehirizo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Aoka ihany")
            }
        }
    )
}
