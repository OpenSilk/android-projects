package org.opensilk.media.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.opensilk.dagger2.ForApp
import javax.inject.Inject

const private val VERSION = 7

/**
 * Created by drew on 7/18/17.
 */
internal class MediaDB
@Inject constructor(
        @ForApp context: Context
) : SQLiteOpenHelper(context, "media.sqlite", null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        onUpgrade(db, 0, VERSION)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 1) {
            db.execSQL("DROP TABLE IF EXISTS tv_series;")
            db.execSQL("CREATE TABLE tv_series (" +
                    "_id INTEGER PRIMARY KEY , " +
                    "_display_name TEXT NOT NULL, " +
                    "overview TEXT," +
                    "first_aired TEXT, " +
                    "poster TEXT, " +
                    "backdrop TEXT " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS tv_episodes;")
            db.execSQL("CREATE TABLE tv_episodes (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "_display_name TEXT, " +
                    "first_aired TEXT, " +
                    "overview TEXT, " +
                    "episode_number INTEGER NOT NULL, " +
                    "season_number INTEGER NOT NULL, " +
                    "series_id INTEGER NOT NULL," +
                    "poster TEXT," +
                    "backdrop TEXT " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS tv_banners;")
            db.execSQL("CREATE TABLE tv_banners (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "path TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "type2 TEXT NOT NULL, " +
                    "resolution TEXT NOT NULL, " +
                    "rating FLOAT, " +
                    "rating_count INTEGER, " +
                    "series_id INTEGER NOT NULL " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS movies;")
            db.execSQL("CREATE TABLE movies (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "_display_name TEXT NOT NULL, " +
                    "overview TEXT, " +
                    "release_date TEXT, " +
                    "poster_path TEXT, " +
                    "backdrop_path TEXT " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS movie_images;")
            db.execSQL("CREATE TABLE movie_images (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "file_path TEXT NOT NULL, " +
                    "image_type TEXT NOT NULL, " + //poster|backdrop
                    "resolution TEXT NOT NULL, " +
                    "vote_average FLOAT, " +
                    "vote_count INTEGER, " +
                    "movie_id INTEGER NOT NULL " +
                    ");")

            db.execSQL("DROP TABLE IF EXISTS upnp_device")
            db.execSQL("CREATE TABLE upnp_device (" +
                    "device_id TEXT NOT NULL UNIQUE, " +
                    "title TEXT NOT NULL, " +
                    "subtitle TEXT, " +
                    "artwork_uri TEXT, " +
                    "available INTEGER DEFAULT 0," +
                    "update_id INTEGER DEFAULT 0, " +
                    "scanning INTEGER DEFAULT 0 " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS upnp_folder")
            db.execSQL("CREATE TABLE upnp_folder (" +
                    "device_id TEXT NOT NULL, " +
                    "folder_id TEXT NOT NULL, " +
                    "parent_id TEXT NOT NULL, " +
                    "_display_name TEXT NOT NULL, " +

                    "hidden INTEGER DEFAULT 0," +
                    "UNIQUE(device_id,folder_id,parent_id) " + //milli
                    ");")
            db.execSQL("DROP TABLE IF EXISTS upnp_video")
            db.execSQL("CREATE TABLE upnp_video (" +
                    "device_id TEXT NOT NULL, " +
                    "item_id TEXT NOT NULL, " +
                    "parent_id TEXT NOT NULL, " +
                    "_display_name TEXT NOT NULL, " +
                    "mime_type TEXT NOT NULL, " +
                    "media_uri TEXT NOT NULL, " +
                    "duration INTEGER DEFAULT 0, " + //milli
                    "bitrate INTEGER DEFAULT 0, " +
                    "file_size INTEGER DEFAULT 0, " +
                    "resolution TEXT, " +

                    "date_added INTEGER NOT NULL, " + //milli
                    "hidden INTEGER DEFAULT 0," +

                    "episode_id INTEGER, " +
                    "movie_id INTEGER, " +
                    "custom_artwork_uri TEXT, " +
                    "custom_backdrop_uri TEXT, " +

                    "UNIQUE(device_id,item_id,parent_id) " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS media_position")
            db.execSQL("CREATE TABLE media_position (" +
                    "_display_name TEXT NOT NULL PRIMARY KEY, " +
                    "last_played INTEGER NOT NULL, " + //milli
                    "last_position INTEGER NOT NULL, " +
                    "last_completion INTEGER NOT NULL " +
                    ");")
        }
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS document_directory")
            db.execSQL("CREATE TABLE document_directory (" +
                    "authority TEXT NOT NULL, " +
                    "tree_uri TEXT NOT NULL, " +
                    "document_id TEXT NOT NULL, " +
                    "parent_id TEXT NOT NULL, " +
                    "_display_name TEXT NOT NULL, " +
                    "flags INTEGER DEFAULT 0, " +
                    "last_modified INTEGER DEFAULT 0," +
                    "mime_type TEXT NOT NULL, " +

                    "hidden INTEGER DEFAULT 0," +
                    "UNIQUE(tree_uri, document_id, parent_id)" +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS document_video")
            db.execSQL("CREATE TABLE document_video (" +
                    "authority TEXT NOT NULL," +
                    "tree_uri TEXT NOT NULL," +
                    "document_id TEXT NOT NULL," +
                    "parent_id TEXT NOT NULL," +
                    "_display_name TEXT NOT NULL," +
                    "mime_type TEXT," +
                    "last_modified INTEGER DEFAULT 0," +
                    "flags INTEGER DEFAULT 0," +
                    "_size INTEGER DEFAULT 0," +
                    "summary TEXT," +

                    "date_added INTEGER NOT NULL, " +
                    "hidden INTEGER DEFAULT 0, " +

                    "episode_id INTEGER, " +
                    "movie_id INTEGER, " +
                    "custom_artwork_uri TEXT, " +
                    "custom_backdrop_uri TEXT, " +

                    "UNIQUE(tree_uri, document_id, parent_id)" +
                    ");")
        }
        if (oldVersion < 3) {
            db.execSQL("DROP TABLE IF EXISTS storage_device")
            db.execSQL("CREATE TABLE storage_device (" +
                    "uuid TEXT NOT NULL UNIQUE, " +
                    "path TEXT NOT NULL, " +
                    "is_primary INTEGER NOT NULL DEFAULT 0, " +
                    "_display_name TEXT NOT NULL, " +

                    "hidden INTEGER DEFAULT 0" +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS storage_directory")
            db.execSQL("CREATE TABLE storage_directory (" +
                    "path TEXT NOT NULL, " +
                    "parent_path TEXT NOT NULL, " +
                    "device_uuid TEXT NOT NULL, " +
                    "_display_name TEXT NOT NULL, " +

                    "hidden INTEGER DEFAULT 0," +
                    "UNIQUE(path, device_uuid)" +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS storage_video")
            db.execSQL("CREATE TABLE storage_video (" +
                    "path TEXT NOT NULL," +
                    "parent_path TEXT NOT NULL, " +
                    "device_uuid TEXT NOT NULL," +
                    "_display_name TEXT NOT NULL," +
                    "mime_type TEXT," +
                    "last_modified INTEGER DEFAULT 0," +
                    "_size INTEGER DEFAULT 0," +

                    "date_added INTEGER NOT NULL, " +
                    "hidden INTEGER DEFAULT 0, " +

                    "episode_id INTEGER, " +
                    "movie_id INTEGER, " +
                    "custom_artwork_uri TEXT, " +
                    "custom_backdrop_uri TEXT, " +

                    "UNIQUE(path, device_uuid)" +
                    ");")
        }
        if (oldVersion < 5) {
            db.execSQL("DROP TABLE IF EXISTS pinned")
            db.execSQL("CREATE TABLE pinned (" +
                    "media_id TEXT UNIQUE NOT NULL, " +
                    "pinned INTEGER DEFAULT 1 " +
                    ");")
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE upnp_video ADD COLUMN last_played INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE document_video ADD COLUMN last_played INTEGER DEFAULT 0" )
            db.execSQL("ALTER TABLE storage_video ADD COLUMN last_played INTEGER DEFAULT 0")
        }
        if (oldVersion < 7) {
            db.execSQL("DROP TABLE IF EXISTS document_music_track")
            db.execSQL("CREATE TABLE document_music_track (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "authority TEXT NOT NULL," +
                    "tree_uri TEXT NOT NULL," +
                    "document_id TEXT NOT NULL," +
                    "parent_id TEXT NOT NULL," +

                    "_display_name TEXT NOT NULL," +
                    "mime_type TEXT," +
                    "last_modified INTEGER DEFAULT 0," +
                    "flags INTEGER DEFAULT 0," +
                    "_size INTEGER DEFAULT 0," +
                    "summary TEXT," +

                    "date_added INTEGER NOT NULL, " + //milli
                    "hidden INTEGER DEFAULT 0," +

                    "album_id INTEGER, " +
                    "artist_id INTEGER, " +
                    "custom_artwork_uri TEXT, " +
                    "custom_backdrop_uri TEXT, " +

                    "UNIQUE(tree_uri, document_id, parent_id) " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS storage_music_track")
            db.execSQL("CREATE TABLE storage_music_track (" +
                    "path TEXT NOT NULL," +
                    "parent_path TEXT NOT NULL, " +
                    "device_uuid TEXT NOT NULL," +

                    "_display_name TEXT NOT NULL, " +
                    "mime_type TEXT," +
                    "last_modified INTEGER DEFAULT 0," +
                    "_size INTEGER DEFAULT 0," +

                    "date_added INTEGER NOT NULL, " + //milli
                    "hidden INTEGER DEFAULT 0," +

                    "album_id INTEGER, " +
                    "artist_id INTEGER, " +
                    "custom_artwork_uri TEXT, " +
                    "custom_backdrop_uri TEXT, " +

                    "UNIQUE(path, device_uuid) " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS upnp_music_track")
            db.execSQL("CREATE TABLE upnp_music_track (" +
                    "device_id TEXT NOT NULL, " +
                    "item_id TEXT NOT NULL, " +
                    "parent_id TEXT NOT NULL, " +
                    "_display_name TEXT NOT NULL, " +
                    "genre TEXT, " +
                    "artist TEXT, " +
                    "album TEXT, " +
                    "date TEXT, " +
                    "track_num INTEGER DEFAULT 0, " +
                    "mime_type TEXT NOT NULL, " +
                    "media_uri TEXT NOT NULL, " +
                    "duration INTEGER DEFAULT 0, " + //milli
                    "bitrate INTEGER DEFAULT 0, " +
                    "file_size INTEGER DEFAULT 0, " +
                    "n_channels INTEGER DEFAULT 0, " +
                    "s_freq INTEGER DEFAULT 0, " +
                    "artwork_uri TEXT, " +

                    "date_added INTEGER NOT NULL, " + //milli
                    "hidden INTEGER DEFAULT 0," +

                    "album_id INTEGER, " +
                    "artist_id INTEGER, " +
                    "custom_artwork_uri TEXT, " +
                    "custom_backdrop_uri TEXT, " +

                    "UNIQUE(device_id,item_id,parent_id) " +
                    ");")
        }
    }
}