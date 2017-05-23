package org.opensilk.upnp.cds.browser

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.CursorWindow
import android.database.CursorWrapper
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import org.fourthline.cling.model.meta.RemoteDevice
import org.opensilk.common.dagger.ForApplication
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by drew on 5/20/17.
 */
const val DATABASE_VERSION = 1
const val DATABASE_FILENAME = "cds_database.sqlite"

@Singleton
class CDSDatabase @Inject constructor(
        @ForApplication context: Context): SQLiteOpenHelper(context, DATABASE_FILENAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS remote_dev;")
        db.execSQL("CREATE TABLE remote_dev (" +
                "identity TEXT PRIMARY KEY , " +
                "_display_name TEXT NOT NULL, " +
                "description TEXT," +
                "network TEXT, " +
                "last_seen INTEGER " +
                ");")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        TODO("not implemented")
    }
}

class CDSDatabaseClient @Inject constructor(val db: CDSDatabase) {

    fun addRemoteDevice(device: RemoteDevice): Boolean {
        var identity = device.identity.udn.identifierString
        var displayName = device.details.friendlyName
        var details = device.displayString
        if (displayName.isNullOrBlank()) {
            displayName = device.displayString
            details = ""
        }
        val values = ContentValues(5)
        values.put("_display_name", displayName)
        values.put("description", details)
        values.put("last_seen", System.currentTimeMillis())
        db.writableDatabase.query("remote_dev", arrayOf("identity"), "identity = ?",
                arrayOf(identity), null, null, null)?.use {
            if (it.count > 0) {
                return db.writableDatabase.update("remote_dev", values, "identity = ?", arrayOf(identity)) == 1
            }
        }
        values.put("identity", identity)
        return db.writableDatabase.insert("remote_dev", null, values) != -1L
    }

    fun queryRemoteDevices(projection: Array<out String>?) : Cursor {
        return db.readableDatabase.query("remote_dev", projection, null, null, null, null, null)
    }

}

class MetaCursor(cursor: Cursor) : CursorWrapper(cursor) {
    override fun setExtras(extras: Bundle?) {
        super.setExtras(extras)
    }
}