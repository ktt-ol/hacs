package io.mainframe.hacs.ssh

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.OpenableColumns
import io.mainframe.hacs.R
import org.pmw.tinylog.Logger
import java.io.File
import java.io.FileNotFoundException

data class PkCredentials(
    val privateKey: KeyData?,
    val password: String?
) {

    companion object {
        private fun getPrefs(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

        @JvmStatic
        fun fromSettings(context: Context): PkCredentials {
            val prefs = getPrefs(context)
            val privateKeyUri = prefs.getString(context.getString(R.string.PREFS_PRIVATE_KEY_FILENAME), null)
                ?.let { Uri.parse(it) }

            val privateKey = if (privateKeyUri != null) {
                KeyData.fromUri(privateKeyUri, context)
            } else {
                null
            }
            val password = prefs.getString(context.getString(R.string.PREFS_PRIVATE_KEY_PASSWORD), null)
            return PkCredentials(privateKey, password)
        }

        @JvmStatic
        fun isPasswordSet(context: Context): Boolean {
            val prefs = getPrefs(context)
            val password = prefs.getString(context.getString(R.string.PREFS_PRIVATE_KEY_PASSWORD), null)
            return password != null && password.isNotEmpty()
        }
    }
}

data class KeyData(val name: String, val data: ByteArray) {
    companion object {
        @JvmStatic
        fun fromUri(uri: Uri, context: Context): KeyData? {
            val name = getDisplayName(uri, context) ?: File(uri.lastPathSegment!!).name
            return try {
                val stream = context.contentResolver.openInputStream(uri) ?: return null
                KeyData(name, stream.use { it.readBytes() })
            } catch (e: FileNotFoundException) {
                Logger.error(e.message, e)
                null
            }
        }

        // https://developer.android.com/training/data-storage/shared/documents-files#examine-metadata
        private fun getDisplayName(uri: Uri, context: Context): String? {
            // The query, because it only applies to a single document, returns only
            // one row. There's no need to filter, sort, or select fields,
            // because we want all fields for one document.
            val cursor: Cursor? = context.contentResolver.query(
                uri, null, null, null, null, null
            )

            cursor?.use {
                // moveToFirst() returns false if the cursor has 0 rows. Very handy for
                // "if there's anything to look at, look at it" conditionals.
                if (it.moveToFirst()) {

                    // Note it's called "Display Name". This is
                    // provider-specific, and might not necessarily be the file name.
                    val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex == -1) {
                        return null;
                    }
                    return it.getString(columnIndex)
                }
            }

            return null
        }
    }
}