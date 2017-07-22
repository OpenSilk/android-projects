package org.opensilk.video

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.opensilk.common.dagger.ForApplication
import org.opensilk.common.dagger.ProviderScope
import javax.inject.Inject

const private val VERSION = 15

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

            db.execSQL("DROP TABLE IF EXISTS tv_series;")
            db.execSQL("CREATE TABLE tv_series (" +
                    "_id INTEGER PRIMARY KEY , " +
                    "_display_name TEXT NOT NULL, " +
                    "overview TEXT," +
                    "first_aired TEXT, " +
                    "poster_path TEXT, " +
                    "backdrop_path TEXT " +
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
                    "season_id INTEGER, " +
                    "series_id INTEGER NOT NULL " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS tv_banners;")
            db.execSQL("CREATE TABLE tv_banners (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "path TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "type2 TEXT NOT NULL, " +
                    "rating FLOAT, " +
                    "rating_count INTEGER, " +
                    "thumb_path TEXT, " +
                    "season INTEGER, " +
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
            db.execSQL("DROP TABLE IF EXISTS movies;")
            db.execSQL("CREATE TABLE movies (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "_display_name TEXT NOT NULL, " +
                    "overview TEXT, " +
                    "release_date TEXT, " +
                    "poster_path TEXT, " +
                    "backdrop_path TEXT," +
                    "image_base_url TEXT NOT NULL " +
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
                    "movie_id INTEGER NOT NULL, " +
                    "image_base_url TEXT NOT NULL, " +
                    "image_type TEXT NOT NULL, " + //poster|backdrop

                    "height INTEGER NOT NULL, " +
                    "width INTEGER NOT NULL, " +
                    "file_path TEXT NOT NULL UNIQUE, " +
                    "vote_average FLOAT, " +
                    "vote_count INTEGER " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS movie_lookups;")
            db.execSQL("CREATE TABLE movie_lookups (" +
                    "q TEXT PRIMARY KEY, " +
                    "movie_id INTEGER NOT NULL " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS media_position")
            db.execSQL("CREATE TABLE media_position (" +
                    "_display_name TEXT NOT NULL PRIMARY KEY, " +
                    "last_played INTEGER, " + //milli
                    "last_position INTEGER DEFAULT -1 " +
                    ");")
            db.execSQL("DROP TABLE IF EXISTS upnp_device")
            db.execSQL("CREATE TABLE upnp_device (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "device_id TEXT NOT NULL UNIQUE, " +
                    "mime_type TEXT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "subtitle TEXT, " +
                    "artwork_uri TEXT, " +
                    "available INTEGER DEFAULT 0 " +
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

                    "date_added INTEGER NOT NULL," +
                    "UNIQUE(device_id,folder_id) " + //milli
                    ");")
            db.execSQL("DROP TABLE IF EXISTS upnp_video")
            db.execSQL("CREATE TABLE upnp_video (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "device_id TEXT NOT NULL, " +
                    "item_id TEXT NOT NULL, " +
                    "parent_id TEXT NOT NULL, " +
                    "_display_name TEXT NOT NULL, " +
                    "artwork_uri TEXT, " +
                    "custom_artwork_uri TEXT, " +
                    "backdrop_uri TEXT, " +
                    "custom_backdrop_uri TEXT, " +
                    "mime_type TEXT NOT NULL, " +
                    "media_uri TEXT NOT NULL, " +
                    "duration INTEGER DEFAULT -1, " + //milli
                    "bitrate INTEGER DEFAULT -1, " +
                    "file_size INTEGER DEFAULT -1, " +
                    "creator TEXT, " +

                    "series_id INTEGER, " +
                    "episode_id INTEGER, " +
                    "movie_id INTEGER, " +

                    "date_added INTEGER NOT NULL, " + //milli

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
            db.execSQL("DROP TRIGGER IF EXISTS tv_series_cleanup;")
            db.execSQL("CREATE TRIGGER tv_series_cleanup AFTER DELETE ON upnp_video " +
                    "FOR EACH ROW " +
                    "WHEN (SELECT COUNT(_id) FROM upnp_video WHERE series_id=OLD.series_id) = 0 " +
                    "BEGIN " +
                    "DELETE FROM tv_series WHERE _id=OLD.series_id; " +
                    "DELETE FROM tv_episodes WHERE series_id=OLD.series_id; " +
                    "DELETE FROM tv_banners WHERE series_id=OLD.series_id; " +
                    "DELETE FROM tv_actors WHERE series_id=OLD.series_id; " +
                    "DELETE FROM tv_lookups WHERE series_id=OLD.series_id; " +
                    "DELETE FROM tv_series_search WHERE rowid=OLD.series_id; " +
                    "END")
            db.execSQL("DROP TRIGGER IF EXISTS movies_cleanup;")
            db.execSQL("CREATE TRIGGER movies_cleanup AFTER DELETE ON upnp_video " +
                    "FOR EACH ROW " +
                    "WHEN (SELECT COUNT(_id) FROM upnp_video WHERE movie_id=OLD.movie_id) = 0 " +
                    "BEGIN " +
                    "DELETE FROM movies WHERE _id=OLD.movie_id; " +
                    "DELETE FROM movie_images WHERE movie_id=OLD.movie_id; " +
                    "DELETE FROM movie_lookups WHERE movie_id=OLD.movie_id; " +
                    "DELETE FROM movies_search WHERE rowid=OLD.movie_id; " +
                    "END")
        }
    }
}