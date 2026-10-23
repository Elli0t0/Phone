package io.github.elli0t0.phone.core.common

import android.text.BidiFormatter

/**
 * Unicode-wrap a display name or number for showing next to text in
 * the opposite direction. Without the isolate marks, the bidi
 * algorithm reorders RTL names that contain parentheses or sit next
 * to Latin digits (e.g. a Persian name followed by a phone number).
 */
fun bidiWrap(text: String): String = BidiFormatter.getInstance().unicodeWrap(text)
