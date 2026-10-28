package io.github.elli0t0.phone.core.recording.adb

import android.content.Context
import com.flyfishxu.kadb.cert.KadbCert
import com.flyfishxu.kadb.cert.KadbPrivateKeyStore
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Kadb's identity defaults to an in-memory key: pairing authorizes a key
 * that vanishes when the process dies, so the next status check reports
 * Not paired even though pairing just succeeded. Persist the private key
 * in app-private storage so one pairing sticks across process restarts
 * (and app updates); only clearing app data or forgetting the pairing in
 * system Settings invalidates it.
 */
private val configured = AtomicBoolean(false)

fun ensureKadbIdentity(context: Context) {
    if (!configured.compareAndSet(false, true)) return
    val file = File(context.filesDir, "kadb/adbkey.pem")
    KadbCert.configure(store = FilePrivateKeyStore(file))
}

/** Minimal file store for Kadb's private key; no new dependency needed. */
private class FilePrivateKeyStore(private val file: File) : KadbPrivateKeyStore {
    override fun readPrivateKeyPem(): ByteArray? =
        if (file.exists()) runCatching { file.readBytes() }.getOrNull() else null

    override fun writePrivateKeyPemAtomic(privateKeyPem: ByteArray) {
        val parent = file.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()
        val tmp = File(parent, "${file.name}.tmp-${System.nanoTime().toString(16)}")
        tmp.writeBytes(privateKeyPem)
        if (!tmp.renameTo(file)) {
            tmp.delete()
            file.writeBytes(privateKeyPem)
        }
    }

    override fun clear() {
        if (file.exists()) file.delete()
    }
}
