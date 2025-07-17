package com.miniai.plugin

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

abstract class MiniAIProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    // This allows the host app to query information about the AI
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val cursor = MatrixCursor(arrayOf("key", "value"))

        // Return basic info about the AI
        cursor.addRow(arrayOf("name", "Mini"))
        cursor.addRow(arrayOf("version", "1.0"))
        cursor.addRow(arrayOf("status", "ready"))

        return cursor
    }

    // Other required methods (not used)
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    abstract override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int
}

annotation class int
