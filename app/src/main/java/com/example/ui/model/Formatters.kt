package com.example.ui.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object Formatters {
    private val symbols = DecimalFormatSymbols(Locale.FRANCE).apply {
        groupingSeparator = ' '
    }
    private val intFormat = DecimalFormat("#,##0", symbols)
    private val decimalFormat = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun formatAriary(amount: Long): String {
        val sign = if (amount < 0) "-" else ""
        return "$sign${intFormat.format(abs(amount))} Ar"
    }

    fun formatQty(qty: Double, unit: String = ""): String {
        val qtyStr = if (qty % 1.0 == 0.0) {
            qty.roundToLong().toString()
        } else {
            decimalFormat.format(qty)
        }
        return if (unit.isBlank()) qtyStr else "$qtyStr $unit"
    }

    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    fun formatDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))

    fun formatDate(timestamp: Long): String = dateOnlyFormat.format(Date(timestamp))

    fun isToday(timestamp: Long): Boolean {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        return now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }
}
