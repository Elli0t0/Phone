package io.github.elli0t0.phone

import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.data.contacts.util.ContactGroupingUtil
import io.github.elli0t0.phone.data.contacts.util.ContactSearchMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactUtilsTest {

    @Test
    fun contactSearchMatcher_matchesByNameCaseInsensitive() {
        val contact = ContactSummary(id = 1L, lookupKey = "k1", displayName = "Alice Smith", primaryNumberPreview = "12345")
        assertTrue(ContactSearchMatcher.matches(contact, "alice"))
        assertTrue(ContactSearchMatcher.matches(contact, "ALICE"))
        assertTrue(ContactSearchMatcher.matches(contact, "Smith"))
        assertFalse(ContactSearchMatcher.matches(contact, "Bob"))
    }

    @Test
    fun contactSearchMatcher_matchesByDigitsInsensitivePhone() {
        val contact = ContactSummary(id = 1L, lookupKey = "k1", displayName = "Office", primaryNumberPreview = "+98 (912) 345-6789")
        assertTrue(ContactSearchMatcher.matches(contact, "0912")) // Persian/local prefix substring or digits check
        assertTrue(ContactSearchMatcher.matches(contact, "912345"))
        assertTrue(ContactSearchMatcher.matches(contact, "3456789"))
        assertFalse(ContactSearchMatcher.matches(contact, "999999"))
    }

    @Test
    fun contactGroupingUtil_placesNonLettersUnderHash() {
        assertEquals("#", ContactGroupingUtil.getSectionHeader("123"))
        assertEquals("#", ContactGroupingUtil.getSectionHeader("+12345"))
        assertEquals("#", ContactGroupingUtil.getSectionHeader(""))
        assertEquals("#", ContactGroupingUtil.getSectionHeader("!@#"))
        assertEquals("A", ContactGroupingUtil.getSectionHeader("Alice"))
        assertEquals("B", ContactGroupingUtil.getSectionHeader("Bob"))
    }

    @Test
    fun contactGroupingUtil_handlesPersianAndArabicLetters() {
        val header = ContactGroupingUtil.getSectionHeader("علی")
        assertEquals("ع", header)
    }

    @Test
    fun contactGroupingUtil_sortsHashAtEnd() {
        val contacts = listOf(
            ContactSummary(id = 1L, lookupKey = "k1", displayName = "123 Support", primaryNumberPreview = "123"),
            ContactSummary(id = 2L, lookupKey = "k2", displayName = "Alice", primaryNumberPreview = "456"),
            ContactSummary(id = 3L, lookupKey = "k3", displayName = "Bob", primaryNumberPreview = "789")
        )
        val grouped = ContactGroupingUtil.groupContacts(contacts)
        val keys = grouped.keys.toList()
        assertEquals(listOf("A", "B", "#"), keys)
    }

    @Test
    fun contactDetail_customRingtoneAndPhotoHandling() {
        val detail = io.github.elli0t0.phone.data.contacts.model.ContactDetail(
            id = 10L,
            lookupKey = "k10",
            displayName = "Charlie",
            photoUri = "content://com.android.contacts/display_photo/1",
            customRingtone = "content://media/internal/audio/media/42"
        )
        assertEquals("content://com.android.contacts/display_photo/1", detail.photoUri)
        assertEquals("content://media/internal/audio/media/42", detail.customRingtone)
    }
}

