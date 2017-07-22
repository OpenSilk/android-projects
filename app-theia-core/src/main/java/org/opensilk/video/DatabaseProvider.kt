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
                val sb = SQLiteQueryBuilder()
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
            else -> throw IllegalArgumentException("Unmatched uri: $uri")
        }
        if (id != -1L) {
            if (realSelection.isBlank()) {
                realSelection = "_id=" + id
            } else {
                realSelection += " AND _id=" + id
            }
        }
        return mDatabase.readableDatabase.query(table, projection, realSelection, selectionArgs, null, null, sortOrder)
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
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