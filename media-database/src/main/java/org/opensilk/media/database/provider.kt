package org.opensilk.media.database

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import dagger.android.AndroidInjection
import javax.inject.Inject

/**
 * Created by drew on 7/18/17.
 */
class MediaProvider : ContentProvider() {

    @Inject internal lateinit var mMediaDB: MediaDB
    @Inject internal lateinit var mUris: MediaDBUris

    override fun onCreate(): Boolean {
        AndroidInjection.inject(this)
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val table: String
        when (mUris.matcher.match(uri)) {
            M.TV_SERIES -> {
                table = "tv_series"
            }
            M.TV_EPISODE -> {
                table = "tv_episodes"
            }
            M.TV_IMAGE -> {
                table = "tv_banners"
            }
            M.MOVIE -> {
                table = "movies"
            }
            M.MOVIE_IMAGE -> {
                table = "movie_images"
            }
            M.UPNP_AUDIO -> {
                table = "upnp_audio"
            }
            M.UPNP_DEVICE -> {
                table = "upnp_device"
            }
            M.UPNP_FOLDER -> {
                table = "upnp_folder"
            }
            M.UPNP_MUSIC_TRACK -> {
                TODO()
            }
            M.UPNP_VIDEO -> {
                table = "upnp_video v " +
                        "LEFT JOIN tv_episodes e ON v.episode_id = e._id " +
                        "LEFT JOIN tv_series s ON e.series_id = s._id " +
                        "LEFT JOIN movies m ON v.movie_id = m._id "
            }
            M.PLAYBACK_POSITION -> {
                table = "media_position"
            }
            M.DOCUMENT_DIRECTORY -> {
                table = "document_directory"
            }
            M.DOCUMENT_AUDIO -> {
                TODO()
            }
            M.DOCUMENT_VIDEO -> {
                table = "document_video d " +
                        "LEFT JOIN tv_episodes e ON d.episode_id = e._id " +
                        "LEFT JOIN tv_series s ON e.series_id = s._id " +
                        "LEFT JOIN movies m ON d.movie_id = m._id "
            }
            else -> TODO("Unmatched uri: $uri")
        }
        return mMediaDB.readableDatabase.query(table, projection, selection,
                selectionArgs, null, null, sortOrder)
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues): Uri? {
        val db = mMediaDB.writableDatabase
        when (mUris.matcher.match(uri)) {
            M.TV_SERIES -> {
                val id = db.insertWithOnConflict("tv_series", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return if (id > 0) URI_SUCCESS else URI_FAILURE
            }
            M.TV_EPISODE -> {
                val id = db.insertWithOnConflict("tv_episodes", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return if (id > 0) URI_SUCCESS else URI_FAILURE
            }
            M.TV_IMAGE -> {
                val id = db.insertWithOnConflict("tv_banners", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return if (id > 0) URI_SUCCESS else URI_FAILURE
            }
            M.MOVIE -> {
                val id = db.insertWithOnConflict("movies", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return if (id > 0) URI_SUCCESS else URI_FAILURE
            }
            M.MOVIE_IMAGE -> {
                val id = db.insertWithOnConflict("movie_images", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return if (id > 0) URI_SUCCESS else URI_FAILURE
            }
            M.UPNP_AUDIO -> {
                val id: Long = try {
                    db.insertWithOnConflict("upnp_audio", null, values, SQLiteDatabase.CONFLICT_FAIL)
                } catch (ignored: SQLiteException) { -1L }
                if (id > 0) {
                    return URI_SUCCESS
                }
                //already in database, we don't want to replace so update entry
                val device_id = values.getAsString("device_id")
                val parent_id = values.getAsString("parent_id")
                val item_id = values.getAsString("item_id")
                values.remove("device_id")
                values.remove("parent_id")
                values.remove("item_id")
                values.remove("date_added")
                //update the entry
                return if (db.update("upnp_audio", values,
                        "device_id=? AND parent_id=? AND item_id=?",
                        arrayOf(device_id, parent_id, item_id)) != 0)
                    URI_SUCCESS else URI_FAILURE
            }
            M.UPNP_DEVICE -> {
                val id: Long = try {
                    db.insertWithOnConflict("upnp_device", null, values, SQLiteDatabase.CONFLICT_FAIL)
                } catch (ignored: SQLiteException) { -1L }
                if (id > 0) {
                    return URI_SUCCESS
                }
                val device_id = values.getAsString("device_id")
                values.remove("device_id")
                return if (db.update("upnp_device", values, "device_id=?", arrayOf(device_id)) != 0)
                    URI_SUCCESS else URI_FAILURE
            }
            M.UPNP_FOLDER -> {
                val id = db.insertWithOnConflict("upnp_folder", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return if (id > 0) URI_SUCCESS else URI_FAILURE
            }
            M.UPNP_MUSIC_TRACK -> {
                val id: Long = try {
                    db.insertWithOnConflict("upnp_music_track", null, values, SQLiteDatabase.CONFLICT_FAIL)
                } catch (ignored: SQLiteException) { -1L }
                if (id > 0) {
                    return URI_SUCCESS
                }
                //already in database, we don't want to replace so update entry
                val device_id = values.getAsString("device_id")
                val parent_id = values.getAsString("parent_id")
                val item_id = values.getAsString("item_id")
                values.remove("device_id")
                values.remove("parent_id")
                values.remove("item_id")
                values.remove("date_added")
                //update the entry
                return if (db.update("upnp_music_track", values,
                        "device_id=? AND parent_id=? AND item_id=?",
                        arrayOf(device_id, parent_id, item_id)) != 0)
                    URI_SUCCESS else URI_FAILURE
            }
            M.UPNP_VIDEO -> {
                val id: Long = try {
                    db.insertWithOnConflict("upnp_video", null, values, SQLiteDatabase.CONFLICT_FAIL)
                } catch (ignored: SQLiteException) { -1L }
                if (id > 0) {
                    return URI_SUCCESS
                }
                //already in database, we don't want to replace so update entry
                val device_id = values.getAsString("device_id")
                val parent_id = values.getAsString("parent_id")
                val item_id = values.getAsString("item_id")
                values.remove("device_id")
                values.remove("parent_id")
                values.remove("item_id")
                values.remove("date_added")
                //update the entry
                return if (db.update("upnp_video", values,
                        "device_id=? AND parent_id=? AND item_id=?",
                        arrayOf(device_id, parent_id, item_id)) != 0)
                    URI_SUCCESS else URI_FAILURE
            }
            M.PLAYBACK_POSITION -> {
                val id = db.insertWithOnConflict("media_position", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return if (id > 0) URI_SUCCESS else URI_FAILURE
            }
            M.DOCUMENT_DIRECTORY -> {
                val id = db.insertWithOnConflict("document_directory", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return if (id > 0) URI_SUCCESS else URI_FAILURE
            }
            M.DOCUMENT_AUDIO -> {
                val id: Long = try {
                    db.insertWithOnConflict("document_audio", null, values, SQLiteDatabase.CONFLICT_FAIL)
                } catch (ignored: SQLiteException) { -1L }
                if (id > 0) {
                    return URI_SUCCESS
                }
                //already in db
                val tree_uri = values.getAsString("tree_uri")
                val doc_id = values.getAsString("document_id")
                val parent_id = values.getAsString("parent_id")
                values.remove("tree_uri")
                values.remove("document_id")
                values.remove("parent_id")
                values.remove("date_added")
                //update existing
                return if (db.update("document_audio", values,
                        "tree_uri=? AND document_id=? AND parent_id=?",
                        arrayOf(tree_uri, doc_id, parent_id)) != 0)
                    URI_SUCCESS else URI_FAILURE
            }
            M.DOCUMENT_VIDEO -> {
                val id: Long = try {
                    db.insertWithOnConflict("document_video", null, values, SQLiteDatabase.CONFLICT_FAIL)
                } catch (ignored: SQLiteException) { -1L }
                if (id > 0) {
                    return URI_SUCCESS
                }
                //already in db
                val tree_uri = values.getAsString("tree_uri")
                val doc_id = values.getAsString("document_id")
                val parent_id = values.getAsString("parent_id")
                values.remove("tree_uri")
                values.remove("document_id")
                values.remove("parent_id")
                values.remove("date_added")
                //update existing
                return if (db.update("document_video", values,
                        "tree_uri=? AND document_id=? AND parent_id=?",
                        arrayOf(tree_uri, doc_id, parent_id)) != 0)
                    URI_SUCCESS else URI_FAILURE
            }
            else -> TODO("Unmatched uri: $uri")
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        TODO()
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        val db = mMediaDB.writableDatabase
        when (mUris.matcher.match(uri)) {
            M.MOVIE -> {
                return db.update("movies", values, selection, selectionArgs)
            }
            M.MOVIE_IMAGE -> {
                return db.update("movie_images", values, selection, selectionArgs)
            }
            M.UPNP_DEVICE -> {
                return db.update("upnp_device", values, selection, selectionArgs)
            }
            M.UPNP_AUDIO -> {
                return db.update("upnp_audio", values, selection, selectionArgs)
            }
            M.UPNP_FOLDER -> {
                return db.update("upnp_folder", values, selection, selectionArgs)
            }
            M.UPNP_MUSIC_TRACK -> {
                return db.update("upnp_music_track", values, selection, selectionArgs)
            }
            M.UPNP_VIDEO -> {
                return db.update("upnp_video", values, selection, selectionArgs)
            }
            M.DOCUMENT_DIRECTORY -> {
                return db.update("document_directory", values, selection, selectionArgs)
            }
            M.DOCUMENT_AUDIO -> {
                return db.update("document_audio", values, selection, selectionArgs)
            }
            M.DOCUMENT_VIDEO -> {
                return db.update("document_video", values, selection, selectionArgs)
            }
            else -> TODO("Unmatched uri: $uri")
        }
    }
}