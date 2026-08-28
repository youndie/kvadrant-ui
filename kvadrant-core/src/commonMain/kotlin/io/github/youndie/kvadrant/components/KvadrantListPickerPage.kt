package io.github.youndie.kvadrant.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantTheme

/**
 * The page a picker with more than five options opens, rather than unfolding in place.
 *
 * **Full is the common case and Expanded is the exception**, which is the wrong way round from how
 * this library was built: Windows Phone's own Settings pages went to a page far more often than they
 * grew a list under a caption. `ListPicker.cs` makes the trip a real frame navigation —
 * `OpenPickerPage` walks `Application.Current.RootVisual as PhoneApplicationFrame` and calls
 * `Navigate(PickerPageUri)` — and on the way back it reads `SelectedItem` off the page and commits
 * it.
 *
 * **This library owns no navigator and this composable does not change that.** A component library
 * with a back stack is a component library that fights every application's. What is here is the
 * *content* of that page; a caller routes to it with whatever it already uses, and the phone's own
 * back button dismisses it because it is a real destination rather than an overlay. An overlay would
 * have needed no routing and would have swallowed back, which is the most reliable way to make an
 * application feel foreign on Android.
 *
 * **What is transcribed and what is not.** The navigation shape, the selection commit and the
 * threshold are `ListPicker.cs`. The page chrome is [KvadrantPage], which is. The **type size of a
 * row is this project's**: the toolkit's `ListPickerPage.xaml` is a separate file from the
 * `Generic.xaml` this repository has, so the one number that decides how the page looks was not
 * available to read — it ships as [itemStyle] rather than as a constant, and it is the first thing
 * to fix if that file ever turns up.
 *
 * `DatePicker` and `TimePicker` are the same shape and are also unbuilt; research §1.11 records them
 * as "real frame navigation with zeroed transitions, not a popup". This composable is the answer for
 * those too.
 */
@Composable
public fun KvadrantListPickerPage(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    header: String = "",
    applicationTitle: String = "",
    itemStyle: TextStyle = KvadrantTheme.typography.large,
    cyrillic: FontFamily? = null,
) {
    KvadrantPage(
        applicationTitle = applicationTitle,
        pageTitle = header,
        cyrillic = cyrillic,
        modifier = modifier,
    ) {
        // **No `verticalScroll` here.** [KvadrantPage] already scrolls its content, and a scrolling
        // column inside a scrolling column is measured with an infinite maximum height — which
        // Compose refuses outright, so the page did not merely lay out oddly, it crashed the
        // application the moment a row was tapped. A long list is the one thing this page is for,
        // which is how the mistake made itself so easy: the scroll looked obviously necessary and
        // was already there one level up.
        Column(Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, label ->
                KvadrantText(
                    label,
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(index) }
                        // `PhoneTouchTargetOverhang` again: a row of a long list is a target, and
                        // the phone gave every one of them the same twelve pixels of slack.
                        .padding(vertical = KvadrantTheme.metrics.touchTargetOverhang),
                    itemStyle.copy(
                        color =
                            if (index == selectedIndex) {
                                KvadrantTheme.colors.foreground
                            } else {
                                KvadrantTheme.colors.subtle
                            },
                    ),
                    cyrillic,
                )
            }
            // The page is a destination, so what closes it is the system's back and not a control
            // this library draws. Nothing is added here on purpose.
        }
    }
}
