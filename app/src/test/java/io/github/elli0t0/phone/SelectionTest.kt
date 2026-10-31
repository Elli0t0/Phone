package io.github.elli0t0.phone

import io.github.elli0t0.phone.core.common.Selection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionTest {

    @Test
    fun selection_toggle_addsAndRemoves() {
        val selection = Selection().toggle(1L).toggle(2L)
        assertTrue(selection.isActive)
        assertEquals(setOf(1L, 2L), selection.ids)

        val removed = selection.toggle(1L)
        assertEquals(setOf(2L), removed.ids)
        assertTrue(removed.isActive)
    }

    @Test
    fun selection_toggleLastId_autoExits() {
        val selection = Selection().toggle(7L).toggle(7L)
        assertFalse(selection.isActive)
        assertTrue(selection.ids.isEmpty())
    }

    @Test
    fun selection_selectAll_replacesIds() {
        val selection = Selection().toggle(1L).selectAll(listOf(2L, 3L))
        assertEquals(setOf(2L, 3L), selection.ids)
    }

    @Test
    fun selection_clear_empties() {
        val selection = Selection().toggle(1L).clear()
        assertFalse(selection.isActive)
    }
}
