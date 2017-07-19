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
                    "series_id INTEGER NOT NULL " +
                    ");")
            db.execSQL("DROP VIEW IF EXISTS tv_episode_series_map")
            db.execSQL("CREATE VIEW tv_episode_series_map AS " +
                    "SELECT " +
                    "e._id, " +
                    "e._display_name as episode_name, " +
                    "e.first_aired, " +
                    "e.overview, " +
                    "e.episode_number, " +
                    "e.season_number, " +
                    "e.series_id, " +
                    "s._display_name as series_name, " +
                    "s.first_aired as series_first_aired, " +
                    "s.poster_path, " +
                    "s.backdrop_path " +
                    "FROM tv_episodes e " +
                    "JOIN tv_series s ON e.series_id = s._id " +
                    "GROUP BY e._id " +
                    ";")
            db.execSQL("DROP VIEW IF EXISTS tv_episode_banner_map;")
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
            db.execSQL("DROP TABLE IF EXISTS media;")
            db.execSQL("CREATE TABLE media (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "media_uri TEXT NOT NULL UNIQUE, " +
                    "_display_name TEXT NOT NULL, " +
                    "_title TEXT, " +
                    "_subtitle TEXT, " +
                    "parent_media_uri TEXT NOT NULL, " +
                    "server_id TEXT, " +
                    "artwork_uri TEXT, " +
                    "custom_artwork_uri INTEGER DEFAULT 0, " +
                    "backdrop_uri TEXT, " +
                    "custom_backdrop_uri INTEGER DEFAULT 0, " +
                    "media_category INTEGER DEFAULT 0, " + //type from mediametaextras

                    "series_id INTEGER, " +
                    "episode_id INTEGER, " +
                    "movie_id INTEGER, " +
                    "is_indexed INTEGER DEFAULT 0, " +
                    "date_added INTEGER NOT NULL, " + //milli

                    "last_played INTEGER, " + //milli

                    "last_position INTEGER DEFAULT -1, " + //milli

                    "duration INTEGER, " + //milli

                    "file_size INTEGER DEFAULT -1, " +
                    "audio_track INTEGER DEFAULT -1, " +
                    "audio_delay INTEGER DEFAULT 0, " +
                    "spu_track INTEGER DEFAULT -1, " +
                    "spu_delay INTEGER DEFAULT -1," +
                    "spu_path TEXT " +
                    ");")
            db.execSQL("DROP VIEW IF EXISTS media_episode_series_map")
            db.execSQL("DROP VIEW IF EXISTS media_description")
            db.execSQL("DROP VIEW IF EXISTS media_episode_map")
            db.execSQL("DROP VIEW IF EXISTS media_movie_map")
            db.execSQL("DROP TRIGGER IF EXISTS tv_series_cleanup;")
            db.execSQL("CREATE TRIGGER tv_series_cleanup AFTER DELETE ON media " +
                    "FOR EACH ROW " +
                    "WHEN (SELECT COUNT(_id) FROM media WHERE series_id=OLD.series_id) = 0 " +
                    "BEGIN " +
                    "DELETE FROM tv_series WHERE _id=OLD.series_id; " +
                    "DELETE FROM tv_episodes WHERE series_id=OLD.series_id; " +
                    "DELETE FROM tv_banners WHERE series_id=OLD.series_id; " +
                    "DELETE FROM tv_actors WHERE series_id=OLD.series_id; " +
                    "DELETE FROM tv_lookups WHERE series_id=OLD.series_id; " +
                    "DELETE FROM tv_series_search WHERE rowid=OLD.series_id; " +
                    "END")
            db.execSQL("DROP TRIGGER IF EXISTS movies_cleanup;")
            db.execSQL("CREATE TRIGGER movies_cleanup AFTER DELETE ON media " +
                    "FOR EACH ROW " +
                    "WHEN (SELECT COUNT(_id) FROM media WHERE movie_id=OLD.movie_id) = 0 " +
                    "BEGIN " +
                    "DELETE FROM movies WHERE _id=OLD.movie_id; " +
                    "DELETE FROM movie_images WHERE movie_id=OLD.movie_id; " +
                    "DELETE FROM movie_lookups WHERE movie_id=OLD.movie_id; " +
                    "DELETE FROM movies_search WHERE rowid=OLD.movie_id; " +
                    "END")
        }
    }
}