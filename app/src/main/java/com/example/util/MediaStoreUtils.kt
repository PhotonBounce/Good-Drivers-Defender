package com.example.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

/**
 * Saves a file to the public Downloads directory using the MediaStore API.
 * Supports Android Q (API 29) and above via scoped storage. For older APIs it
 * falls back to direct file system access, requiring WRITE_EXTERNAL_STORAGE
 * permission.
 */
fun saveFileToPublicDownloads(
    context: Context,
    sourceFile: File,
    displayName: String,
    mimeType: String
): Boolean {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore to insert into the public Downloads collection.
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val itemUri: Uri? = resolver.insert(collection, values)
            if (itemUri == null) return false
            resolver.openOutputStream(itemUri).use { outStream ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(outStream!!)
                }
            }
            // Mark the item as not pending so it becomes visible.
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
            return true
        } else {
            // Legacy approach – write directly to the public Downloads folder.
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val destFile = File(downloadsDir, displayName)
            sourceFile.copyTo(destFile, overwrite = true)
            return true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}
