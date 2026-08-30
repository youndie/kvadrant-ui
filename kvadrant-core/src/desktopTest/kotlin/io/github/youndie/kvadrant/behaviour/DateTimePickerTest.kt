package io.github.youndie.kvadrant.behaviour

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.intl.Locale
import io.github.youndie.kvadrant.components.KvadrantDate
import io.github.youndie.kvadrant.components.KvadrantDateOrder
import io.github.youndie.kvadrant.components.KvadrantDatePicker
import io.github.youndie.kvadrant.components.KvadrantHourCycle
import io.github.youndie.kvadrant.components.KvadrantTime
import io.github.youndie.kvadrant.components.KvadrantTimePicker
import io.github.youndie.kvadrant.components.daysInMonth
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What the two pickers hand back, which is the half of them that is not a picture.
 *
 * [B-42](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-42-date-and-time-pickers.md)
 * assembles `KvadrantLoopingSelector` and `KvadrantPickerPage`, both of which were already built and
 * previewed. What is new is the arithmetic between them — a wrapping window, a day that has to stay
 * inside its month, and a twelve-hour column that is not a modulo — and arithmetic is what a
 * screenshot cannot check.
 */
@OptIn(ExperimentalTestApi::class)
class DateTimePickerTest {
    @Test
    fun the_column_order_follows_the_locale() {
        assertEquals(KvadrantDateOrder.MonthDayYear, KvadrantDateOrder.forLocale(Locale("en-US")))
        // Not "English": Britain writes the day first, which is the whole reason the table keys on
        // the region and not on the language.
        assertEquals(KvadrantDateOrder.DayMonthYear, KvadrantDateOrder.forLocale(Locale("en-GB")))
        assertEquals(KvadrantDateOrder.DayMonthYear, KvadrantDateOrder.forLocale(Locale("ru-RU")))
        assertEquals(KvadrantDateOrder.YearMonthDay, KvadrantDateOrder.forLocale(Locale("ja-JP")))
        // The default for everything the table does not list, stated as a test so that changing it
        // is a decision rather than an edit.
        assertEquals(KvadrantDateOrder.DayMonthYear, KvadrantDateOrder.forLocale(Locale("sw-KE")))
    }

    @Test
    fun the_columns_are_laid_out_in_that_order() {
        // Read off the screen rather than from the enum: the order the picker *draws* is the claim,
        // and a mapping that returns the right list and a layout that ignores it look the same from
        // inside the component.
        assertEquals(listOf("день", "месяц", "год"), labelsOf(KvadrantDateOrder.DayMonthYear))
        assertEquals(listOf("месяц", "день", "год"), labelsOf(KvadrantDateOrder.MonthDayYear))
        assertEquals(listOf("год", "месяц", "день"), labelsOf(KvadrantDateOrder.YearMonthDay))
    }

    @Test
    fun the_day_stays_inside_the_month_it_moves_to() {
        // 31 January, then the month column moved to February.
        var value = KvadrantDate(2026, 1, 31)
        runComposeUiTest {
            setContent {
                KvadrantTheme {
                    KvadrantDatePicker(
                        value = value,
                        onValueChange = { value = it },
                        monthNames = MONTHS,
                        monthLabel = "месяц",
                    )
                }
            }
            // The cell after January in the wrapped window.
            onNodeWithText("фев").performClick()
        }
        assertEquals(KvadrantDate(2026, 2, 28), value, "31 January went to February without clamping")
    }

    @Test
    fun a_leap_day_survives_leaving_its_year_by_being_clamped() {
        assertEquals(29, daysInMonth(2024, 2))
        assertEquals(28, daysInMonth(2023, 2))
        // The century rule, which is the half of it nobody meets for seventy years.
        assertEquals(28, daysInMonth(2100, 2))
        assertEquals(29, daysInMonth(2000, 2))

        var value = KvadrantDate(2024, 2, 29)
        runComposeUiTest {
            setContent {
                KvadrantTheme {
                    KvadrantDatePicker(
                        value = value,
                        onValueChange = { value = it },
                        years = 2023..2025,
                        yearLabel = "год",
                    )
                }
            }
            onNodeWithText("2023").performClick()
        }
        assertEquals(KvadrantDate(2023, 2, 28), value, "29 February survived a move to 2023")
    }

    @Test
    fun the_day_column_wraps_at_the_end_of_the_month() {
        var value = KvadrantDate(2026, 3, 1)
        runComposeUiTest {
            setContent {
                KvadrantTheme {
                    KvadrantDatePicker(
                        value = value,
                        onValueChange = { value = it },
                        monthNames = MONTHS,
                    )
                }
            }
            // The cell *above* the first of the month is the last of it, not nothing: that is what
            // makes the selector a looping one.
            onNodeWithText("31").performClick()
        }
        assertEquals(KvadrantDate(2026, 3, 31), value)
    }

    @Test
    fun a_twelve_hour_column_is_not_a_modulo() {
        // Half past one in the afternoon. Tapping the cell that reads "12" has to mean noon, not
        // midnight: the hour the column shows is `hour % 12` and the half of the day is carried
        // separately, so this is the one place the arithmetic is not a modulo either way.
        assertEquals(KvadrantTime(12, 30), tapping("12", KvadrantTime(13, 30)))
        // And from the morning, the same cell is midnight.
        assertEquals(KvadrantTime(0, 30), tapping("12", KvadrantTime(1, 30)))
        // The meridiem column moves the hour by twelve and leaves the minute alone.
        assertEquals(KvadrantTime(1, 30), tapping("AM", KvadrantTime(13, 30)))
        assertEquals(KvadrantTime(13, 30), tapping("PM", KvadrantTime(1, 30)))
        // Midnight is in the morning half, which is the row a modulo gets wrong.
        assertEquals(KvadrantTime(12, 0), tapping("PM", KvadrantTime(0, 0)))
    }

    @Test
    fun the_meridiem_column_keeps_its_order_whichever_half_is_chosen() {
        // Two values in a window of two wrapped, so AM and PM changed places depending on which was
        // selected. A column with no ends has nothing to join. Asserted by where they are on the
        // screen, because that is the complaint.
        assertEquals(true, morningIsAboveAfternoon(KvadrantTime(9, 0)))
        assertEquals(true, morningIsAboveAfternoon(KvadrantTime(21, 0)))
    }

    private fun tapping(
        cell: String,
        from: KvadrantTime,
    ): KvadrantTime {
        var value = from
        runComposeUiTest {
            setContent {
                KvadrantTheme {
                    KvadrantTimePicker(
                        value = value,
                        onValueChange = { value = it },
                        hourCycle = KvadrantHourCycle.Twelve,
                    )
                }
            }
            onNodeWithText(cell).performClick()
        }
        return value
    }

    private fun morningIsAboveAfternoon(time: KvadrantTime): Boolean {
        var above = false
        runComposeUiTest {
            setContent {
                KvadrantTheme {
                    KvadrantTimePicker(
                        value = time,
                        onValueChange = {},
                        hourCycle = KvadrantHourCycle.Twelve,
                    )
                }
            }
            val morning = onNodeWithText("AM").fetchSemanticsNode().positionInRoot.y
            val afternoon = onNodeWithText("PM").fetchSemanticsNode().positionInRoot.y
            above = morning < afternoon
        }
        return above
    }

    private fun labelsOf(order: KvadrantDateOrder): List<String> {
        val labels = mutableListOf<String>()
        runComposeUiTest {
            setContent {
                KvadrantTheme {
                    KvadrantDatePicker(
                        value = KvadrantDate(2026, 8, 29),
                        onValueChange = {},
                        modifier = Modifier.testTag(TAG),
                        order = order,
                        monthNames = MONTHS,
                        dayLabel = "день",
                        monthLabel = "месяц",
                        yearLabel = "год",
                    )
                }
            }
            labels +=
                onNodeWithTag(TAG, useUnmergedTree = true)
                    .fetchSemanticsNode()
                    .children
                    .mapNotNull { column ->
                        column.textInReadingOrder().firstOrNull { it in setOf("день", "месяц", "год") }
                    }
        }
        return labels
    }

    private fun androidx.compose.ui.semantics.SemanticsNode.textInReadingOrder(): List<String> {
        val own = config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        return own + children.sortedBy { it.positionInRoot.y }.flatMap { it.textInReadingOrder() }
    }

    private companion object {
        const val TAG = "picker"
        val MONTHS =
            listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
    }
}
