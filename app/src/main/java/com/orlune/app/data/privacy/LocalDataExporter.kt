package com.orlune.app.data.privacy

import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.sqlite.db.SupportSQLiteDatabase
import com.orlune.app.data.local.OrluneDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Creates a user-initiated JSON export from the local Room database. */
object LocalDataExporter {

    suspend fun share(context: Context, database: OrluneDatabase) {
        val file = withContext(Dispatchers.IO) { createExportFile(context, database) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export Orlune data"))
    }

    private fun createExportFile(context: Context, database: OrluneDatabase): File {
        val export = JSONObject()
            .put("product", "Orlune")
            .put("exportedAtEpochMillis", System.currentTimeMillis())
            .put("tables", exportTables(database.openHelper.readableDatabase))
        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        return File(directory, "orlune-export-${System.currentTimeMillis()}.json").also { file ->
            file.writeText(export.toString(2), Charsets.UTF_8)
        }
    }

    private fun exportTables(database: SupportSQLiteDatabase): JSONObject {
        val tables = JSONObject()
        database.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'android_%' AND name != 'room_master_table' ORDER BY name"
        ).use { tableCursor ->
            val nameIndex = tableCursor.getColumnIndexOrThrow("name")
            while (tableCursor.moveToNext()) {
                val tableName = tableCursor.getString(nameIndex)
                tables.put(tableName, exportRows(database, tableName))
            }
        }
        return tables
    }

    private fun exportRows(database: SupportSQLiteDatabase, tableName: String): JSONArray {
        val rows = JSONArray()
        database.query("SELECT * FROM `${tableName.replace("`", "``")}`").use { cursor ->
            while (cursor.moveToNext()) {
                val row = JSONObject()
                cursor.columnNames.forEachIndexed { index, columnName ->
                    row.put(columnName, cursorValue(cursor, index))
                }
                rows.put(row)
            }
        }
        return rows
    }

    private fun cursorValue(cursor: android.database.Cursor, index: Int): Any? = when {
        cursor.isNull(index) -> JSONObject.NULL
        cursor.getType(index) == android.database.Cursor.FIELD_TYPE_BLOB ->
            Base64.encodeToString(cursor.getBlob(index), Base64.NO_WRAP)
        cursor.getType(index) == android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
        cursor.getType(index) == android.database.Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index)
        else -> cursor.getString(index)
    }
}
