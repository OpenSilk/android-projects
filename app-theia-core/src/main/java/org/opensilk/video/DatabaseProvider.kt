package org.opensilk.video

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.provider.ContactsContract
import dagger.Binds
import dagger.Module
import dagger.Subcomponent
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.ProviderScope
import org.opensilk.common.dagger.injectMe
import org.opensilk.media.playback.MediaProviderClient
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
            DatabaseMatches.TV_SERIES_ONE_EPISODES -> {
                table = "tv_episodes"
                realSelection = "series_id=" + uri.pathSegments[uri.pathSegments.size - 1]
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
            DatabaseMatches.TV_ACTORS_ONE -> {
                id = uri.lastPathSegment.toLong()
                table = "tv_actors"
            }
            DatabaseMatches.TV_ACTORS -> {
                table = "tv_actors"
            }
            DatabaseMatches.TV_LOOKUPS -> {
                table = "tv_lookups"
            }
            DatabaseMatches.TV_EPISODE_DESC_ONE -> {
                id = uri.lastPathSegment.toLong()
                table = "tv_episode_series_map"
            }
            DatabaseMatches.TV_EPISODE_DESC -> {
                table = "tv_episode_series_map"
            }
            DatabaseMatches.MEDIA_ONE -> {
                id = uri.lastPathSegment.toLong()
                table = "media"
            }
            DatabaseMatches.MEDIA -> {
                table = "media"
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
            DatabaseMatches.MOVIE_LOOKUPS -> {
                table = "movie_lookups"
            }
            DatabaseMatches.MOVIE_SEARCH -> {
                table = "movies_search"
            }
            DatabaseMatches.UPNP_DEVICES -> {
                table = "upnp_device"
            }
            DatabaseMatches.UPNP_FOLDERS -> {
                //TODO join on upnp_device and only return folders were available=1
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
            DatabaseMatches.TV_ACTORS -> {
                val id = db.insertWithOnConflict("tv_actors", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return mUris.tvActor(id)
            }
            DatabaseMatches.TV_LOOKUPS -> {
                val id = db.insertWithOnConflict("tv_lookups", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return mUris.tvLookups()
            }
            DatabaseMatches.MEDIA -> {
                val id = db.insertWithOnConflict("media", null, values, SQLiteDatabase.CONFLICT_FAIL)
                return mUris.media(id)
            }
            DatabaseMatches.MOVIES -> {
                val id = db.insertWithOnConflict("movies", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return mUris.movie(id)
            }
            DatabaseMatches.MOVIE_IMAGES -> {
                val id = db.insertWithOnConflict("movie_images", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return mUris.movieImage(id)
            }
            DatabaseMatches.MOVIE_LOOKUPS -> {
                val id = db.insertWithOnConflict("movie_lookups", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return mUris.movieLookups()
            }
            DatabaseMatches.UPNP_DEVICES -> {
                val id = db.insertWithOnConflict("upnp_device", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return mUris.upnpDevice(id)
            }
            DatabaseMatches.UPNP_FOLDERS -> {
                val id = db.insertWithOnConflict("upnp_folder", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                return mUris.upnpFolder(id)
            }
            DatabaseMatches.UPNP_VIDEOS -> {
                var id = db.insert("upnp_video", null, values)
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
                    return@use if (it.moveToNext()) it.getLong(0) else -1 } ?: -1
                //update the entry
                if (db.update("upnp_video", values, "_id=?", arrayOf(id.toString())) != 0) {
                    return mUris.upnpVideo(id)
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
            DatabaseMatches.MEDIA -> {
                return db.delete("media", selection, selectionArgs)
            }
            DatabaseMatches.MEDIA_ONE -> {
                if (selection != null) {
                    Timber.w("Ignoring selection %s for delete of %s", selectionArgs, uri)
                }
                return db.delete("media", "_id=" + uri.lastPathSegment, null)
            }
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
            DatabaseMatches.MEDIA -> {
                return db.updateWithOnConflict("media", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE)
            }
            DatabaseMatches.MEDIA_ONE -> {
                val id = uri.lastPathSegment.toLong()
                var realSel = selection
                if (realSel.isNullOrEmpty()) {
                    realSel = "_id=" + id
                } else {
                    realSel = selection + " AND _id=" + id
                }
                return db.updateWithOnConflict("media", values, realSel, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE)
            }
            DatabaseMatches.MOVIES -> {
                return db.update("movies", values, selection, selectionArgs)
            }
            DatabaseMatches.MOVIE_IMAGES -> {
                return db.update("movie_images", values, selection, selectionArgs)
            }
            DatabaseMatches.UPNP_DEVICES -> {
                return db.update("upnp_device", values, selection, selectionArgs)
            }
            else -> throw IllegalArgumentException("Unmatched uri: $uri")
        }
    }
}