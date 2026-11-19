package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

object ProductCategories {
    const val ALL = "Rehetra"
    val LIST = listOf("Sakafo", "Zava-pisotro", "Fidiovana", "Hafa")
}

object ProductUnits {
    val LIST = listOf("isa", "kg", "L", "fonosana", "boaty")
}

object ExpenseCategories {
    val LIST = listOf("Entana", "Saran-dalana", "Jiro & Rano", "Hafa")
}

object PaymentMethods {
    const val CASH = "CASH"
    const val CREDIT = "CREDIT"
}

object CreditEntryTypes {
    const val CREDIT = "CREDIT"
    const val PAYMENT = "PAYMENT"
}

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val unit: String,
    val costPriceAr: Long,
    val sellPriceAr: Long,
    val stockQty: Double,
    val minStockAlert: Double = 5.0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val balanceAr: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "credit_entries",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("customerId")]
)
data class CreditEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val type: String, // CREDIT or PAYMENT
    val amountAr: Long,
    val note: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val totalAr: Long,
    val estimatedProfitAr: Long,
    val paymentMethod: String, // CASH or CREDIT
    val customerId: Long? = null,
    val customerName: String? = null,
    val summaryText: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("saleId")]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long? = null,
    val productName: String,
    val unit: String,
    val quantity: Double,
    val unitPriceAr: Long,
    val costPriceAr: Long
)

data class SaleWithItems(
    @Embedded val sale: SaleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "saleId"
    )
    val items: List<SaleItemEntity>
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val amountAr: Long,
    val createdAt: Long = System.currentTimeMillis()
)
