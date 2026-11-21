package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

@Dao
interface TsenakelyDao {

    // --- Products ---
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE ASC")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProductById(productId: Long)

    @Query("UPDATE products SET stockQty = MAX(0.0, stockQty + :delta), updatedAt = :now WHERE id = :productId")
    suspend fun adjustProductStock(productId: Long, delta: Double, now: Long = System.currentTimeMillis())

    // --- Customers & Credit ---
    @Query("SELECT * FROM customers ORDER BY balanceAr DESC, name COLLATE NOCASE ASC")
    fun observeCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :customerId LIMIT 1")
    suspend fun getCustomerById(customerId: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun deleteCustomerById(customerId: Long)

    @Query("SELECT * FROM credit_entries WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun observeCreditEntriesForCustomer(customerId: Long): Flow<List<CreditEntryEntity>>

    @Query("SELECT * FROM credit_entries ORDER BY createdAt DESC")
    fun observeAllCreditEntries(): Flow<List<CreditEntryEntity>>

    @Insert
    suspend fun insertCreditEntry(entry: CreditEntryEntity): Long

    // --- Sales ---
    @Transaction
    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun observeSalesWithItems(): Flow<List<SaleWithItems>>

    @Insert
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Query("DELETE FROM sales WHERE id = :saleId")
    suspend fun deleteSaleById(saleId: Long)

    // --- Expenses ---
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: Long)

    // --- Atomic Business Transactions ---
    @Transaction
    suspend fun recordSaleTransaction(
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        newCustomerName: String? = null,
        newCustomerPhone: String = ""
    ): Long {
        val now = System.currentTimeMillis()
        var resolvedCustomerId = sale.customerId
        var resolvedCustomerName = sale.customerName

        if (sale.paymentMethod == PaymentMethods.CREDIT) {
            if (resolvedCustomerId == null && !newCustomerName.isNullOrBlank()) {
                val created = CustomerEntity(
                    name = newCustomerName.trim(),
                    phone = newCustomerPhone.trim(),
                    balanceAr = 0L,
                    updatedAt = now
                )
                resolvedCustomerId = insertCustomer(created)
                resolvedCustomerName = created.name
            }

            if (resolvedCustomerId != null) {
                val customer = getCustomerById(resolvedCustomerId)
                if (customer != null) {
                    resolvedCustomerName = customer.name
                    updateCustomer(
                        customer.copy(
                            balanceAr = customer.balanceAr + sale.totalAr,
                            updatedAt = now
                        )
                    )
                    insertCreditEntry(
                        CreditEntryEntity(
                            customerId = customer.id,
                            type = CreditEntryTypes.CREDIT,
                            amountAr = sale.totalAr,
                            note = "Varotra: ${sale.summaryText}",
                            createdAt = now
                        )
                    )
                }
            }
        }

        val finalSale = sale.copy(
            customerId = resolvedCustomerId,
            customerName = resolvedCustomerName,
            createdAt = now
        )
        val saleId = insertSale(finalSale)
        if (items.isNotEmpty()) {
            insertSaleItems(items.map { it.copy(saleId = saleId) })
        }

        // Deduct stock safely for catalog products
        items.forEach { line ->
            val pid = line.productId
            if (pid != null) {
                adjustProductStock(pid, -line.quantity, now)
            }
        }

        return saleId
    }

    @Transaction
    suspend fun recordCreditPaymentTransaction(
        customerId: Long,
        amountAr: Long,
        note: String
    ) {
        if (amountAr <= 0L) return
        val now = System.currentTimeMillis()
        val customer = getCustomerById(customerId) ?: return
        val newBalance = max(0L, customer.balanceAr - amountAr)
        updateCustomer(customer.copy(balanceAr = newBalance, updatedAt = now))
        insertCreditEntry(
            CreditEntryEntity(
                customerId = customerId,
                type = CreditEntryTypes.PAYMENT,
                amountAr = amountAr,
                note = note.ifBlank { "Fandoavana trosa" },
                createdAt = now
            )
        )
    }

    @Transaction
    suspend fun addManualCreditTransaction(
        customerId: Long,
        amountAr: Long,
        note: String
    ) {
        if (amountAr <= 0L) return
        val now = System.currentTimeMillis()
        val customer = getCustomerById(customerId) ?: return
        updateCustomer(
            customer.copy(
                balanceAr = customer.balanceAr + amountAr,
                updatedAt = now
            )
        )
        insertCreditEntry(
            CreditEntryEntity(
                customerId = customerId,
                type = CreditEntryTypes.CREDIT,
                amountAr = amountAr,
                note = note.ifBlank { "Trosa fanampiny" },
                createdAt = now
            )
        )
    }

    @Transaction
    suspend fun voidSaleAndRestoreStock(saleWithItems: SaleWithItems) {
        val now = System.currentTimeMillis()
        saleWithItems.items.forEach { item ->
            val pid = item.productId
            if (pid != null) {
                adjustProductStock(pid, item.quantity, now)
            }
        }
        val sale = saleWithItems.sale
        if (sale.paymentMethod == PaymentMethods.CREDIT && sale.customerId != null) {
            val customer = getCustomerById(sale.customerId)
            if (customer != null) {
                val updatedBalance = max(0L, customer.balanceAr - sale.totalAr)
                updateCustomer(customer.copy(balanceAr = updatedBalance, updatedAt = now))
                insertCreditEntry(
                    CreditEntryEntity(
                        customerId = customer.id,
                        type = CreditEntryTypes.PAYMENT,
                        amountAr = sale.totalAr,
                        note = "Fanafoanana varotra: ${sale.summaryText}",
                        createdAt = now
                    )
                )
            }
        }
        deleteSaleById(sale.id)
    }

    // --- Backup & Restore Support ---
    @Query("SELECT * FROM products ORDER BY id ASC")
    suspend fun getAllProductsOnce(): List<ProductEntity>

    @Query("SELECT * FROM customers ORDER BY id ASC")
    suspend fun getAllCustomersOnce(): List<CustomerEntity>

    @Query("SELECT * FROM credit_entries ORDER BY id ASC")
    suspend fun getAllCreditEntriesOnce(): List<CreditEntryEntity>

    @Query("SELECT * FROM sales ORDER BY id ASC")
    suspend fun getAllSalesOnce(): List<SaleEntity>

    @Query("SELECT * FROM sale_items ORDER BY id ASC")
    suspend fun getAllSaleItemsOnce(): List<SaleItemEntity>

    @Query("SELECT * FROM expenses ORDER BY id ASC")
    suspend fun getAllExpensesOnce(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditEntries(entries: List<CreditEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<SaleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Query("DELETE FROM sale_items")
    suspend fun clearSaleItems()

    @Query("DELETE FROM sales")
    suspend fun clearSales()

    @Query("DELETE FROM credit_entries")
    suspend fun clearCreditEntries()

    @Query("DELETE FROM customers")
    suspend fun clearCustomers()

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    @Transaction
    suspend fun restoreFullBackupTransaction(
        products: List<ProductEntity>,
        customers: List<CustomerEntity>,
        creditEntries: List<CreditEntryEntity>,
        sales: List<SaleEntity>,
        saleItems: List<SaleItemEntity>,
        expenses: List<ExpenseEntity>
    ) {
        clearSaleItems()
        clearSales()
        clearCreditEntries()
        clearCustomers()
        clearProducts()
        clearExpenses()

        if (products.isNotEmpty()) insertProducts(products)
        if (customers.isNotEmpty()) insertCustomers(customers)
        if (creditEntries.isNotEmpty()) insertCreditEntries(creditEntries)
        if (sales.isNotEmpty()) insertSales(sales)
        if (saleItems.isNotEmpty()) insertSaleItems(saleItems)
        if (expenses.isNotEmpty()) insertExpenses(expenses)
    }
}
