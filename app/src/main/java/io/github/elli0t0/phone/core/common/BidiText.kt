package io.github.elli0t0.phone.core.common

import android.text.BidiFormatter
import androidx.compose.ui.text.style.TextAlign

/**
 * Unicode-wrap a display name or number for showing next to text in
 * the opposite direction. Without the isolate marks, the bidi
 * algorithm reorders RTL names that contain parentheses or sit next
 * to Latin digits (e.g. a Persian name followed by a phone number).
 */
fun bidiWrap(text: String): String = BidiFormatter.getInstance().unicodeWrap(text)

/**
 * Alignment for a name: End (right in an LTR layout) when the first
 * strong character is RTL (Persian/Arabic/Hebrew), Start otherwise.
 * TextDirection.Content alone sets the paragraph direction, but the
 * Text block itself still sits at the row's start edge unless its
 * alignment follows the name's direction too.
 */
fun textAlignFor(text: String): TextAlign {
    for (char in text) {
        when (Character.getDirectionality(char)) {
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE -> return TextAlign.End
            Character.DIRECTIONALITY_LEFT_TO_RIGHT,
            Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING,
            Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE -> return TextAlign.Start
            else -> Unit
        }
    }
    return TextAlign.Start
}
