package io.github.youndie.kvadrant.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Which Material graph actually *draws*, which is the only question that decides
 * [B-14](../../../../../../../../docs/backlog/B-14-material-adapter.md)'s dependency line.
 *
 * Research §1.2 is explicit that resolving and compiling settle nothing here: the failure it is
 * guarding against — `AbstractMethodError: OutlinedTextFieldDefaults$$Lambda does not define
 * CustomStyle.applyStyle` — happened on a graph that resolved cleanly and compiled cleanly and died
 * on the first screen with a text field. So the subject is an `OutlinedTextField`, and the check is
 * that pixels come out.
 */
@OptIn(ExperimentalTestApi::class)
class GraphRendersTest {
    @Test
    fun an_outlined_text_field_draws() =
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    Box(Modifier.size(400.dp).testTag("frame")) {
                        OutlinedTextField(
                            value = "готово",
                            onValueChange = {},
                            label = { Text("имя") },
                        )
                    }
                }
            }
            val image = onNodeWithTag("frame").captureToImage()
            val pixels = IntArray(image.width * image.height)
            image.readPixels(pixels)
            val lit = pixels.count { (it shr 16 and 0xFF) > 0x10 }
            assertTrue(lit > 500, "the field drew $lit lit pixels — it did not render")
        }
}
