package io.github.elli0t0.phone

import android.content.ContentResolver
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
 * The failure is injected through ContactNameIndex's
 * registerContactsObserver seam: ContentResolver.registerContentObserver
 * is final and cannot be overridden by a fake resolver, and
 * Handler/ContentObserver construction itself throws on JVM stubs, so
 * the seam covers the whole registration step. ContactNameIndex is a
 * process-wide object, so each test resets its started flag (and ready
 * state) via reflection first and restores a clean state after, keeping
 * this file independent of test order.
 */
class ContactNameIndexRetryTest {

    // ContentResolver is abstract: a bare subclass with no overrides
    // instantiates it (its methods are stubs on the JVM test runtime).
    private class DummyResolver : ContentResolver(null)

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
        ContactNameIndex.registerContactsObserver = ContactNameIndex::defaultRegisterContactsObserver
        resetIndex()
    }

    @Test
    fun registrationRetriesAfterPermissionGrant() {
        var attempts = 0
        var throwOnRegister = true
        ContactNameIndex.registerContactsObserver = { _, _ ->
            attempts++
            if (throwOnRegister) throw SecurityException("no permission yet")
        }
        // Permission denied: registration attempted, flag stays unset.
        ContactNameIndex.ensureStarted(DummyResolver())
        assertEquals(1, attempts)
        // Permission granted: the next call retries and succeeds.
        throwOnRegister = false
        ContactNameIndex.ensureStarted(DummyResolver())
        assertEquals(2, attempts)
        // Started: further calls do not re-register.
        ContactNameIndex.ensureStarted(DummyResolver())
        assertEquals(2, attempts)
    }
}
