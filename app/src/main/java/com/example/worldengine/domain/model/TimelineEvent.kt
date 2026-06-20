package com.example.worldengine.domain.model

/**
 * A single dated event on a [World]'s timeline.
 *
 * Dates are deliberately freeform ([dateLabel]) because worlds may use fantasy/custom calendars that
 * can't be parsed as real dates. Ordering is driven separately by [sortKey] (lower = earlier), so the
 * user controls chronology regardless of how the date is written.
 *
 * [name] and [dateLabel] are required; everything else is optional. [characterId] links the event to
 * a character in the same world (nullable — the character may be unset or later deleted). [location]
 * and [duration] are freeform for now; [location] can become a Maps reference once that feature lands.
 */
data class TimelineEvent(
    val id: Long = 0,
    val worldId: Long,
    val name: String,
    val dateLabel: String,
    val sortKey: Long = 0,
    val description: String = "",
    val characterId: Long? = null,
    val location: String = "",
    val duration: String = "",
    val createdAt: Long = 0,
    /** Custom calendar this date was entered with, or null for a freeform date. See [CustomCalendar]. */
    val calendarId: String? = null,
    /** End ordering value for a period/span (e.g. a war). null = single point in time. */
    val endSortKey: Long? = null,
    /** Display label for the end of a period, or null when the event has no end. */
    val endDateLabel: String? = null,
) {
    /** True when this event spans a period rather than a single instant. */
    val isPeriod: Boolean get() = endSortKey != null
}
