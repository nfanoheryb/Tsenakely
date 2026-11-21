package com.example.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupJsonCodec
import com.example.data.backup.ShopBackupData
import com.example.data.local.CreditEntryEntity
import com.example.data.local.CreditEntryTypes
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.PaymentMethods
import com.example.data.local.ProductCategories
import com.example.data.local.ProductEntity
import com.example.data.local.SaleEntity
import com.example.data.local.SaleItemEntity
import com.example.data.local.SaleWithItems
import com.example.data.repository.ShopRepository
import com.example.ui.model.Formatters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

enum class ShopTab {
    POS,
    INVENTORY,
    CREDIT,
    REPORTS
}

enum class InventoryFilter(val label: String) {
    ALL("Rehetra"),
    LOW("Kely sisa"),
    OUT("Lany")
}

enum class CreditFilter(val label: String) {
    ALL("Rehetra"),
    OWING("Misy trosa"),
    SETTLED("Voaloa")
}

enum class ReportPeriodMode(val label: String) {
    DAY("Andro"),
    MONTH("Volana"),
    YEAR("Taona")
}

data class CartLine(
    val key: String, // "p_<id>" for catalog item or "c_<timestamp>" for custom item
    val productId: Long?,
    val name: String,
    val unit: String,
    val unitPriceAr: Long,
    val costPriceAr: Long,
    val quantity: Double,
    val maxAvailableStock: Double? = null
) {
    val lineTotalAr: Long get() = (unitPriceAr * quantity).roundToLong()
    val lineProfitAr: Long get() = ((unitPriceAr - costPriceAr) * quantity).roundToLong()
}

data class DailySummary(
    val todaySalesCount: Int = 0,
    val todayTotalSalesAr: Long = 0L,
    val todayCashSalesAr: Long = 0L,
    val todayCreditSalesAr: Long = 0L,
    val todayDebtRepaidAr: Long = 0L,
    val todayExpensesAr: Long = 0L,
    val todayEstimatedProfitAr: Long = 0L,
    val drawerNetCashAr: Long = 0L
)

data class PeriodReportSummary(
    val mode: ReportPeriodMode = ReportPeriodMode.DAY,
    val anchorMillis: Long = System.currentTimeMillis(),
    val periodLabel: String = "",
    val isCurrentPeriod: Boolean = true,
    val salesCount: Int = 0,
    val totalSalesAr: Long = 0L,
    val cashSalesAr: Long = 0L,
    val creditSalesAr: Long = 0L,
    val debtRepaidAr: Long = 0L,
    val expensesAr: Long = 0L,
    val estimatedProfitAr: Long = 0L,
    val netCashAr: Long = 0L,
    val filteredSales: List<SaleWithItems> = emptyList(),
    val filteredExpenses: List<ExpenseEntity> = emptyList()
)

data class ShopUiState(
    val activeTab: ShopTab = ShopTab.POS,
    val products: List<ProductEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val sales: List<SaleWithItems> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val selectedCustomerId: Long? = null,
    val selectedCustomerEntries: List<CreditEntryEntity> = emptyList(),
    val posSearchQuery: String = "",
    val posSelectedCategory: String = ProductCategories.ALL,
    val inventorySearchQuery: String = "",
    val inventorySelectedCategory: String = ProductCategories.ALL,
    val inventoryFilter: InventoryFilter = InventoryFilter.ALL,
    val creditSearchQuery: String = "",
    val creditFilter: CreditFilter = CreditFilter.ALL,
    val cart: List<CartLine> = emptyList(),
    val dailySummary: DailySummary = DailySummary(),
    val periodReport: PeriodReportSummary = PeriodReportSummary(),
    val pendingRestoreBackup: ShopBackupData? = null,
    val totalDebtAr: Long = 0L,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val totalInventoryValueAr: Long = 0L
) {
    val cartTotalAr: Long get() = cart.sumOf { it.lineTotalAr }
    val cartItemCount: Int get() = cart.size

    val selectedCustomer: CustomerEntity?
        get() = selectedCustomerId?.let { id -> customers.firstOrNull { it.id == id } }
}

private data class FilterState(
    val activeTab: ShopTab = ShopTab.POS,
    val posSearch: String = "",
    val posCategory: String = ProductCategories.ALL,
    val invSearch: String = "",
    val invCategory: String = ProductCategories.ALL,
    val invFilter: InventoryFilter = InventoryFilter.ALL,
    val creditSearch: String = "",
    val creditFilter: CreditFilter = CreditFilter.ALL,
    val reportPeriodMode: ReportPeriodMode = ReportPeriodMode.DAY,
    val reportAnchorMillis: Long = System.currentTimeMillis()
)

class ShopViewModel(private val repository: ShopRepository) : ViewModel() {

    private val filterState = MutableStateFlow(FilterState())
    private val cartLines = MutableStateFlow<List<CartLine>>(emptyList())
    private val selectedCustomerId = MutableStateFlow<Long?>(null)
    private val pendingRestoreBackup = MutableStateFlow<ShopBackupData?>(null)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val selectedCustomerEntries = selectedCustomerId.flatMapLatest { cid ->
        if (cid == null) flowOf(emptyList())
        else repository.observeCustomerCreditEntries(cid)
    }

    private val dataBundle = combine(
        repository.products,
        repository.customers,
        repository.sales,
        repository.expenses,
        repository.allCreditEntries
    ) { products, customers, sales, expenses, creditEntries ->
        DataBundle(products, customers, sales, expenses, creditEntries)
    }

    private val selectionBundle = combine(
        filterState,
        cartLines,
        selectedCustomerId,
        selectedCustomerEntries,
        pendingRestoreBackup
    ) { filters, cart, customerId, customerEntries, pendingBackup ->
        SelectionBundle(filters, cart, customerId, customerEntries, pendingBackup)
    }

    val uiState: StateFlow<ShopUiState> = combine(
        dataBundle,
        selectionBundle
    ) { data, selection ->
        val now = System.currentTimeMillis()
        val todaySales = data.sales.filter { Formatters.isToday(it.sale.createdAt) }
        val todayCashSales = todaySales
            .filter { it.sale.paymentMethod == PaymentMethods.CASH }
            .sumOf { it.sale.totalAr }
        val todayCreditSales = todaySales
            .filter { it.sale.paymentMethod == PaymentMethods.CREDIT }
            .sumOf { it.sale.totalAr }
        val todayTotalSales = todayCashSales + todayCreditSales
        val todayProfit = todaySales.sumOf { it.sale.estimatedProfitAr }

        val todayRepaid = data.creditEntries
            .filter { it.type == CreditEntryTypes.PAYMENT && Formatters.isToday(it.createdAt) }
            .sumOf { it.amountAr }

        val todayExpenses = data.expenses
            .filter { Formatters.isToday(it.createdAt) }
            .sumOf { it.amountAr }

        val drawerNet = todayCashSales + todayRepaid - todayExpenses

        // Period Report computation (Andro / Volana / Taona)
        val periodMode = selection.filters.reportPeriodMode
        val anchor = selection.filters.reportAnchorMillis
        val matchesPeriod: (Long) -> Boolean = when (periodMode) {
            ReportPeriodMode.DAY -> { ts -> Formatters.isSameDay(ts, anchor) }
            ReportPeriodMode.MONTH -> { ts -> Formatters.isSameMonth(ts, anchor) }
            ReportPeriodMode.YEAR -> { ts -> Formatters.isSameYear(ts, anchor) }
        }
        val periodSales = data.sales.filter { matchesPeriod(it.sale.createdAt) }
        val periodCashSales = periodSales
            .filter { it.sale.paymentMethod == PaymentMethods.CASH }
            .sumOf { it.sale.totalAr }
        val periodCreditSales = periodSales
            .filter { it.sale.paymentMethod == PaymentMethods.CREDIT }
            .sumOf { it.sale.totalAr }
        val periodTotalSales = periodCashSales + periodCreditSales
        val periodProfit = periodSales.sumOf { it.sale.estimatedProfitAr }
        val periodRepaid = data.creditEntries
            .filter { it.type == CreditEntryTypes.PAYMENT && matchesPeriod(it.createdAt) }
            .sumOf { it.amountAr }
        val periodExpensesList = data.expenses.filter { matchesPeriod(it.createdAt) }
        val periodExpensesTotal = periodExpensesList.sumOf { it.amountAr }
        val periodNetCash = periodCashSales + periodRepaid - periodExpensesTotal

        val periodLabel = when (periodMode) {
            ReportPeriodMode.DAY -> Formatters.formatMalagasyDayLabel(anchor)
            ReportPeriodMode.MONTH -> Formatters.formatMalagasyMonthLabel(anchor)
            ReportPeriodMode.YEAR -> Formatters.formatMalagasyYearLabel(anchor)
        }
        val isCurrentPeriod = matchesPeriod(now)

        val lowStock = data.products.count { it.stockQty <= it.minStockAlert }
        val outOfStock = data.products.count { it.stockQty <= 0.0 }
        val invValue = data.products.sumOf { (it.costPriceAr * it.stockQty).roundToLong() }
        val totalDebt = data.customers.sumOf { it.balanceAr }

        ShopUiState(
            activeTab = selection.filters.activeTab,
            products = data.products,
            customers = data.customers,
            sales = data.sales,
            expenses = data.expenses,
            selectedCustomerId = selection.selectedCustomerId,
            selectedCustomerEntries = selection.customerEntries,
            posSearchQuery = selection.filters.posSearch,
            posSelectedCategory = selection.filters.posCategory,
            inventorySearchQuery = selection.filters.invSearch,
            inventorySelectedCategory = selection.filters.invCategory,
            inventoryFilter = selection.filters.invFilter,
            creditSearchQuery = selection.filters.creditSearch,
            creditFilter = selection.filters.creditFilter,
            cart = selection.cart,
            dailySummary = DailySummary(
                todaySalesCount = todaySales.size,
                todayTotalSalesAr = todayTotalSales,
                todayCashSalesAr = todayCashSales,
                todayCreditSalesAr = todayCreditSales,
                todayDebtRepaidAr = todayRepaid,
                todayExpensesAr = todayExpenses,
                todayEstimatedProfitAr = todayProfit,
                drawerNetCashAr = drawerNet
            ),
            periodReport = PeriodReportSummary(
                mode = periodMode,
                anchorMillis = anchor,
                periodLabel = periodLabel,
                isCurrentPeriod = isCurrentPeriod,
                salesCount = periodSales.size,
                totalSalesAr = periodTotalSales,
                cashSalesAr = periodCashSales,
                creditSalesAr = periodCreditSales,
                debtRepaidAr = periodRepaid,
                expensesAr = periodExpensesTotal,
                estimatedProfitAr = periodProfit,
                netCashAr = periodNetCash,
                filteredSales = periodSales,
                filteredExpenses = periodExpensesList
            ),
            pendingRestoreBackup = selection.pendingRestoreBackup,
            totalDebtAr = totalDebt,
            lowStockCount = lowStock,
            outOfStockCount = outOfStock,
            totalInventoryValueAr = invValue
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ShopUiState()
    )

    fun selectTab(tab: ShopTab) {
        filterState.update { it.copy(activeTab = tab) }
        if (tab != ShopTab.CREDIT) {
            selectedCustomerId.value = null
        }
    }

    fun notifyMessage(message: String) {
        _messages.tryEmit(message)
    }

    fun updatePosSearch(query: String) {
        filterState.update { it.copy(posSearch = query) }
    }

    fun updatePosCategory(category: String) {
        filterState.update { it.copy(posCategory = category) }
    }

    fun updateInventorySearch(query: String) {
        filterState.update { it.copy(invSearch = query) }
    }

    fun updateInventoryCategory(category: String) {
        filterState.update { it.copy(invCategory = category) }
    }

    fun updateInventoryFilter(filter: InventoryFilter) {
        filterState.update { it.copy(invFilter = filter) }
    }

    fun updateCreditSearch(query: String) {
        filterState.update { it.copy(creditSearch = query) }
    }

    fun updateCreditFilter(filter: CreditFilter) {
        filterState.update { it.copy(creditFilter = filter) }
    }

    fun openCustomerDetail(customerId: Long?) {
        selectedCustomerId.value = customerId
    }

    // --- Report Period Controls ---
    fun setReportPeriodMode(mode: ReportPeriodMode) {
        filterState.update { it.copy(reportPeriodMode = mode) }
    }

    fun shiftReportPeriod(delta: Int) {
        filterState.update { state ->
            val nextAnchor = when (state.reportPeriodMode) {
                ReportPeriodMode.DAY -> Formatters.shiftDay(state.reportAnchorMillis, delta)
                ReportPeriodMode.MONTH -> Formatters.shiftMonth(state.reportAnchorMillis, delta)
                ReportPeriodMode.YEAR -> Formatters.shiftYear(state.reportAnchorMillis, delta)
            }
            state.copy(reportAnchorMillis = nextAnchor)
        }
    }

    fun setReportAnchorMillis(anchorMillis: Long) {
        filterState.update { it.copy(reportAnchorMillis = anchorMillis) }
    }

    fun jumpReportToToday() {
        filterState.update { it.copy(reportAnchorMillis = System.currentTimeMillis()) }
    }

    // --- Backup & Restore ---
    fun exportBackupToUri(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    repository.exportBackupJson()
                }
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                        stream.flush()
                    } ?: throw IllegalStateException("Tsy azo nosokafana ny rakitra.")
                }
                _messages.emit("Voatahiry soa aman-tsara ny kopian'ny angona rehetra!")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("Tsy tontosa ny fitahirizana kopia: ${e.localizedMessage ?: "Hadisoana"}")
            }
        }
    }

    fun inspectBackupFromUri(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                val parsed = withContext(Dispatchers.IO) {
                    val text = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    } ?: throw IllegalStateException("Tsy azo novakiana ny rakitra.")
                    BackupJsonCodec.decode(text)
                }
                pendingRestoreBackup.value = parsed
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("Rakitra tsy mety na tsy kopia Tsenakely.")
            }
        }
    }

    fun dismissPendingRestore() {
        pendingRestoreBackup.value = null
    }

    fun confirmRestoreBackup() {
        val backup = pendingRestoreBackup.value ?: return
        viewModelScope.launch {
            try {
                repository.restoreBackup(backup)
                pendingRestoreBackup.value = null
                cartLines.value = emptyList()
                selectedCustomerId.value = null
                _messages.emit("Tafaverina soa aman-tsara ny angona rehetra!")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("Tsy tontosa ny famerenana angona.")
            }
        }
    }

    // --- Cart Operations ---
    fun addProductToCart(product: ProductEntity, step: Double = 1.0) {
        if (product.stockQty <= 0.0) {
            _messages.tryEmit("Lany tahiry ny ${product.name}")
            return
        }
        val key = "p_${product.id}"
        cartLines.update { current ->
            val existing = current.firstOrNull { it.key == key }
            if (existing != null) {
                val nextQty = existing.quantity + step
                if (nextQty > product.stockQty) {
                    _messages.tryEmit("Tsy ampy tahiry (${Formatters.formatQty(product.stockQty, product.unit)} sisa)")
                    current
                } else {
                    current.map {
                        if (it.key == key) it.copy(quantity = nextQty) else it
                    }
                }
            } else {
                val initialQty = minOf(step, product.stockQty)
                current + CartLine(
                    key = key,
                    productId = product.id,
                    name = product.name,
                    unit = product.unit,
                    unitPriceAr = product.sellPriceAr,
                    costPriceAr = product.costPriceAr,
                    quantity = initialQty,
                    maxAvailableStock = product.stockQty
                )
            }
        }
    }

    fun decrementProductFromCart(productId: Long, step: Double = 1.0) {
        updateCartLineQuantity("p_$productId", delta = -step)
    }

    fun updateCartLineQuantity(key: String, delta: Double) {
        cartLines.update { current ->
            current.mapNotNull { line ->
                if (line.key != key) line
                else {
                    val nextQty = line.quantity + delta
                    when {
                        nextQty <= 0.0 -> null
                        line.maxAvailableStock != null && nextQty > line.maxAvailableStock -> {
                            _messages.tryEmit("Tsy ampy tahiry (${Formatters.formatQty(line.maxAvailableStock, line.unit)} sisa)")
                            line
                        }
                        else -> line.copy(quantity = nextQty)
                    }
                }
            }
        }
    }

    fun addCustomCartItem(name: String, amountAr: Long) {
        if (amountAr <= 0L) return
        val label = name.trim().ifBlank { "Entana tsotra" }
        cartLines.update { current ->
            current + CartLine(
                key = "c_${System.nanoTime()}",
                productId = null,
                name = label,
                unit = "isa",
                unitPriceAr = amountAr,
                costPriceAr = amountAr,
                quantity = 1.0,
                maxAvailableStock = null
            )
        }
        _messages.tryEmit("Nampidirina tao anaty harona: $label")
    }

    fun clearCart() {
        cartLines.value = emptyList()
    }

    fun checkoutCart(
        paymentMethod: String,
        customerId: Long? = null,
        newCustomerName: String? = null,
        newCustomerPhone: String = ""
    ) {
        val currentCart = cartLines.value
        if (currentCart.isEmpty()) return
        if (paymentMethod == PaymentMethods.CREDIT && customerId == null && newCustomerName.isNullOrBlank()) {
            _messages.tryEmit("Fidio na soraty ny anaran'ny mpanjifa")
            return
        }

        viewModelScope.launch {
            try {
                val totalAr = currentCart.sumOf { it.lineTotalAr }
                val profitAr = currentCart.sumOf { it.lineProfitAr }
                val summaryText = currentCart.joinToString(", ") { line ->
                    "${line.name} x${Formatters.formatQty(line.quantity)}"
                }
                val sale = SaleEntity(
                    totalAr = totalAr,
                    estimatedProfitAr = profitAr,
                    paymentMethod = paymentMethod,
                    customerId = customerId,
                    customerName = newCustomerName?.trim()?.takeIf { it.isNotEmpty() },
                    summaryText = summaryText
                )
                val saleItems = currentCart.map { line ->
                    SaleItemEntity(
                        saleId = 0L,
                        productId = line.productId,
                        productName = line.name,
                        unit = line.unit,
                        quantity = line.quantity,
                        unitPriceAr = line.unitPriceAr,
                        costPriceAr = line.costPriceAr
                    )
                }
                repository.recordSale(
                    sale = sale,
                    items = saleItems,
                    newCustomerName = newCustomerName,
                    newCustomerPhone = newCustomerPhone
                )
                cartLines.value = emptyList()
                val msg = if (paymentMethod == PaymentMethods.CREDIT) {
                    "Voasoratra trosa ${Formatters.formatAriary(totalAr)}"
                } else {
                    "Varotra voaray: ${Formatters.formatAriary(totalAr)}"
                }
                _messages.emit(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("Nisy olana teo am-piraketana ny varotra")
            }
        }
    }

    fun voidSale(saleWithItems: SaleWithItems) {
        viewModelScope.launch {
            try {
                repository.voidSale(saleWithItems)
                _messages.emit("Nofoanana ny varotra ary naverina ny tahiry")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("Tsy tontosa ny fanafoanana")
            }
        }
    }

    // --- Inventory Operations ---
    fun saveProduct(
        existingId: Long = 0L,
        name: String,
        category: String,
        unit: String,
        costPriceAr: Long,
        sellPriceAr: Long,
        stockQty: Double,
        minStockAlert: Double
    ) {
        if (name.isBlank()) {
            _messages.tryEmit("Soraty ny anaran'ny entana")
            return
        }
        viewModelScope.launch {
            try {
                repository.saveProduct(
                    ProductEntity(
                        id = existingId,
                        name = name.trim(),
                        category = category,
                        unit = unit,
                        costPriceAr = costPriceAr.coerceAtLeast(0L),
                        sellPriceAr = sellPriceAr.coerceAtLeast(0L),
                        stockQty = stockQty.coerceAtLeast(0.0),
                        minStockAlert = minStockAlert.coerceAtLeast(0.0)
                    )
                )
                _messages.emit(
                    if (existingId == 0L) "Entana nampiana: ${name.trim()}"
                    else "Entana voavaovao: ${name.trim()}"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("Tsy voatahiry ny entana")
            }
        }
    }

    fun restockProduct(productId: Long, addedQty: Double) {
        if (addedQty <= 0.0) return
        viewModelScope.launch {
            try {
                repository.adjustStock(productId, addedQty)
                _messages.emit("Tahiry nampiana +${Formatters.formatQty(addedQty)}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("Tsy tontosa ny fanampiana tahiry")
            }
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(productId)
                cartLines.update { list -> list.filterNot { it.productId == productId } }
                _messages.emit("Voafafa ny entana")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("Tsy voafafa ny entana")
            }
        }
    }

    fun loadSampleCatalog() {
        viewModelScope.launch {
            try {
                repository.loadSampleMalagasyCatalog()
                _messages.emit("Nampidirina ny entana modely fanombohana")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("Tsy tontosa ny fampidirana entana modely")
            }
        }
    }

    // --- Customer & Credit Operations ---
    fun addCustomer(name: String, phone: String, initialDebtAr: Long = 0L, note: String = "") {
        if (name.isBlank()) {
            _messages.tryEmit("Soraty ny anaran'ny mpanjifa")
            return
        }
        viewModelScope.launch {
            try {
                val id = repository.createCustomer(name, phone)
                if (initialDebtAr > 0L) {
                    repository.addManualCredit(id, initialDebtAr, note.ifBlank { "Trosa fanombohana" })
                }
                _messages.emit("Mpanjifa voasoratra: ${name.trim()}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("Tsy voasoratra ny mpanjifa")
            }
        }
    }

    fun recordCreditPayment(customerId: Long, amountAr: Long, note: String) {
        if (amountAr <= 0L) return
        viewModelScope.launch {
            try {
                repository.recordCreditPayment(customerId, amountAr, note)
                _messages.emit("Fandoavana voaray: ${Formatters.formatAriary(amountAr)}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("Tsy tontosa ny fandoavana")
            }
        }
    }

    fun addManualCredit(customerId: Long, amountAr: Long, note: String) {
        if (amountAr <= 0L) return
        viewModelScope.launch {
            try {
                repository.addManualCredit(customerId, amountAr, note)
                _messages.emit("Trosa nampiana: ${Formatters.formatAriary(amountAr)}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("Tsy tontosa ny fanampiana trosa")
            }
        }
    }

    fun deleteCustomer(customerId: Long) {
        viewModelScope.launch {
            try {
                if (selectedCustomerId.value == customerId) {
                    selectedCustomerId.value = null
                }
                repository.deleteCustomer(customerId)
                _messages.emit("Voafafa ny mpanjifa")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("Tsy voafafa ny mpanjifa")
            }
        }
    }

    // --- Expense Operations ---
    fun addExpense(title: String, category: String, amountAr: Long) {
        if (title.isBlank() || amountAr <= 0L) {
            _messages.tryEmit("Fenoy ny antony sy ny vola lany")
            return
        }
        viewModelScope.launch {
            try {
                repository.addExpense(title, category, amountAr)
                _messages.emit("Fandaniana voasoratra: ${Formatters.formatAriary(amountAr)}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("Tsy voatahiry ny fandaniana")
            }
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteExpense(expenseId)
                _messages.emit("Voafafa ny fandaniana")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("Tsy voafafa ny fandaniana")
            }
        }
    }

    private data class DataBundle(
        val products: List<ProductEntity>,
        val customers: List<CustomerEntity>,
        val sales: List<SaleWithItems>,
        val expenses: List<ExpenseEntity>,
        val creditEntries: List<CreditEntryEntity>
    )

    private data class SelectionBundle(
        val filters: FilterState,
        val cart: List<CartLine>,
        val selectedCustomerId: Long?,
        val customerEntries: List<CreditEntryEntity>,
        val pendingRestoreBackup: ShopBackupData?
    )

    companion object {
        fun provideFactory(repository: ShopRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ShopViewModel(repository) as T
                }
            }
    }
}
