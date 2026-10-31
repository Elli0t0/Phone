package io.github.elli0t0.phone.feature.contacts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.Contacts
import androidx.core.content.FileProvider
import io.github.elli0t0.phone.R
import java.io.File

/** The provider's multi-vCard path segment: lookup keys joined by ":", then encoded. */
fun multiVCardLookupKeysParam(
    lookupKeys: List<String>,
    encode: (String) -> String = Uri::encode
): String = encode(lookupKeys.joinToString(":"))

/** Long lists go through a cache file instead: provider URIs with very long key lists are unreliable. */
fun useVCardFileFallback(lookupKeyCount: Int): Boolean = lookupKeyCount > 50

fun multiVCardUri(lookupKeys: List<String>): Uri =
    Uri.withAppendedPath(Contacts.CONTENT_MULTI_VCARD_URI, multiVCardLookupKeysParam(lookupKeys))

/**
 * Shares contacts as provider vCards — the full stored details
 * (notes, birthday, address, photo), not a hand-built subset.
 * Direct provider stream for normal lists; for long lists the
 * stream is copied to cacheDir/shared/contacts.vcf and shared
 * through the FileProvider instead.
 */
fun shareContacts(context: Context, lookupKeys: List<String>) {
    val keys = lookupKeys.filter { it.isNotBlank() }.distinct()
    if (keys.isEmpty()) return
    val chooserTitle = context.getString(R.string.action_share)
    if (useVCardFileFallback(keys.size)) {
        val file = copyVCardStreamToCache(context, multiVCardUri(keys))
        if (file != null) {
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            shareVCardUri(context, fileUri, chooserTitle)
            return
        }
        // Cache copy failed: fall back to the direct stream rather
        // than failing the share outright.
    }
    shareVCardUri(context, multiVCardUri(keys), chooserTitle)
}

private fun shareVCardUri(context: Context, uri: Uri, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = Contacts.CONTENT_VCARD_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

private fun copyVCardStreamToCache(context: Context, uri: Uri): File? {
    return runCatching {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "contacts.vcf")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        file
    }.getOrNull()
}
