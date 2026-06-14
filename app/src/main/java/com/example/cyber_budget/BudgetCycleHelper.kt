package com.example.cyber_budget

import java.text.SimpleDateFormat
import java.util.*

/**
 * BudgetCycleHelper: Utility to calculate the start and end dates of budget cycles.
 */
object BudgetCycleHelper {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    data class DateRange(val startDate: Date, val endDate: Date)

    /**
     * Calculates the start and end dates of the current budget cycle based on the cycle start day.
     * Robust against month-end variations (e.g., 31st in a 30-day month).
     */
    fun getCurrentCycleRange(cycleDay: Int): DateRange {
        val today = Calendar.getInstance()
        
        val startCalendar = today.clone() as Calendar
        startCalendar.set(Calendar.HOUR_OF_DAY, 0)
        startCalendar.set(Calendar.MINUTE, 0)
        startCalendar.set(Calendar.SECOND, 0)
        startCalendar.set(Calendar.MILLISECOND, 0)

        val currentDay = today.get(Calendar.DAY_OF_MONTH)
        
        if (currentDay >= cycleDay) {
            // Cycle started this month
            val maxDays = startCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            startCalendar.set(Calendar.DAY_OF_MONTH, minOf(cycleDay, maxDays))
        } else {
            // Cycle started last month
            startCalendar.add(Calendar.MONTH, -1)
            val maxDays = startCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            startCalendar.set(Calendar.DAY_OF_MONTH, minOf(cycleDay, maxDays))
        }

        // End date is one cycle length minus one second
        val endCalendar = startCalendar.clone() as Calendar
        endCalendar.add(Calendar.MONTH, 1)
        endCalendar.add(Calendar.SECOND, -1)

        return DateRange(startCalendar.time, endCalendar.time)
    }

    /**
     * Calculates the start and end dates of the previous budget cycle.
     */
    fun getPreviousCycleRange(cycleDay: Int): DateRange {
        val currentRange = getCurrentCycleRange(cycleDay)
        
        val startCalendar = Calendar.getInstance()
        startCalendar.time = currentRange.startDate
        startCalendar.add(Calendar.MONTH, -1)
        
        // Adjust start to cycle day if possible
        val maxDays = startCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        startCalendar.set(Calendar.DAY_OF_MONTH, minOf(cycleDay, maxDays))

        val endCalendar = startCalendar.clone() as Calendar
        endCalendar.add(Calendar.MONTH, 1)
        endCalendar.add(Calendar.SECOND, -1)

        return DateRange(startCalendar.time, endCalendar.time)
    }

    /**
     * Checks if a given date string (yyyy-MM-dd) falls within the provided range.
     */
    fun isDateInRange(dateStr: String?, range: DateRange): Boolean {
        if (dateStr == null) return false
        return try {
            val date = sdf.parse(dateStr) ?: return false
            // Compare time in millis for accuracy
            val dateTime = date.time
            val start = range.startDate.time
            val end = range.endDate.time
            dateTime in start..end
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Calculates how many days are left in the current cycle, including today.
     */
    fun getDaysRemaining(range: DateRange): Int {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val diff = range.endDate.time - today.timeInMillis
        val days = (diff / (24 * 60 * 60 * 1000)).toInt()
        return if (days < 0) 0 else days + 1
    }
}
