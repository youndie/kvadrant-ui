package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantListPickerPage
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.KvadrantTypography
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The page lays out, and a row reports its own index.
 *
 * It did neither on a device: `KvadrantPage` scrolls its content already, and a scrolling column
 * inside a scrolling column is measured with an infinite maximum height, which Compose refuses
 * outright. So the page did not lay out oddly — it crashed the application the moment a row was
 * tapped. Nothing caught it because nothing composed this page at all; it was written, wired into
 * the sample and looked at only through the sample.
 */
@OptIn(ExperimentalTestApi::class)
class ListPickerPageTest {
    private val zones = listOf("калининград", "москва", "самара", "екатеринбург", "новосибирск", "владивосток")

    @Test
    fun a_row_reports_its_index_and_the_page_survives_being_drawn() {
        var chosen = -1
        runComposeUiTest {
            setContent {
                KvadrantTheme(KvadrantColors.dark(), KvadrantTypography.default(kvadrantLatin())) {
                    Box(Modifier.size(400.dp, 700.dp)) {
                        KvadrantListPickerPage(
                            items = zones,
                            selectedIndex = 1,
                            onSelect = { chosen = it },
                            header = "часовой пояс",
                            applicationTitle = "KVADRANT UI",
                        )
                    }
                }
            }
            // The first row, which is the one the report named.
            onNodeWithText(zones[0]).performClick()
            waitForIdle()
        }
        assertEquals(0, chosen, "tapping the first row did not report index 0")
    }
}
