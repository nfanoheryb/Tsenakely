package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.viewmodel.ShopTab
import com.example.ui.viewmodel.ShopViewModel

private data class NavDestination(
    val tab: ShopTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val Destinations = listOf(
    NavDestination(
        tab = ShopTab.POS,
        labelRes = R.string.tab_pos,
        selectedIcon = Icons.Filled.PointOfSale,
        unselectedIcon = Icons.Outlined.PointOfSale,
        testTag = "nav_tab_pos"
    ),
    NavDestination(
        tab = ShopTab.INVENTORY,
        labelRes = R.string.tab_inventory,
        selectedIcon = Icons.Filled.Inventory2,
        unselectedIcon = Icons.Outlined.Inventory2,
        testTag = "nav_tab_inventory"
    ),
    NavDestination(
        tab = ShopTab.CREDIT,
        labelRes = R.string.tab_credit,
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook,
        testTag = "nav_tab_credit"
    ),
    NavDestination(
        tab = ShopTab.REPORTS,
        labelRes = R.string.tab_reports,
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
        testTag = "nav_tab_reports"
    )
)

@Composable
fun TsenakelyApp(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var addProductSignal by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Support predictive/system Back when on non-POS tabs
    BackHandler(enabled = uiState.activeTab != ShopTab.POS && uiState.selectedCustomerId == null) {
        viewModel.selectTab(ShopTab.POS)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Destinations.forEach { dest ->
                            val selected = uiState.activeTab == dest.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    NavTabIcon(
                                        dest = dest,
                                        selected = selected,
                                        lowStockCount = uiState.lowStockCount
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Destinations.forEach { dest ->
                            val selected = uiState.activeTab == dest.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    NavTabIcon(
                                        dest = dest,
                                        selected = selected,
                                        lowStockCount = uiState.lowStockCount
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (uiState.activeTab) {
                        ShopTab.POS -> PosScreen(
                            uiState = uiState,
                            onSearchChange = viewModel::updatePosSearch,
                            onCategoryChange = viewModel::updatePosCategory,
                            onAddProduct = { product -> viewModel.addProductToCart(product) },
                            onDecrementProduct = { pid -> viewModel.decrementProductFromCart(pid) },
                            onUpdateCartQty = viewModel::updateCartLineQuantity,
                            onAddCustomItem = viewModel::addCustomCartItem,
                            onClearCart = viewModel::clearCart,
                            onCheckout = viewModel::checkoutCart,
                            onGoToAddProduct = {
                                viewModel.selectTab(ShopTab.INVENTORY)
                                addProductSignal += 1
                            },
                            onLoadSampleCatalog = viewModel::loadSampleCatalog,
                            onRestockProduct = viewModel::restockProduct,
                            onNotify = viewModel::notifyMessage
                        )

                        ShopTab.INVENTORY -> InventoryScreen(
                            uiState = uiState,
                            openAddDialogSignal = addProductSignal,
                            onSearchChange = viewModel::updateInventorySearch,
                            onCategoryChange = viewModel::updateInventoryCategory,
                            onFilterChange = viewModel::updateInventoryFilter,
                            onSaveProduct = viewModel::saveProduct,
                            onRestockProduct = viewModel::restockProduct,
                            onDeleteProduct = viewModel::deleteProduct,
                            onLoadSampleCatalog = viewModel::loadSampleCatalog,
                            onNotify = viewModel::notifyMessage
                        )

                        ShopTab.CREDIT -> CreditScreen(
                            uiState = uiState,
                            onSearchChange = viewModel::updateCreditSearch,
                            onFilterChange = viewModel::updateCreditFilter,
                            onOpenCustomerDetail = viewModel::openCustomerDetail,
                            onAddCustomer = viewModel::addCustomer,
                            onRecordPayment = viewModel::recordCreditPayment,
                            onAddManualCredit = viewModel::addManualCredit,
                            onDeleteCustomer = viewModel::deleteCustomer
                        )

                        ShopTab.REPORTS -> {
                            val contentResolver = androidx.compose.ui.platform.LocalContext.current.contentResolver
                            ReportsScreen(
                                uiState = uiState,
                                onPeriodModeChange = viewModel::setReportPeriodMode,
                                onShiftPeriod = viewModel::shiftReportPeriod,
                                onPickDateMillis = viewModel::setReportAnchorMillis,
                                onJumpToToday = viewModel::jumpReportToToday,
                                onExportBackupUri = { uri -> viewModel.exportBackupToUri(contentResolver, uri) },
                                onInspectBackupUri = { uri -> viewModel.inspectBackupFromUri(contentResolver, uri) },
                                onConfirmRestoreBackup = viewModel::confirmRestoreBackup,
                                onDismissRestoreBackup = viewModel::dismissPendingRestore,
                                onVoidSale = viewModel::voidSale,
                                onAddExpense = viewModel::addExpense,
                                onDeleteExpense = viewModel::deleteExpense
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavTabIcon(
    dest: NavDestination,
    selected: Boolean,
    lowStockCount: Int
) {
    val iconVector = if (selected) dest.selectedIcon else dest.unselectedIcon
    if (dest.tab == ShopTab.INVENTORY && lowStockCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(if (lowStockCount > 99) "99+" else lowStockCount.toString())
                }
            }
        ) {
            Icon(imageVector = iconVector, contentDescription = stringResource(dest.labelRes))
        }
    } else {
        Icon(imageVector = iconVector, contentDescription = stringResource(dest.labelRes))
    }
}
