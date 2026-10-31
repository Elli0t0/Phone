package io.github.elli0t0.phone

import io.github.elli0t0.phone.feature.contacts.multiVCardLookupKeysParam
import io.github.elli0t0.phone.feature.contacts.useVCardFileFallback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactShareTest {

    @Test
    fun vCardParam_joinsKeysWithColon() {
        // Uri.encode is an Android stub under JVM tests, so the
        // encoder arrives as a parameter; production passes Uri::encode.
        val param = multiVCardLookupKeysParam(listOf("key1", "key2")) { it }
        assertEquals("key1:key2", param)
    }

    @Test
    fun vCardParam_singleKey_noSeparator() {
        assertEquals("only", multiVCardLookupKeysParam(listOf("only")) { it })
    }

    @Test
    fun vCardFallback_smallList_directStream() {
        assertFalse(useVCardFileFallback(1))
        assertFalse(useVCardFileFallback(50))
    }

    @Test
    fun vCardFallback_longList_cacheFile() {
        assertTrue(useVCardFileFallback(51))
        assertTrue(useVCardFileFallback(80))
    }
}
