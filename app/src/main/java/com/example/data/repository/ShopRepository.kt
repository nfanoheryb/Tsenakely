package com.example.data.repository

import com.example.data.backup.BackupJsonCodec
import com.example.data.backup.ShopBackupData
import com.example.data.local.CreditEntryEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.ProductEntity
import com.example.data.local.SaleEntity
import com.example.data.local.SaleItemEntity
import com.example.data.local.SaleWithItems
import com.example.data.local.TsenakelyDao
import kotlinx.coroutines.flow.Flow

class ShopRepository(private val dao: TsenakelyDao) {

    val products: Flow<List<ProductEntity>> = dao.observeProducts()
    val customers: Flow<List<CustomerEntity>> = dao.observeCustomers()
    val allCreditEntries: Flow<List<CreditEntryEntity>> = dao.observeAllCreditEntries()
    val sales: Flow<List<SaleWithItems>> = dao.observeSalesWithItems()
    val expenses: Flow<List<ExpenseEntity>> = dao.observeExpenses()

    fun observeCustomerCreditEntries(customerId: Long): Flow<List<CreditEntryEntity>> =
        dao.observeCreditEntriesForCustomer(customerId)

    suspend fun saveProduct(product: ProductEntity): Long {
        val now = System.currentTimeMillis()
        return if (product.id == 0L) {
            dao.insertProduct(product.copy(updatedAt = now))
        } else {
            dao.updateProduct(product.copy(updatedAt = now))
            product.id
        }
    }

    suspend fun adjustStock(productId: Long, delta: Double) {
        dao.adjustProductStock(productId, delta)
    }

    suspend fun deleteProduct(productId: Long) {
        dao.deleteProductById(productId)
    }

    suspend fun recordSale(
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        newCustomerName: String? = null,
        newCustomerPhone: String = ""
    ): Long {
        return dao.recordSaleTransaction(
            sale = sale,
            items = items,
            newCustomerName = newCustomerName,
            newCustomerPhone = newCustomerPhone
        )
    }

    suspend fun voidSale(saleWithItems: SaleWithItems) {
        dao.voidSaleAndRestoreStock(saleWithItems)
    }

    suspend fun createCustomer(name: String, phone: String): Long {
        return dao.insertCustomer(
            CustomerEntity(
                name = name.trim(),
                phone = phone.trim(),
                balanceAr = 0L,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteCustomer(customerId: Long) {
        dao.deleteCustomerById(customerId)
    }

    suspend fun recordCreditPayment(customerId: Long, amountAr: Long, note: String) {
        dao.recordCreditPaymentTransaction(customerId, amountAr, note)
    }

    suspend fun addManualCredit(customerId: Long, amountAr: Long, note: String) {
        dao.addManualCreditTransaction(customerId, amountAr, note)
    }

    suspend fun addExpense(title: String, category: String, amountAr: Long): Long {
        return dao.insertExpense(
            ExpenseEntity(
                title = title.trim(),
                category = category,
                amountAr = amountAr,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteExpense(expenseId: Long) {
        dao.deleteExpenseById(expenseId)
    }

    /**
     * Optional one-tap sample catalog requested explicitly by the user when exploring an empty shop.
     */
    suspend fun loadSampleMalagasyCatalog() {
        val now = System.currentTimeMillis()
        val samples = listOf(
            ProductEntity(
                name = "Vary Makalioka",
                category = "Sakafo",
                unit = "kg",
                costPriceAr = 2800L,
                sellPriceAr = 3200L,
                stockQty = 50.0,
                minStockAlert = 10.0,
                updatedAt = now
            ),
            ProductEntity(
                name = "Menaka Hita 1L",
                category = "Sakafo",
                unit = "L",
                costPriceAr = 7800L,
                sellPriceAr = 8800L,
                stockQty = 18.0,
                minStockAlert = 5.0,
                updatedAt = now
            ),
            ProductEntity(
                name = "Siramamy Mena",
                category = "Sakafo",
                unit = "kg",
                costPriceAr = 4200L,
                sellPriceAr = 4800L,
                stockQty = 25.0,
                minStockAlert = 6.0,
                updatedAt = now
            ),
            ProductEntity(
                name = "Rano 1.5L",
                category = "Zava-pisotro",
                unit = "isa",
                costPriceAr = 1800L,
                sellPriceAr = 2500L,
                stockQty = 24.0,
                minStockAlert = 6.0,
                updatedAt = now
            ),
            ProductEntity(
                name = "Kafe Gasy (fonosana)",
                category = "Zava-pisotro",
                unit = "fonosana",
                costPriceAr = 1200L,
                sellPriceAr = 1600L,
                stockQty = 15.0,
                minStockAlert = 4.0,
                updatedAt = now
            ),
            ProductEntity(
                name = "Savony Nosy",
                category = "Fidiovana",
                unit = "isa",
                costPriceAr = 1500L,
                sellPriceAr = 2000L,
                stockQty = 20.0,
                minStockAlert = 5.0,
                updatedAt = now
            ),
            ProductEntity(
                name = "Atody",
                category = "Sakafo",
                unit = "isa",
                costPriceAr = 550L,
                sellPriceAr = 700L,
                stockQty = 60.0,
                minStockAlert = 12.0,
                updatedAt = now
            ),
            ProductEntity(
                name = "Labozia (boaty)",
                category = "Hafa",
                unit = "boaty",
                costPriceAr = 1100L,
                sellPriceAr = 1500L,
                stockQty = 3.0,
                minStockAlert = 5.0,
                updatedAt = now
            )
        )
        dao.insertProducts(samples)
    }

    suspend fun exportBackupJson(): String {
        val snapshot = ShopBackupData(
            products = dao.getAllProductsOnce(),
            customers = dao.getAllCustomersOnce(),
            creditEntries = dao.getAllCreditEntriesOnce(),
            sales = dao.getAllSalesOnce(),
            saleItems = dao.getAllSaleItemsOnce(),
            expenses = dao.getAllExpensesOnce()
        )
        return BackupJsonCodec.encode(snapshot)
    }

    suspend fun restoreBackup(backup: ShopBackupData) {
        dao.restoreFullBackupTransaction(
            products = backup.products,
            customers = backup.customers,
            creditEntries = backup.creditEntries,
            sales = backup.sales,
            saleItems = backup.saleItems,
            expenses = backup.expenses
        )
    }
}
