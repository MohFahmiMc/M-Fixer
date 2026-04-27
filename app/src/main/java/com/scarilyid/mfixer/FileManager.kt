package com.scarilyid.mfixer

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.InputStream
import java.io.OutputStream

object FileManager {

    // Fungsi untuk menyalin file
    fun copyFile(context: Context, sourceUri: Uri, destDir: DocumentFile): String? {
        val fileName = getFileName(context, sourceUri) ?: "new_file"
        val newFile = destDir.createFile("*/*", fileName) ?: return null

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                input.copyTo(output)
            }
        }
        return fileName
    }

    // Fungsi untuk ambil nama file
    fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = cursor.getString(index)
                }
            }
        }
        return result ?: uri.path?.substringAfterLast('/')
    }
}
