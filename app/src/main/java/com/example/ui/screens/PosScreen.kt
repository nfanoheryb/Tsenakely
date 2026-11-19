package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.data.local.CustomerEntity
import com.example.data.local.PaymentMethods
import com.example.data.local.ProductCategories
import com.example.data.local.ProductEntity
import com.example.ui.model.Formatters
import com.example.ui.viewmodel.CartLine
import com.example.ui.viewmodel.ShopUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    uiState: ShopUiState,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAddProduct: (ProductEntity) -> Unit,
    onDecrementProduct: (Long) -> Unit,
    onUpdateCartQty: (String, Double) -> Unit,
    onAddCustomItem: (String, Long) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: (paymentMethod: String, customerId: Long?, newCustomerName: String?, newCustomerPhone: String) -> Unit,
    onGoToAddProduct: () -> Unit,
    onLoadSampleCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCartSheet by remember { mutableStateOf(false) }
    var showQuickSaleDialog by remember { mutableStateOf(false) }

    val filteredProducts = remember(
        uiState.products,
        uiState.posSearchQuery,
        uiState.posSelectedCategory
    ) {
        uiState.products.filter { product ->
            val matchesCategory = uiState.posSelectedCategory == ProductCategories.ALL ||
                product.category == uiState.posSelectedCategory
            val matchesQuery = uiState.posSearchQuery.isBlank() ||
                product.name.contains(uiState.posSearchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    val cartQtyByProductId = remember(uiState.cart) {
        uiState.cart.mapNotNull { line ->
            line.productId?.let { pid -> pid to line.quantity }
        }.toMap()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 740.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Left / Main Product Catalog Pane
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tsenakely Caisse",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Anio: ${Formatters.formatAriary(uiState.dailySummary.todayTotalSalesAr)} (${uiState.dailySummary.todaySalesCount} varotra)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        FilledTonalButton(
                            onClick = { showQuickSaleDialog = true },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("quick_sale_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Varotra tsotra")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TsenakelySearchField(
                        value = uiState.posSearchQuery,
                        onValueChange = onSearchChange,
                        placeholder = "Hitady entana amidy...",
                        testTag = "pos_search_input"
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    val categories = listOf(ProductCategories.ALL) + ProductCategories.LIST
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            FilterChip(
                                selected = uiState.posSelectedCategory == category,
                                onClick = { onCategoryChange(category) },
                                label = { Text(category) },
                                modifier = Modifier.height(38.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (uiState.products.isEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            item {
                                TsenakelyEmptyState(
                                    title = "Tsy mbola misy entana ao amin'ny tahiry",
                                    subtitle = "Ampidiro ny entana amidinao ao amin'ny Tsenakely mba hahafahana manao varotra haingana eto amin'ny Caisse.",
                                    primaryLabel = "Hanampy entana voalohany",
                                    onPrimaryClick = onGoToAddProduct,
                                    primaryTestTag = "empty_pos_add_product",
                                    secondaryLabel = "Andramo amin'ny entana modely",
                                    onSecondaryClick = onLoadSampleCatalog,
                                    secondaryTestTag = "empty_pos_load_sample"
                                )
                            }
                        }
                    } else if (filteredProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tsy nahitana entana mifanaraka amin'ny fikarohana.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 158.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(
                                bottom = if (!isWideLayout && uiState.cart.isNotEmpty()) 96.dp else 20.dp
                            ),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredProducts, key = { it.id }) { product ->
                                val inCartQty = cartQtyByProductId[product.id] ?: 0.0
                                PosProductCard(
                                    product = product,
                                    inCartQty = inCartQty,
                                    onAdd = { onAddProduct(product) },
                                    onRemove = { onDecrementProduct(product.id) }
                                )
                            }
                        }
                    }
                }

                // Floating Bottom Cart Bar on compact screens
                if (!isWideLayout) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = uiState.cart.isNotEmpty(),
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Surface(
                            onClick = { showCartSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_cart_bar"),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.ShoppingBasket,
                                        contentDescription = stringResource(R.string.desc_cart_icon),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${uiState.cartItemCount} entana ao anaty harona",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                                        )
                                        Text(
                                            text = Formatters.formatAriary(uiState.cartTotalAr),
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Hamarana",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Supporting Checkout Pane on Tablet / Expanded screens
            if (isWideLayout) {
                Surface(
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 2.dp
                ) {
                    CheckoutCartContent(
                        cart = uiState.cart,
                        cartTotalAr = uiState.cartTotalAr,
                        customers = uiState.customers,
                        onUpdateCartQty = onUpdateCartQty,
                        onClearCart = onClearCart,
                        onCheckout = onCheckout,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }
        }
    }

    if (showCartSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = sheetState
        ) {
            CheckoutCartContent(
                cart = uiState.cart,
                cartTotalAr = uiState.cartTotalAr,
                customers = uiState.customers,
                onUpdateCartQty = onUpdateCartQty,
                onClearCart = {
                    onClearCart()
                    showCartSheet = false
                },
                onCheckout = { method, customerId, newName, newPhone ->
                    onCheckout(method, customerId, newName, newPhone)
                    showCartSheet = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            )
        }
    }

    if (showQuickSaleDialog) {
        QuickCustomSaleDialog(
            onDismiss = { showQuickSaleDialog = false },
            onConfirm = { label, amount ->
                onAddCustomItem(label, amount)
                showQuickSaleDialog = false
            }
        )
    }
}

@Composable
private fun PosProductCard(
    product: ProductEntity,
    inCartQty: Double,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val isOut = product.stockQty <= 0.0
    val isLow = !isOut && product.stockQty <= product.minStockAlert
    val isSelected = inCartQty > 0.0

    val border = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }

    Card(
        onClick = { if (!isOut) onAdd() },
        enabled = !isOut,
        shape = RoundedCornerShape(18.dp),
        border = border,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pos_product_${product.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = categoryIcon(product.category),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = product.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                val stockBg = when {
                    isOut -> MaterialTheme.colorScheme.errorContainer
                    isLow -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
                val stockFg = when {
                    isOut -> MaterialTheme.colorScheme.onErrorContainer
                    isLow -> MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = stockBg
                ) {
                    Text(
                        text = if (isOut) "Lany" else Formatters.formatQty(product.stockQty, product.unit),
                        style = MaterialTheme.typography.labelSmall,
                        color = stockFg,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Formatters.formatAriary(product.sellPriceAr),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "/ ${product.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isSelected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = stringResource(R.string.desc_decrease_qty),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = Formatters.formatQty(inCartQty),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckoutCartContent(
    cart: List<CartLine>,
    cartTotalAr: Long,
    customers: List<CustomerEntity>,
    onUpdateCartQty: (String, Double) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: (paymentMethod: String, customerId: Long?, newCustomerName: String?, newCustomerPhone: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var paymentMethod by remember { mutableStateOf(PaymentMethods.CASH) }
    var cashGivenText by remember { mutableStateOf("") }
    var selectedCustomerId by remember { mutableStateOf<Long?>(null) }
    var useNewCustomer by remember { mutableStateOf(customers.isEmpty()) }
    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerPhone by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Harona (${cart.size})",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (cart.isNotEmpty()) {
                TextButton(onClick = onClearCart) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fafao")
                }
            }
        }

        if (cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.ShoppingBasket,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tsindrio ny entana hampidirana eto",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cart, key = { it.key }) { line ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = line.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${Formatters.formatAriary(line.unitPriceAr)} x ${Formatters.formatQty(line.quantity, line.unit)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onUpdateCartQty(line.key, -1.0) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = stringResource(R.string.desc_decrease_qty),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = Formatters.formatQty(line.quantity),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            IconButton(
                                onClick = { onUpdateCartQty(line.key, 1.0) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = stringResource(R.string.desc_increase_qty),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = Formatters.formatAriary(line.lineTotalAr),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // Payment Method Toggle
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = paymentMethod == PaymentMethods.CASH,
                onClick = { paymentMethod = PaymentMethods.CASH },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            ) {
                Text("Vola (Cash)")
            }
            SegmentedButton(
                selected = paymentMethod == PaymentMethods.CREDIT,
                onClick = { paymentMethod = PaymentMethods.CREDIT },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            ) {
                Text("Trosa (Karnety)")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (paymentMethod == PaymentMethods.CASH) {
            OutlinedTextField(
                value = cashGivenText,
                onValueChange = { raw -> cashGivenText = raw.filter { it.isDigit() } },
                label = { Text("Vola nomen'ny mpanjifa (tsy voatery)") },
                suffix = { Text("Ar") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val quickBills = listOf(1000L, 2000L, 5000L, 10000L, 20000L)
                items(quickBills) { bill ->
                    AssistChip(
                        onClick = { cashGivenText = bill.toString() },
                        label = { Text(Formatters.formatAriary(bill)) }
                    )
                }
            }
            val cashGiven = cashGivenText.toLongOrNull()
            if (cashGiven != null && cashGiven >= cartTotalAr) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Famerim-bola (Monnaie)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = Formatters.formatAriary(cashGiven - cartTotalAr),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        } else {
            // Credit customer selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mpanjifa mampiasa trosa",
                    style = MaterialTheme.typography.labelLarge
                )
                if (customers.isNotEmpty()) {
                    TextButton(onClick = { useNewCustomer = !useNewCustomer }) {
                        Icon(
                            imageVector = Icons.Rounded.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (useNewCustomer) "Hisafidy ao" else "Mpanjifa vaovao")
                    }
                }
            }

            if (useNewCustomer || customers.isEmpty()) {
                OutlinedTextField(
                    value = newCustomerName,
                    onValueChange = { newCustomerName = it },
                    label = { Text("Anaran'ny mpanjifa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newCustomerPhone,
                    onValueChange = { newCustomerPhone = it },
                    label = { Text("Finday (tsy voatery)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(customers, key = { it.id }) { customer ->
                        FilterChip(
                            selected = selectedCustomerId == customer.id,
                            onClick = { selectedCustomerId = customer.id },
                            label = {
                                Text(
                                    "${customer.name} (${Formatters.formatAriary(customer.balanceAr)})"
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Fitambarany",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = Formatters.formatAriary(cartTotalAr),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        val canSubmit = paymentMethod == PaymentMethods.CASH ||
            (useNewCustomer && newCustomerName.isNotBlank()) ||
            (!useNewCustomer && selectedCustomerId != null)

        Button(
            onClick = {
                onCheckout(
                    paymentMethod,
                    if (useNewCustomer) null else selectedCustomerId,
                    if (useNewCustomer) newCustomerName else null,
                    newCustomerPhone
                )
            },
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("checkout_confirm_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (paymentMethod == PaymentMethods.CASH) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.tertiary
                }
            )
        ) {
            Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (paymentMethod == PaymentMethods.CASH) "Raikitra (Aloa vola)" else "Raikitra (Ampidirina trosa)",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun QuickCustomSaleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Varotra tsotra haingana") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Ampidiro ny vidin'ny entana tsy mbola ao anaty lisitra.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Anaran'ny entana (ohatra: Mofo)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { raw -> amountText = raw.filter { it.isDigit() } },
                    label = { Text("Vidiny (Ar)") },
                    suffix = { Text("Ar") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amount = amountText.toLongOrNull() ?: 0L
            Button(
                onClick = { onConfirm(itemName, amount) },
                enabled = amount > 0L
            ) {
                Text("Ampidiro harona")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Aoka ihany")
            }
        }
    )
}
