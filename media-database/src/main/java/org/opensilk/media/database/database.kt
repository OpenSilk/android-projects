package org.opensilk.media.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.opensilk.dagger2.ForApp
import javax.inject.Inject

const private val VERSION = 1

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
        if (oldVersion < VERSION) {
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
            db.execSQL("DROP TABLE IF EXISTS media_position")
            db.execSQL("CREATE TABLE media_position (" +
                    "_display_name TEXT NOT NULL PRIMARY KEY, " +
                    "last_played INTEGER NOT NULL, " + //milli
                    "last_position INTEGER NOT NULL, " +
                    "last_completion INTEGER NOT NULL " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS upnp_device")
            db.execSQL("CREATE TABLE upnp_device (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
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
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "device_id TEXT NOT NULL, " +
                    "folder_id TEXT NOT NULL, " +
                    "parent_id TEXT NOT NULL, " +
                    "_display_name TEXT NOT NULL, " +

                    "date_added INTEGER NOT NULL," +
                    "hidden INTEGER DEFAULT 0," +
                    "UNIQUE(device_id,folder_id,parent_id) " + //milli
                    ");")
            db.execSQL("DROP TABLE IF EXISTS upnp_video")
            db.execSQL("CREATE TABLE upnp_video (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "device_id TEXT NOT NULL, " +
                    "item_id TEXT NOT NULL, " +
                    "parent_id TEXT NOT NULL, " +
                    "_display_name TEXT NOT NULL, " +
                    "mime_type TEXT NOT NULL, " +
                    "media_uri TEXT NOT NULL, " +
                    "duration INTEGER DEFAULT -1, " + //milli
                    "bitrate INTEGER DEFAULT -1, " +
                    "file_size INTEGER DEFAULT -1, " +
                    "resolution TEXT, " +

                    "date_added INTEGER NOT NULL, " + //milli
                    "hidden INTEGER DEFAULT 0," +

                    "episode_id INTEGER, " +
                    "movie_id INTEGER, " +
                    "custom_artwork_uri TEXT, " +
                    "custom_backdrop_uri TEXT, " +

                    "UNIQUE(device_id,item_id,parent_id) " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS document")
            db.execSQL("CREATE TABLE document (" +
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

                    "date_added INTEGER NOT NULL, " +
                    "hidden INTEGER DEFAULT 0, " +

                    "episode_id INTEGER, " +
                    "movie_id INTEGER, " +
                    "custom_artwork_uri TEXT, " +
                    "custom_backdrop_uri TEXT, " +

                    "UNIQUE(tree_uri, document_id, parent_id)" +
                    ");")
        }
    }
}