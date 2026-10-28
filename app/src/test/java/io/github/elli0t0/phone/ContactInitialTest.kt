package io.github.elli0t0.phone

import io.github.elli0t0.phone.feature.contacts.ui.contactInitial
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactInitialTest {

    @Test
    fun lowerCaseInitialIsUppercased() {
        assertEquals("A", contactInitial("ali"))
    }

    @Test
    fun blankNameFallsBackToQuestionMark() {
        assertEquals("?", contactInitial(""))
        assertEquals("?", contactInitial("   "))
    }

    @Test
    fun surroundingWhitespaceIsTrimmed() {
        assertEquals("A", contactInitial("  ali  "))
    }

    @Test
    fun alreadyUppercaseStaysUnchanged() {
        assertEquals("A", contactInitial("Ali"))
    }
}
