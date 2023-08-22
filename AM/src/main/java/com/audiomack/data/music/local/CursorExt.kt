package com.audiomack.data.music.local

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import com.audiomack.utils.takeIfZeroOrPositive

/**
 * Returns the [Long] value for the given column name or null if there is no value
 */
internal fun Cursor.getLong(columnName: String): Long? =
    getColumnIndex(columnName).takeIfZeroOrPositive()?.let { getLong(it) }

/**
 * Returns the [Int] value for the given column name or null if there is no value
 */
internal fun Cursor.getInt(columnName: String): Int? =
    getColumnIndex(columnName).takeIfZeroOrPositive()?.let { getInt(it) }

/**
 * Returns the [String] value for the given column name or null if there is no value
 */
internal fun Cursor.getString(columnName: String): String? =
    getColumnIndex(columnName).takeIfZeroOrPositive()?.let { getString(it) }

/**
 * Returns the [Long] value for the given column name, falling back to [default]
 */
internal fun Cursor.getLong(columnName: String, default: Long): Long =
    getColumnIndex(columnName).takeIfZeroOrPositive()?.let { getLong(it) } ?: default

/**
 * Returns the [Int] value for the given column name, falling back to [default]
 */
internal fun Cursor.getInt(columnName: String, default: Int): Int =
    getColumnIndex(columnName).takeIfZeroOrPositive()?.let { getInt(it) } ?: default

/**
 * Returns the [String] value for the given column name, falling back to [default]
 */
internal fun Cursor.getString(columnName: String, default: String): String =
    getColumnIndex(columnName).takeIfZeroOrPositive()?.let { getString(it) } ?: default

internal fun ContentResolver.query(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null
): Cursor? = query(uri, projection, selection, selectionArgs, sortOrder)

internal fun Cursor.getContentId(): Long? = getLong(BaseColumns._ID)
