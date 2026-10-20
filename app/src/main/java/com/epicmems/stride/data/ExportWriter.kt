package com.epicmems.stride.data

import android.content.Context
import android.net.Uri

/** Writes exported text to a user-chosen document (SAF). */
object ExportWriter {

    fun write(context: Context, uri: Uri, content: String) {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }
}
