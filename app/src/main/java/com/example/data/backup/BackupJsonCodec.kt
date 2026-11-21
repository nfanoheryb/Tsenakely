package com.example.data.backup

import com.example.data.local.CreditEntryEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.ProductEntity
import com.example.data.local.SaleEntity
import com.example.data.local.SaleItemEntity
import org.json.JSONArray
import org.json.JSONObject

data class ShopBackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val products: List<ProductEntity>,
    val customers: List<CustomerEntity>,
    val creditEntries: List<CreditEntryEntity>,
    val sales: List<SaleEntity>,
    val saleItems: List<SaleItemEntity>,
    val expenses: List<ExpenseEntity>
)

object BackupJsonCodec {
    private const val FORMAT_MARKER = "tsenakely_backup"

    fun encode(data: ShopBackupData): String {
        val root = JSONObject().apply {
            put("app", FORMAT_MARKER)
            put("version", data.version)
            put("exportedAt", data.exportedAt)

            put("products", JSONArray().apply {
                data.products.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("category", p.category)
                        put("unit", p.unit)
                        put("costPriceAr", p.costPriceAr)
                        put("sellPriceAr", p.sellPriceAr)
                        put("stockQty", p.stockQty)
                        put("minStockAlert", p.minStockAlert)
                        put("updatedAt", p.updatedAt)
                    })
                }
            })

            put("customers", JSONArray().apply {
                data.customers.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("phone", c.phone)
                        put("balanceAr", c.balanceAr)
                        put("updatedAt", c.updatedAt)
                    })
                }
            })

            put("creditEntries", JSONArray().apply {
                data.creditEntries.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("customerId", e.customerId)
                        put("type", e.type)
                        put("amountAr", e.amountAr)
                        put("note", e.note)
                        put("createdAt", e.createdAt)
                    })
                }
            })

            put("sales", JSONArray().apply {
                data.sales.forEach { s ->
                    put(JSONObject().apply {
                        put("id", s.id)
                        put("totalAr", s.totalAr)
                        put("estimatedProfitAr", s.estimatedProfitAr)
                        put("paymentMethod", s.paymentMethod)
                        if (s.customerId != null) put("customerId", s.customerId) else put("customerId", JSONObject.NULL)
                        if (s.customerName != null) put("customerName", s.customerName) else put("customerName", JSONObject.NULL)
                        put("summaryText", s.summaryText)
                        put("createdAt", s.createdAt)
                    })
                }
            })

            put("saleItems", JSONArray().apply {
                data.saleItems.forEach { item ->
                    put(JSONObject().apply {
                        put("id", item.id)
                        put("saleId", item.saleId)
                        if (item.productId != null) put("productId", item.productId) else put("productId", JSONObject.NULL)
                        put("productName", item.productName)
                        put("unit", item.unit)
                        put("quantity", item.quantity)
                        put("unitPriceAr", item.unitPriceAr)
                        put("costPriceAr", item.costPriceAr)
                    })
                }
            })

            put("expenses", JSONArray().apply {
                data.expenses.forEach { ex ->
                    put(JSONObject().apply {
                        put("id", ex.id)
                        put("title", ex.title)
                        put("category", ex.category)
                        put("amountAr", ex.amountAr)
                        put("createdAt", ex.createdAt)
                    })
                }
            })
        }
        return root.toString(2)
    }

    fun decode(jsonText: String): ShopBackupData {
        val root = JSONObject(jsonText)
        val marker = root.optString("app", "")
        require(marker == FORMAT_MARKER) {
            "Tsy rakitra tahiry Tsenakely ity rakitra ity."
        }

        val productsArr = root.optJSONArray("products") ?: JSONArray()
        val products = buildList {
            for (i in 0 until productsArr.length()) {
                val o = productsArr.getJSONObject(i)
                add(
                    ProductEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        category = o.getString("category"),
                        unit = o.getString("unit"),
                        costPriceAr = o.getLong("costPriceAr"),
                        sellPriceAr = o.getLong("sellPriceAr"),
                        stockQty = o.getDouble("stockQty"),
                        minStockAlert = o.optDouble("minStockAlert", 5.0),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val customersArr = root.optJSONArray("customers") ?: JSONArray()
        val customers = buildList {
            for (i in 0 until customersArr.length()) {
                val o = customersArr.getJSONObject(i)
                add(
                    CustomerEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        phone = o.optString("phone", ""),
                        balanceAr = o.getLong("balanceAr"),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val creditArr = root.optJSONArray("creditEntries") ?: JSONArray()
        val creditEntries = buildList {
            for (i in 0 until creditArr.length()) {
                val o = creditArr.getJSONObject(i)
                add(
                    CreditEntryEntity(
                        id = o.getLong("id"),
                        customerId = o.getLong("customerId"),
                        type = o.getString("type"),
                        amountAr = o.getLong("amountAr"),
                        note = o.optString("note", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val salesArr = root.optJSONArray("sales") ?: JSONArray()
        val sales = buildList {
            for (i in 0 until salesArr.length()) {
                val o = salesArr.getJSONObject(i)
                add(
                    SaleEntity(
                        id = o.getLong("id"),
                        totalAr = o.getLong("totalAr"),
                        estimatedProfitAr = o.getLong("estimatedProfitAr"),
                        paymentMethod = o.getString("paymentMethod"),
                        customerId = if (o.isNull("customerId")) null else o.optLong("customerId"),
                        customerName = if (o.isNull("customerName")) null else o.optString("customerName"),
                        summaryText = o.optString("summaryText", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val saleItemsArr = root.optJSONArray("saleItems") ?: JSONArray()
        val saleItems = buildList {
            for (i in 0 until saleItemsArr.length()) {
                val o = saleItemsArr.getJSONObject(i)
                add(
                    SaleItemEntity(
                        id = o.getLong("id"),
                        saleId = o.getLong("saleId"),
                        productId = if (o.isNull("productId")) null else o.optLong("productId"),
                        productName = o.getString("productName"),
                        unit = o.optString("unit", "isa"),
                        quantity = o.getDouble("quantity"),
                        unitPriceAr = o.getLong("unitPriceAr"),
                        costPriceAr = o.getLong("costPriceAr")
                    )
                )
            }
        }

        val expensesArr = root.optJSONArray("expenses") ?: JSONArray()
        val expenses = buildList {
            for (i in 0 until expensesArr.length()) {
                val o = expensesArr.getJSONObject(i)
                add(
                    ExpenseEntity(
                        id = o.getLong("id"),
                        title = o.getString("title"),
                        category = o.getString("category"),
                        amountAr = o.getLong("amountAr"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        return ShopBackupData(
            version = root.optInt("version", 1),
            exportedAt = root.optLong("exportedAt", System.currentTimeMillis()),
            products = products,
            customers = customers,
            creditEntries = creditEntries,
            sales = sales,
            saleItems = saleItems,
            expenses = expenses
        )
    }
}
