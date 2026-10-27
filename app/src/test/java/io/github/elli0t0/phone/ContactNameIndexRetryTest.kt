package io.github.elli0t0.phone

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import io.github.elli0t0.phone.data.contacts.ContactNameIndex
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * ContactNameIndex must retry registration after permission is granted:
 * if ensureStarted() ran before READ_CONTACTS was granted, the old code
 * set started=true despite the SecurityException, so later calls
 * returned early and the observer was never registered.
 *
 * ContactNameIndex is a process-wide object, so each test resets its
 * started flag (and ready state) via reflection first and restores a
 * clean state after, keeping this file independent of test order.
 */
class ContactNameIndexRetryTest {

    private class FlakyResolver : ContentResolver(null) {
        var registerAttempts = 0
        var throwOnRegister = true

        override fun registerContentObserver(
            uri: Uri,
            notifyForDescendants: Boolean,
            observer: ContentObserver
        ) {
            registerAttempts++
            if (throwOnRegister) throw SecurityException("no permission yet")
        }
        // query() is final in ContentResolver, so it is not overridden:
        // the stub returns null, which build() already handles.
    }

    private fun setField(name: String, value: Any?) {
        val field = ContactNameIndex.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(ContactNameIndex, value)
    }

    private fun resetIndex() {
        setField("started", false)
        setField("contentResolver", null)
        setField("index", emptyMap<String, ContactNameIndex.Entry>())
        setField("building", false)
        val readyField = ContactNameIndex.javaClass.getDeclaredField("_ready")
        readyField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val ready = readyField.get(ContactNameIndex) as kotlinx.coroutines.flow.MutableStateFlow<Boolean>
        ready.value = false
        val failedField = ContactNameIndex.javaClass.getDeclaredField("_buildFailed")
        failedField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val failed = failedField.get(ContactNameIndex) as kotlinx.coroutines.flow.MutableStateFlow<Boolean>
        failed.value = false
    }

    @Before
    fun setUp() {
        resetIndex()
    }

    @After
    fun tearDown() {
        resetIndex()
    }

    @Test
    fun registrationRetriesAfterPermissionGrant() {
        val resolver = FlakyResolver()
        // Permission denied: registration attempted, flag stays unset.
        ContactNameIndex.ensureStarted(resolver)
        assertEquals(1, resolver.registerAttempts)
        // Permission granted: the next call retries and succeeds.
        resolver.throwOnRegister = false
        ContactNameIndex.ensureStarted(resolver)
        assertEquals(2, resolver.registerAttempts)
        // Started: further calls do not re-register.
        ContactNameIndex.ensureStarted(resolver)
        assertEquals(2, resolver.registerAttempts)
    }
}
