package io.github.youndie.kvadrant.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.Locale

/**
 * A date, as three numbers, because a picker is a control and not a calendar.
 *
 * **`kotlinx-datetime` was the obvious choice and is not made here.** Its `LocalDate` is exactly this
 * type and is the Kotlin multiplatform standard — but putting it on `kvadrant-core`'s API puts it on
 * every consumer of a Metro *button*, and a component library is not the right place to decide which
 * date library an application uses. Three integers convert to whatever the caller already has in one
 * line, and only at the one call site that opened a picker.
 *
 * The arithmetic this costs is [daysInMonth] and it is five lines of the Gregorian rule.
 *
 * @param month 1..12, not the zero-based month of the platform calendars this deliberately avoids.
 */
public data class KvadrantDate(
    val year: Int,
    val month: Int,
    val day: Int,
) {
    init {
        require(month in 1..12) { "month is 1..12, got $month" }
        require(day in 1..daysInMonth(year, month)) { "day is out of range for $year-$month: $day" }
    }
}

/** A time of day on a 24-hour clock, whatever cycle the picker shows it on. */
public data class KvadrantTime(
    val hour: Int,
    val minute: Int,
) {
    init {
        require(hour in 0..23) { "hour is 0..23, got $hour" }
        require(minute in 0..59) { "minute is 0..59, got $minute" }
    }
}

/** Which order the three columns of a date picker go in. */
public enum class KvadrantDateOrder {
    DayMonthYear,
    MonthDayYear,
    YearMonthDay,
    ;

    public companion object {
        /**
         * The order a locale writes a short date in — **from a table, and the table is the honest
         * part of this**.
         *
         * The original read `CultureInfo.DateTimeFormat.ShortDatePattern`, which is the operating
         * system's own answer for every culture it knows. Compose exposes a [Locale] with a
         * language, a script and a region and nothing that formats a date, so there is no
         * multiplatform equivalent to read. A per-platform `expect`/`actual` could reach one —
         * `java.text.DateFormat` on the JVM and Android, `Intl.DateTimeFormat().formatToParts()` in
         * a browser — and that is the faithful version; it is three source sets of parsing for a
         * column order, and a wrong actual is worse than a table that says it is a table.
         *
         * So: the two orders that are not the world's default are listed, and everything else is
         * [DayMonthYear]. That is a real claim about coverage rather than about correctness — a
         * locale missing from these lists gets day-month-year, which is right for most of them and
         * wrong quietly for the rest. The parameter on the picker is the way out.
         */
        public fun forLocale(locale: Locale): KvadrantDateOrder =
            when {
                locale.language in YEAR_FIRST_LANGUAGES -> YearMonthDay
                locale.language == "en" && locale.region in MONTH_FIRST_REGIONS -> MonthDayYear
                else -> DayMonthYear
            }

        /** Japanese, Korean, Chinese, Hungarian, Lithuanian: year first in a short date. */
        private val YEAR_FIRST_LANGUAGES = setOf("ja", "ko", "zh", "hu", "lt")

        /**
         * Month first, and it is **not** "English" — `en-GB` writes the day first. The United
         * States, its territories, and the Philippines, whose short date follows the American one.
         */
        private val MONTH_FIRST_REGIONS = setOf("US", "PH", "AS", "GU", "MP", "PR", "UM", "VI")
    }
}

/** Whether a time picker shows twelve hours and a meridiem column, or twenty-four. */
public enum class KvadrantHourCycle { Twelve, TwentyFour }

/**
 * The date picker the phone navigated to: a page of tall square columns, one per component of the
 * date, tipping in from -50°.
 *
 * This is [KvadrantLoopingSelector] and [KvadrantPickerPage] assembled — both were built, tested and
 * previewed long before the thing they are halves of existed, which is what
 * [B-42](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-42-date-and-time-pickers.md)
 * was about. The metrics are all in the selector: 148 × 148 px cells, the number at 54 px.
 *
 * **The columns wrap, which is what makes the selector "looping".** Each shows [visibleCount] values
 * centred on the current one, and the neighbours are the values on either side *modulo the range* —
 * so the cell after 31 December is 1 December rather than nothing. Tapping a neighbour moves the
 * selection by that much.
 *
 * **The day clamps when the month changes**, because 31 January has no February. Moving from a long
 * month to a short one lands on the last day of the short one, which is what every picker that
 * offers a day column has to do and the reason this component needs [daysInMonth] at all.
 *
 * **Month names are the caller's.** A component library cannot ship two hundred locales' month
 * abbreviations, and shipping one locale's would be a language decision made by a picker. With
 * [monthNames] left null the column shows the month's number, which is what a short date does in
 * most of the world anyway.
 *
 * @param order defaults to [KvadrantDateOrder.forLocale], which is a table — read its documentation
 *   before relying on it for a locale that matters.
 * @param years the range the year column moves within. Wrapping applies here too, so a hundred years
 *   either side is a range and not a limit anybody meets.
 */
@Composable
public fun KvadrantDatePicker(
    value: KvadrantDate,
    onValueChange: (KvadrantDate) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    order: KvadrantDateOrder = KvadrantDateOrder.forLocale(Locale.current),
    years: IntRange = value.year - YEAR_REACH..value.year + YEAR_REACH,
    monthNames: List<String>? = null,
    dayLabel: String? = null,
    monthLabel: String? = null,
    yearLabel: String? = null,
    visibleCount: Int = DEFAULT_VISIBLE,
    cyrillic: FontFamily? = null,
) {
    require(monthNames == null || monthNames.size == MONTHS) {
        "monthNames must name all twelve months, got ${monthNames?.size}"
    }
    KvadrantPickerPage(visible, modifier) {
        order.columns().forEach { column ->
            when (column) {
                DateColumn.Day -> {
                    WrappingSelector(
                        count = daysInMonth(value.year, value.month),
                        selected = value.day - 1,
                        onSelect = { onValueChange(value.copy(day = it + 1)) },
                        label = dayLabel,
                        visibleCount = visibleCount,
                        cyrillic = cyrillic,
                    ) { (it + 1).toString() }
                }

                DateColumn.Month -> {
                    WrappingSelector(
                        count = MONTHS,
                        selected = value.month - 1,
                        onSelect = { onValueChange(value.withMonth(it + 1)) },
                        label = monthLabel,
                        visibleCount = visibleCount,
                        cyrillic = cyrillic,
                    ) { monthNames?.get(it) ?: (it + 1).toString() }
                }

                DateColumn.Year -> {
                    WrappingSelector(
                        count = years.last - years.first + 1,
                        selected = value.year - years.first,
                        onSelect = { onValueChange(value.withYear(years.first + it)) },
                        label = yearLabel,
                        visibleCount = visibleCount,
                        cyrillic = cyrillic,
                    ) { (years.first + it).toString() }
                }
            }
        }
    }
}

/**
 * The same page with an hour and a minute on it, and a meridiem column when the cycle is twelve.
 *
 * [value] is always on a 24-hour clock whatever [hourCycle] shows, because a control that returns
 * "7" and a separate "PM" hands its caller two things to get wrong.
 *
 * **The cycle is a parameter and not derived**, unlike the date's column order. The order has three
 * discrete answers and a published source for each; whether a locale writes half past seven in the
 * evening as 19:30 or 7:30 PM is a preference the operating system holds per user, not a property of
 * the language, and Compose exposes nothing that answers it. A table would be inventing an answer
 * rather than tabulating one. Twenty-four is the default because most of the world uses it.
 */
@Composable
public fun KvadrantTimePicker(
    value: KvadrantTime,
    onValueChange: (KvadrantTime) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    hourCycle: KvadrantHourCycle = KvadrantHourCycle.TwentyFour,
    minuteStep: Int = 1,
    meridiemNames: Pair<String, String> = "AM" to "PM",
    hourLabel: String? = null,
    minuteLabel: String? = null,
    visibleCount: Int = DEFAULT_VISIBLE,
    cyrillic: FontFamily? = null,
) {
    require(minuteStep in 1..30 && MINUTES % minuteStep == 0) {
        "minuteStep must divide sixty, got $minuteStep"
    }
    val twelve = hourCycle == KvadrantHourCycle.Twelve
    KvadrantPickerPage(visible, modifier) {
        WrappingSelector(
            count = if (twelve) 12 else HOURS,
            // Twelve o'clock is the *first* cell of a twelve-hour column and the twelfth value of
            // the hour: 0 and 12 both display as 12, which is the one place this arithmetic is not
            // a modulo.
            selected = if (twelve) (value.hour % 12) else value.hour,
            onSelect = { index ->
                val hour = if (twelve) index + (value.hour / 12) * 12 else index
                onValueChange(value.copy(hour = hour))
            },
            label = hourLabel,
            visibleCount = visibleCount,
            cyrillic = cyrillic,
        ) { if (twelve && it == 0) "12" else it.toString() }

        WrappingSelector(
            count = MINUTES / minuteStep,
            selected = value.minute / minuteStep,
            onSelect = { onValueChange(value.copy(minute = it * minuteStep)) },
            label = minuteLabel,
            visibleCount = visibleCount,
            cyrillic = cyrillic,
        ) { (it * minuteStep).toString().padStart(2, '0') }

        if (twelve) {
            WrappingSelector(
                count = 2,
                selected = value.hour / 12,
                onSelect = { onValueChange(value.copy(hour = value.hour % 12 + it * 12)) },
                label = null,
                visibleCount = 2,
                cyrillic = cyrillic,
            ) { if (it == 0) meridiemNames.first else meridiemNames.second }
        }
    }
}

/**
 * How many days a Gregorian month has.
 *
 * The whole of the calendar arithmetic this library does, and the whole of what a date library would
 * have been added for. The century rule is in it because 1900 was not a leap year and 2000 was, and
 * a picker that offers 29 February 2100 is wrong in a way nobody notices for seventy years.
 */
public fun daysInMonth(
    year: Int,
    month: Int,
): Int =
    when (month) {
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

/** Moving to a shorter month keeps the day inside it: 31 January to February is the 28th or 29th. */
private fun KvadrantDate.withMonth(month: Int): KvadrantDate =
    copy(month = month, day = day.coerceAtMost(daysInMonth(year, month)))

/** And moving off a leap year keeps 29 February inside the year it lands in. */
private fun KvadrantDate.withYear(year: Int): KvadrantDate =
    copy(year = year, day = day.coerceAtMost(daysInMonth(year, month)))

private enum class DateColumn { Day, Month, Year }

private fun KvadrantDateOrder.columns(): List<DateColumn> =
    when (this) {
        KvadrantDateOrder.DayMonthYear -> listOf(DateColumn.Day, DateColumn.Month, DateColumn.Year)
        KvadrantDateOrder.MonthDayYear -> listOf(DateColumn.Month, DateColumn.Day, DateColumn.Year)
        KvadrantDateOrder.YearMonthDay -> listOf(DateColumn.Year, DateColumn.Month, DateColumn.Day)
    }

/**
 * A [KvadrantLoopingSelector] showing a window around [selected], with the ends joined.
 *
 * The selector takes a list and an index, so the wrap lives here: the window is
 * `selected - half .. selected + half` reduced modulo [count], and the index handed back is turned
 * into a value of the real range before it leaves. An even [visibleCount] puts the selection one
 * above centre, which is what a column of cells has to do with no middle.
 */
@Composable
private fun WrappingSelector(
    count: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
    label: String?,
    visibleCount: Int,
    cyrillic: FontFamily?,
    text: (Int) -> String,
) {
    val half = visibleCount / 2
    val indices =
        if (count <= visibleCount) {
            // **No window when everything fits, and the meridiem column is why.** Two values in a
            // window of two came out as `[1, 0]` when PM was selected and `[0, 1]` when AM was —
            // AM and PM swapping places depending on which was chosen, which is a wrap doing
            // exactly what it is for on a column that has no ends to join.
            (0 until count).toList()
        } else {
            (0 until visibleCount).map { ((selected - half + it) % count + count) % count }
        }
    KvadrantLoopingSelector(
        values = indices.map(text),
        selectedIndex = indices.indexOf(selected),
        onSelect = { onSelect(indices[it]) },
        label = label,
        cyrillic = cyrillic,
    )
}

private const val MONTHS = 12
private const val HOURS = 24
private const val MINUTES = 60

/** Three cells of 111 dp is what a phone page holds above the fold, which is what the picker is. */
private const val DEFAULT_VISIBLE = 3

/** A century either side, so the year column wraps somewhere nobody is looking. */
private const val YEAR_REACH = 100
