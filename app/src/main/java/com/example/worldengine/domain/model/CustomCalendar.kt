package com.example.worldengine.domain.model

import kotlinx.serialization.Serializable

/**
 * One unit of a [CustomCalendar], e.g. "Year", "Month", "Day". [countInParent] is how many of this
 * unit fit inside the next-larger unit (e.g. 12 months in a year). It is ignored for the largest
 * (first) unit, which is unbounded.
 *
 * [valueNames] optionally names each value in order (e.g. month 1 = "January"). When present and the
 * value is in range, the name is shown instead of "Unit N". Purely cosmetic.
 */
@Serializable
data class CalendarUnit(
    val name: String,
    val countInParent: Int = 0,
    val valueNames: List<String> = emptyList(),
) {
    /** Display text for a 1-based [value]: its name if one exists, else "Unit value". */
    fun labelFor(value: Long): String =
        valueNames.getOrNull((value - 1).toInt())?.takeIf { it.isNotBlank() } ?: "$name $value"
}

/**
 * A user-defined calendar system, stored globally so it can be reused across every world. Units are
 * ordered largest → smallest. A concrete date is a list of component values aligned to [units]: the
 * top component is an absolute number (e.g. a year), each deeper component is 1-based within its
 * parent (e.g. month 1..12).
 *
 * [encode] flattens a date into a single sortable [Long] (in increments of the smallest unit) so the
 * timeline can order custom/fantasy dates; [decode] is its inverse (for editing); [format] renders a
 * human-readable label.
 *
 * [weekdayNames] is an optional repeating cycle of named days (like Mon..Sun) that loops continuously
 * across the whole calendar, anchored so that the calendar's day 0 is [weekdayStartIndex]. The
 * weekday for any date follows from its [encode]d value, since that value counts smallest units.
 */
@Serializable
data class CustomCalendar(
    val id: String,
    val name: String,
    val units: List<CalendarUnit>,
    val weekdayNames: List<String> = emptyList(),
    val weekdayStartIndex: Int = 0,
) {
    /** Flattens component values into a single ordering key (mixed-radix, top unit most significant). */
    fun encode(values: List<Long>): Long {
        if (units.isEmpty()) return 0L
        var key = values.getOrElse(0) { 0L }
        for (i in 1 until units.size) {
            val radix = units[i].countInParent.coerceAtLeast(1)
            val v = values.getOrElse(i) { 1L }.coerceAtLeast(1L)
            key = key * radix + (v - 1)
        }
        return key
    }

    /** Inverse of [encode]: recovers component values from a sort key (assumes a non-negative key). */
    fun decode(key: Long): List<Long> {
        if (units.isEmpty()) return emptyList()
        val out = LongArray(units.size)
        var s = key
        for (i in units.indices.reversed()) {
            if (i == 0) {
                out[0] = s
            } else {
                val radix = units[i].countInParent.coerceAtLeast(1)
                out[i] = (s % radix) + 1
                s /= radix
            }
        }
        return out.toList()
    }

    /** The looping weekday name for an encoded [key], or null when no weekday cycle is defined. */
    fun weekdayFor(key: Long): String? {
        val n = weekdayNames.size
        if (n == 0) return null
        val index = (((key + weekdayStartIndex) % n) + n) % n
        return weekdayNames[index.toInt()]
    }

    /** Human-readable label, e.g. "Year 3019, January, Day 12 (Monday)". */
    fun format(values: List<Long>): String {
        val datePart = units.mapIndexed { i, unit -> unit.labelFor(values.getOrElse(i) { if (i == 0) 0L else 1L }) }
            .joinToString(", ")
        val weekday = weekdayFor(encode(values))
        return if (weekday != null) "$datePart ($weekday)" else datePart
    }

    companion object {
        const val BUILT_IN_ID = "builtin-gregorian"

        /** The app's default Earth-style calendar, seeded on first run. Simplified to 30-day months. */
        fun builtInGregorian(): CustomCalendar = CustomCalendar(
            id = BUILT_IN_ID,
            name = "Earth (Gregorian)",
            units = listOf(
                CalendarUnit("Year"),
                CalendarUnit(
                    name = "Month",
                    countInParent = 12,
                    valueNames = listOf(
                        "January", "February", "March", "April", "May", "June",
                        "July", "August", "September", "October", "November", "December",
                    ),
                ),
                CalendarUnit("Day", countInParent = 30),
            ),
            weekdayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"),
            weekdayStartIndex = 0,
        )
    }
}
