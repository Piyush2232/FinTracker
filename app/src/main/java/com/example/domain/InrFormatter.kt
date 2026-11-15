package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object InrFormatter {
    fun format(amount: Double, showDecimals: Boolean = false): String {
        val sign = if (amount < 0) "-" else ""
        val value = abs(amount)
        val useDecimals = showDecimals || (value % 1.0 != 0.0 && value < 1000.0)
        return if (useDecimals) {
            val roundedPaise = (value * 100.0).roundToLong()
            val whole = roundedPaise / 100L
            val paise = (roundedPaise % 100L).toInt()
            "$sign₹${formatIndianDigits(whole)}.${paise.toString().padStart(2, '0')}"
        } else {
            "$sign₹${formatIndianDigits(value.roundToLong())}"
        }
    }

    private fun formatIndianDigits(whole: Long): String {
        val digits = whole.toString()
        if (digits.length <= 3) return digits
        val lastThree = digits.takeLast(3)
        val prefix = digits.dropLast(3)
        val groupedPrefix = prefix.reversed().chunked(2).joinToString(",").reversed()
        return "$groupedPrefix,$lastThree"
    }

    fun formatCompact(amount: Double): String {
        val value = abs(amount)
        return when {
            value >= 1_00_00_000 -> String.format(Locale("en", "IN"), "₹%.1fCr", value / 1_00_00_000.0)
            value >= 1_00_000 -> String.format(Locale("en", "IN"), "₹%.1fL", value / 1_00_000.0)
            value >= 10_000 -> String.format(Locale("en", "IN"), "₹%.1fK", value / 1_000.0)
            else -> format(amount)
        }
    }

    fun currentMonthKey(nowMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date(nowMillis))
    }

    fun formatMonthTitle(monthKey: String): String {
        return try {
            val parsed = SimpleDateFormat("yyyy-MM", Locale.US).parse(monthKey) ?: return monthKey
            SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(parsed)
        } catch (_: Exception) {
            monthKey
        }
    }

    fun monthBoundsMillis(monthKey: String): Pair<Long, Long> {
        val parts = monthKey.split("-")
        val cal = Calendar.getInstance()
        if (parts.size == 2) {
            val year = parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR)
            val month = (parts[1].toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
            cal.set(year, month, 1, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
        } else {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis
        return start to end
    }

    fun formatTimestamp(millis: Long): String {
        val cal = Calendar.getInstance()
        val todayDay = cal.get(Calendar.DAY_OF_YEAR)
        val todayYear = cal.get(Calendar.YEAR)

        val txCal = Calendar.getInstance().apply { timeInMillis = millis }
        val txDay = txCal.get(Calendar.DAY_OF_YEAR)
        val txYear = txCal.get(Calendar.YEAR)

        val timeStr = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(millis))
        return when {
            txYear == todayYear && txDay == todayDay -> "Today · $timeStr"
            txYear == todayYear && txDay == todayDay - 1 -> "Yesterday · $timeStr"
            else -> SimpleDateFormat("dd MMM · h:mm a", Locale.ENGLISH).format(Date(millis))
        }
    }

    fun formatDayKey(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
}
