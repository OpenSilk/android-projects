package org.opensilk.video

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.opensilk.common.dagger.ForApplication
import org.opensilk.common.dagger.ProviderScope
import javax.inject.Inject

const private val VERSION = 22

/**
 * Created by drew on 7/18/17.
 */
@ProviderScope
class Database
@Inject constructor(
        @ForApplication context: Context
) : SQLiteOpenHelper(context, "videos.sqlite", null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        onUpgrade(db, 0, VERSION)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < VERSION) {
            db.execSQL("DROP VIEW IF EXISTS tv_episode_banner_map;")
            db.execSQL("DROP VIEW IF EXISTS media_episode_series_map")
            db.execSQL("DROP VIEW IF EXISTS media_description")
            db.execSQL("DROP VIEW IF EXISTS media_episode_map")
            db.execSQL("DROP VIEW IF EXISTS media_movie_map")
            db.execSQL("DROP TABLE IF EXISTS media;")
            db.execSQL("DROP TRIGGER IF EXISTS tv_series_cleanup;")
            db.execSQL("DROP TRIGGER IF EXISTS movies_cleanup;")

            db.execSQL("DROP TABLE IF EXISTS tv_config;")
            db.execSQL("CREATE TABLE tv_config (" +
                    "key TEXT NOT NULL UNIQUE, " +
                    "value TEXT " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS tv_series;")
            db.execSQL("CREATE TABLE tv_series (" +
                    "_id INTEGER PRIMARY KEY , " +
                    "_display_name TEXT NOT NULL, " +
                    "overview TEXT," +
                    "first_aired TEXT, " +
                    "banner TEXT, " +
                    "poster TEXT, " +
                    "backdrop TEXT " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS tv_series_search")
            db.execSQL("CREATE VIRTUAL TABLE tv_series_search USING fts3 (" +
                    "title" +
                    ");")
            db.execSQL("DROP TRIGGER IF EXISTS tv_series_search_maint;")
            db.execSQL("CREATE TRIGGER tv_series_search_maint AFTER INSERT ON tv_series " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "INSERT INTO tv_series_search (rowid,title) VALUES (NEW._id, NEW._display_name); " +
                    "END")
            db.execSQL("DROP TABLE IF EXISTS tv_episodes;")
            db.execSQL("CREATE TABLE tv_episodes (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "_display_name TEXT, " +
                    "first_aired TEXT, " +
                    "overview TEXT, " +
                    "episode_number INTEGER NOT NULL, " +
                    "season_number INTEGER NOT NULL, " +
                    "series_id INTEGER NOT NULL " +
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
                    "thumb_path TEXT, " +
                    "series_id INTEGER NOT NULL " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS tv_actors;")
            db.execSQL("CREATE TABLE tv_actors (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "_display_name TEXT NOT NULL, " +
                    "role TEXT NOT NULL, " +
                    "sort_order INTEGER NOT NULL, " +
                    "image_path TEXT, " +
                    "series_id INTEGER NOT NULL " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS tv_lookups;")
            db.execSQL("CREATE TABLE tv_lookups (" +
                    "q TEXT PRIMARY KEY, " +
                    "series_id INTEGER NOT NULL " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS movie_config")
            db.execSQL("CREATE TABLE movie_config (" +
                    "key TEXT NOT NULL UNIQUE, " +
                    "value TEXT " +
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
            db.execSQL("DROP TABLE IF EXISTS movies_search")
            db.execSQL("CREATE VIRTUAL TABLE movies_search USING fts3 (" +
                    "title" +
                    ");")
            db.execSQL("DROP TRIGGER IF EXISTS movies_search_maint;")
            db.execSQL("CREATE TRIGGER movies_search_maint AFTER INSERT ON movies " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "INSERT INTO movies_search (rowid,title) VALUES (NEW._id, NEW._display_name); " +
                    "END")
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
            db.execSQL("DROP TABLE IF EXISTS movie_lookups;")
            db.execSQL("CREATE TABLE movie_lookups (" +
                    "q TEXT PRIMARY KEY, " +
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
                    "mime_type TEXT NOT NULL, " +
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
                    "artwork_uri TEXT, " +
                    "mime_type TEXT NOT NULL, " +
                    "update_id INTEGER DEFAULT 0, " +

                    "date_added INTEGER NOT NULL," +
                    "hidden INTEGER DEFAULT 0," +
                    "UNIQUE(device_id,folder_id) " + //milli
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

                    "series_id INTEGER, " +
                    "episode_id INTEGER, " +
                    "movie_id INTEGER, " +
                    "custom_artwork_uri TEXT, " +
                    "custom_backdrop_uri TEXT, " +

                    "audio_track INTEGER DEFAULT -1, " +
                    "audio_delay INTEGER DEFAULT 0, " +
                    "spu_track INTEGER DEFAULT -1, " +
                    "spu_delay INTEGER DEFAULT -1," +
                    "spu_path TEXT, " +
                    "UNIQUE(device_id,item_id,parent_id) " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS upnp_video_search")
            db.execSQL("CREATE VIRTUAL TABLE upnp_video_search USING fts3 (" +
                    "title" +
                    ");")
            db.execSQL("DROP TRIGGER IF EXISTS upnp_video_search_maint;")
            db.execSQL("CREATE TRIGGER upnp_video_search_maint AFTER INSERT ON upnp_video " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "INSERT INTO upnp_video_search (rowid,title) VALUES (NEW._id, NEW._display_name); " +
                    "END")
            db.execSQL("DROP TRIGGER IF EXISTS upnp_video_search_cleanup")
            db.execSQL("CREATE TRIGGER upnp_video_search_cleanup AFTER DELETE ON upnp_video " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "DELETE FROM upnp_video_search WHERE rowid=OLD._id; " +
                    "END")
        }
    }
}