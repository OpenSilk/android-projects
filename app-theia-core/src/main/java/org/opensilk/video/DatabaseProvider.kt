package org.opensilk.video

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import dagger.Module
import dagger.Subcomponent
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.ProviderScope
import org.opensilk.common.dagger.injectMe
import timber.log.Timber
import javax.inject.Inject

@ProviderScope
@Subcomponent
interface DatabaseProviderComponent: Injector<DatabaseProvider> {
    @Subcomponent.Builder
    abstract class Builder: Injector.Builder<DatabaseProvider>()
}

@Module(subcomponents = arrayOf(DatabaseProviderComponent::class))
abstract class DatabaseProviderModule

/**
 * Created by drew on 7/18/17.
 */
class DatabaseProvider: ContentProvider() {

    @Inject lateinit var mDatabase: Database
    @Inject lateinit var mUris: DatabaseUris

    override fun onCreate(): Boolean {
        injectMe()
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        var table: String = ""
        var id: Long = -1L
        var realSelection = selection ?: ""
        var realSelectionArgs = selectionArgs
        when (mUris.matcher.match(uri)) {
            DatabaseMatches.TV_SERIES_ONE -> {
                id = uri.lastPathSegment.toLong()
                table = "tv_series"
            }
            DatabaseMatches.TV_SERIES -> {
                table = "tv_series"
            }
            DatabaseMatches.TV_SERIES_SEARCH -> {
                table = "tv_series_search"
            }
            DatabaseMatches.TV_EPISODES_ONE -> {
                id = uri.lastPathSegment.toLong()
                table = "tv_episodes"
            }
            DatabaseMatches.TV_EPISODES -> {
                table = "tv_episodes"
            }
            DatabaseMatches.TV_BANNERS_ONE -> {
                id = uri.lastPathSegment.toLong()
                table = "tv_banners"
            }
            DatabaseMatches.TV_BANNERS -> {
                table = "tv_banners"
            }
            DatabaseMatches.TV_CONFIG -> {
                table = "tv_config"
            }
            DatabaseMatches.MOVIES_ONE -> {
                id = uri.lastPathSegment.toLong()
                table = "movies"
            }
            DatabaseMatches.MOVIES -> {
                table = "movies"
            }
            DatabaseMatches.MOVIE_IMAGES_ONE -> {
                id = uri.lastPathSegment.toLong()
                table = "movie_images"
            }
            DatabaseMatches.MOVIE_IMAGES -> {
                table = "movie_images"
            }
            DatabaseMatches.MOVIE_SEARCH -> {
                table = "movies_search"
            }
            DatabaseMatches.MOVIE_CONFIG -> {
                table = "movie_config"
            }
            DatabaseMatches.UPNP_DEVICES -> {
                table = "upnp_device"
            }
            DatabaseMatches.UPNP_FOLDERS -> {
                table = "upnp_folder"
            }
            DatabaseMatches.UPNP_VIDEOS -> {
                table = "upnp_video v " +
                        "LEFT JOIN tv_episodes e ON v.episode_id = e._id " +
                        "LEFT JOIN tv_series s ON e.series_id = s._id " +
                        "LEFT JOIN movies m ON v.movie_id = m._id "
            }
            DatabaseMatches.UPNP_VIDEOS_ONE -> {
                table = "upnp_video v " +
                        "LEFT JOIN tv_episodes e ON v.episode_id = e._id " +
                        "LEFT JOIN tv_series s ON e.series_id = s._id " +
                        "LEFT JOIN movies m ON v.movie_id = m._id "
                realSelection = "v._id=${uri.lastPathSegment}"
                realSelectionArgs = null
            }
            DatabaseMatches.PLAYBACK_POSITION -> {
                table = "media_position"
            }
            DatabaseMatches.DOCUMENTS -> {
                table = "document d " +
                        "LEFT JOIN tv_episodes e ON d.episode_id = e._id " +
                        "LEFT JOIN tv_series s ON e.series_id = s._id " +
                        "LEFT JOIN movies m ON d.movie_id = m._id "
            }
            else -> throw IllegalArgumentException("Unmatched uri: $uri")
        }
        if (id != -1L) {
            if (!realSelection.isNullOrBlank()) {
                Timber.w("Ignoring selection on single item uri: %s", uri)
            }
            realSelection = "_id=$id"
            realSelectionArgs = null
        }
        return mDatabase.readableDatabase.query(table, projection, realSelection,
                realSelectionArgs, null, null, sortOrder)
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues): Uri? {
        val db = mDatabase.writableDatabase
        when (mUris.matcher.match(uri)) {
            DatabaseMatches.TV_SERIES -> {
                val id = db.insertWithOnConflict("tv_series", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return mUris.tvSeries(id)
            }
            DatabaseMatches.TV_EPISODES -> {
                val id = db.insertWithOnConflict("tv_episodes", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return mUris.tvEpisode(id)
            }
            DatabaseMatches.TV_BANNERS -> {
                val id = db.insertWithOnConflict("tv_banners", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return mUris.tvBanner(id)
            }
            DatabaseMatches.TV_CONFIG -> {
                val id = db.insertWithOnConflict("tv_config", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return mUris.tvConfig()
            }
            DatabaseMatches.MOVIES -> {
                val id = db.insertWithOnConflict("movies", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return mUris.movie(id)
            }
            DatabaseMatches.MOVIE_IMAGES -> {
                val id = db.insertWithOnConflict("movie_images", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return mUris.movieImage(id)
            }
            DatabaseMatches.MOVIE_CONFIG -> {
                val id = db.insertWithOnConflict("movie_config", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return mUris.movieConfig()
            }
            DatabaseMatches.UPNP_DEVICES -> {
                var id: Long = try {
                    db.insertWithOnConflict("upnp_device", null, values, SQLiteDatabase.CONFLICT_FAIL)
                } catch (ignored: SQLiteException) { -1L }
                if (id > 0) {
                    return mUris.upnpDevice(id)
                }
                val device_id = values.getAsString("device_id")
                values.remove("device_id")
                id = db.query("upnp_device", arrayOf("_id"), "device_id=?", arrayOf(device_id), null,
                        null, null)?.use { if (it.moveToNext()) it.getLong(0) else -1L } ?: -1L
                return if (db.update("upnp_device", values, "device_id=?", arrayOf(device_id)) != 0) {
                    mUris.upnpDevice(id)
                } else {
                    null
                }
            }
            DatabaseMatches.UPNP_FOLDERS -> {
                val id = db.insertWithOnConflict("upnp_folder", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return mUris.upnpFolder(id)
            }
            DatabaseMatches.UPNP_VIDEOS -> {
                var id: Long = try {
                    db.insertWithOnConflict("upnp_video", null, values, SQLiteDatabase.CONFLICT_FAIL)
                } catch (ignored: SQLiteException) { -1L }
                if (id > 0) {
                    return mUris.upnpVideo(id)
                }
                //already in database, we don't want to replace so update entry
                val device_id = values.getAsString("device_id")
                val parent_id = values.getAsString("parent_id")
                val item_id = values.getAsString("item_id")
                values.remove("device_id")
                values.remove("parent_id")
                values.remove("item_id")
                values.remove("date_added")
                //fetch the id
                id = db.query("upnp_video", arrayOf("_id"), "device_id=? AND parent_id=? AND item_id=?",
                        arrayOf(device_id, parent_id, item_id), null, null, null)?.use {
                    return@use if (it.moveToNext()) it.getLong(0) else -1L } ?: -1L
                //update the entry
                if (db.update("upnp_video", values, "_id=?", arrayOf(id.toString())) != 0) {
                    return mUris.upnpVideo(id)
                } else {
                    return null
                }
            }
            DatabaseMatches.PLAYBACK_POSITION -> {
                db.insertWithOnConflict("media_position", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return Uri.EMPTY
            }
            DatabaseMatches.DOCUMENTS -> {
                var id: Long = try {
                    db.insertWithOnConflict("document", null, values, SQLiteDatabase.CONFLICT_FAIL)
                } catch (ignored: SQLiteException) { -1L }
                if (id > 0) {
                    return mUris.document(id)
                }
                //already in db
                val tree_uri = values.getAsString("tree_uri")
                val doc_id = values.getAsString("document_id")
                val parent_id = values.getAsString("parent_id")
                values.remove("tree_uri")
                values.remove("document_id")
                values.remove("parent_id")
                values.remove("date_added")
                //get id
                id = db.query("document", arrayOf("_id"), "tree_uri=? AND document_id=? AND parent_id=?",
                        arrayOf(tree_uri, doc_id, parent_id), null, null, null)?.use { c ->
                    return@use if (c.moveToFirst()) c.getLong(0) else -1
                } ?: -1
                //update existing
                if (db.update("document", values, "_id=?", arrayOf(id.toString())) != 0) {
                    return mUris.document(id)
                } else {
                    return null
                }
            }
            else -> throw IllegalArgumentException("Unmatched uri: $uri")
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val db = mDatabase.writableDatabase
        when (mUris.matcher.match(uri)) {
            DatabaseMatches.UPNP_FOLDERS_ONE -> {
                return db.delete("upnp_folder", "_id=?", arrayOf(uri.lastPathSegment))
            }
            DatabaseMatches.UPNP_VIDEOS_ONE -> {
                return db.delete("upnp_video", "_id=?", arrayOf(uri.lastPathSegment))
            }
            else -> throw IllegalArgumentException("Unmatched uri: $uri")
        }
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        val db = mDatabase.writableDatabase
        when (mUris.matcher.match(uri)) {
            DatabaseMatches.MOVIES -> {
                return db.update("movies", values, selection, selectionArgs)
            }
            DatabaseMatches.MOVIE_IMAGES -> {
                return db.update("movie_images", values, selection, selectionArgs)
            }
            DatabaseMatches.UPNP_DEVICES -> {
                return db.update("upnp_device", values, selection, selectionArgs)
            }
            DatabaseMatches.UPNP_DEVICES_SCAN_UP -> {
                //for whatever reason using content values to update scanning in this way
                //didnt work for me, hence the special uri
                try {
                    db.execSQL("UPDATE upnp_device SET scanning = scanning + 1 where $selection", selectionArgs)
                    return 1
                } catch (e: SQLiteException) {
                    return 0
                }
            }
            DatabaseMatches.UPNP_DEVICES_SCAN_DOWN -> {
                try {
                    db.execSQL("UPDATE upnp_device SET scanning = scanning - 1 where $selection", selectionArgs)
                    return 1
                } catch (e: SQLiteException) {
                    return 0
                }
            }
            DatabaseMatches.UPNP_FOLDERS -> {
                return db.update("upnp_folder", values, selection, selectionArgs)
            }
            DatabaseMatches.UPNP_VIDEOS -> {
                return db.update("upnp_video", values, selection, selectionArgs)
            }
            DatabaseMatches.DOCUMENTS -> {
                return db.update("document", values, selection, selectionArgs)
            }
            else -> throw IllegalArgumentException("Unmatched uri: $uri")
        }
    }
}