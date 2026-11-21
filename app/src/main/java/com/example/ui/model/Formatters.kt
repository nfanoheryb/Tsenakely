package com.example.ui.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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
    private val backupFileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val malagasyDays = listOf(
        "Alahady", "Alatsinainy", "Talata", "Alarobia", "Alakamisy", "Zoma", "Sabotsy"
    )
    private val malagasyMonths = listOf(
        "Janoary", "Febroary", "Martsa", "Aprily", "Mey", "Jona",
        "Jolay", "Aogositra", "Septambra", "Oktobra", "Novambra", "Desambra"
    )

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

    fun suggestedBackupFileName(now: Long = System.currentTimeMillis()): String =
        "tsenakely_tahiry_${backupFileDateFormat.format(Date(now))}.json"

    fun formatMalagasyDayLabel(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val dayName = malagasyDays[cal.get(Calendar.DAY_OF_WEEK) - 1]
        val dayNum = cal.get(Calendar.DAY_OF_MONTH)
        val monthName = malagasyMonths[cal.get(Calendar.MONTH)]
        val year = cal.get(Calendar.YEAR)
        return "$dayName $dayNum $monthName $year"
    }

    fun formatMalagasyMonthLabel(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val monthName = malagasyMonths[cal.get(Calendar.MONTH)]
        val year = cal.get(Calendar.YEAR)
        return "$monthName $year"
    }

    fun formatMalagasyYearLabel(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return "Taona ${cal.get(Calendar.YEAR)}"
    }

    fun isToday(timestamp: Long): Boolean = isSameDay(timestamp, System.currentTimeMillis())

    fun isSameDay(timestamp: Long, anchorMillis: Long): Boolean {
        val a = Calendar.getInstance().apply { timeInMillis = timestamp }
        val b = Calendar.getInstance().apply { timeInMillis = anchorMillis }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    fun isSameMonth(timestamp: Long, anchorMillis: Long): Boolean {
        val a = Calendar.getInstance().apply { timeInMillis = timestamp }
        val b = Calendar.getInstance().apply { timeInMillis = anchorMillis }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.MONTH) == b.get(Calendar.MONTH)
    }

    fun isSameYear(timestamp: Long, anchorMillis: Long): Boolean {
        val a = Calendar.getInstance().apply { timeInMillis = timestamp }
        val b = Calendar.getInstance().apply { timeInMillis = anchorMillis }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
    }

    fun shiftDay(anchorMillis: Long, deltaDays: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = anchorMillis
            add(Calendar.DAY_OF_YEAR, deltaDays)
        }.timeInMillis

    fun shiftMonth(anchorMillis: Long, deltaMonths: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = anchorMillis
            add(Calendar.MONTH, deltaMonths)
        }.timeInMillis

    fun shiftYear(anchorMillis: Long, deltaYears: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = anchorMillis
            add(Calendar.YEAR, deltaYears)
        }.timeInMillis

    /**
     * Converts Material3 DatePicker UTC midnight millis to local calendar millis at noon
     * so day/month/year matching in local timezone never shifts off by 1 day.
     */
    fun utcDatePickerMillisToLocalNoon(utcMillis: Long): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun localMillisToUtcDatePickerMillis(localMillis: Long): Long {
        val localCal = Calendar.getInstance().apply { timeInMillis = localMillis }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, localCal.get(Calendar.YEAR))
            set(Calendar.MONTH, localCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, localCal.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }
}
