package io.github.elli0t0.phone.core.recording

import android.content.ContentResolver
import android.net.Uri
import java.io.File

/**
 * Deletes one recording file, whether it lives in MediaStore
 * (content://) or in private storage (file://). Never throws: a
 * missing file is a no-op for the caller, which still drops the row.
 */
fun deleteFile(resolver: ContentResolver, uri: Uri) = runCatching {
    if (uri.scheme == "file") File(uri.path!!).delete() else resolver.delete(uri, null, null)
}
