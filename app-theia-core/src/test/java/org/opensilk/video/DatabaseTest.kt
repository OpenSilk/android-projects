package org.opensilk.video

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.provider.DocumentsContract
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by drew on 7/19/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class DatabaseTest {

    lateinit var mDatabase: SQLiteDatabase

    @Before
    fun setup() {
        mDatabase = Database(RuntimeEnvironment.application).writableDatabase
        insertTestData()
    }


    @Test
    fun test_upnp_video_joined_with_episodes_and_movies() {
        val bob = SQLiteQueryBuilder()
        bob.tables = "upnp_video v " +
                "LEFT JOIN tv_episodes e ON v.episode_id = e._id " +
                "LEFT JOIN tv_series s ON e.series_id = s._id " +
                "LEFT JOIN movies m ON v.movie_id = m._id "
        val projMap = HashMap<String, String>()
        projMap.put("_id", "v._id")
        projMap.put("_display_name", "v._display_name")
        projMap.put("movie_title", "m._display_name")
        projMap.put("episode_title", "e._display_name")
        bob.setProjectionMap(projMap)
        val c = bob.query(mDatabase, arrayOf("_id", "_display_name", "movie_title", "episode_title", "duration"),
                null, null, null, null, null)
        //val c = bob.query(mDatabase, arrayOf("v._id", "v._display_name",
        //       "m._display_name as movies_name","e._display_name as episode_name",
        //        "s._display_name as series_name"), null, null, "v._id" , null, null)
        System.out.println(Arrays.toString(c.columnNames))
        System.out.println("count = " + c.count)
        while (c.moveToNext()) {
            for (ii in 0..c.columnCount-1){
                System.out.println(c.columnNames[ii] + ": " + c.getString(ii) + ", ")
            }
            System.out.println()
        }
    }

    fun insertTestData() {
        val s = ContentValues()
        s.put("_id", 1000)
        s.put("_display_name", "Test Series")
        mDatabase.insert("tv_series", null, s)
        val e = ContentValues()
        e.put("_id", 2000)
        e.put("_display_name", "Test Episode")
        e.put("episode_number", 1)
        e.put("season_number", 1)
        e.put("series_id", 1000)
        mDatabase.insert("tv_episodes", null, e)
        val m = ContentValues()
        m.put("_id", 1000)
        m.put("_display_name", "Test Movie")
        m.put("image_base_url", "http://foo.com")
        mDatabase.insert("movies", null, m)
        val f = ContentValues()
        f.put("device_id", "dev1")
        f.put("folder_id", "fol1")
        f.put("parent_id", "0")
        f.put("mime_type", DocumentsContract.Document.MIME_TYPE_DIR)
        f.put("_display_name", "Folder 1")
        f.put("date_added", System.currentTimeMillis())
        mDatabase.insert("upnp_folder", null, f)
        var v = ContentValues()
        v.put("device_id", "dev1")
        v.put("item_id", "itm1")
        v.put("parent_id", "fol1")
        v.put("_display_name", "Video 1")
        v.put("mime_type", "video/mp4")
        v.put("media_uri", "https://foo.com/video1")
        v.put("date_added", System.currentTimeMillis())
        v.put("episode_id", 2000)
        mDatabase.insert("upnp_video", null, v)
        v = ContentValues()
        v.put("device_id", "dev1")
        v.put("item_id", "itm2")
        v.put("parent_id", "fol1")
        v.put("_display_name", "Video 2")
        v.put("mime_type", "video/mp4")
        v.put("media_uri", "https://foo.com/video2")
        v.put("date_added", System.currentTimeMillis())
        v.put("movie_id", 1000)
        mDatabase.insert("upnp_video", null, v)
        v = ContentValues()
        v.put("device_id", "dev1")
        v.put("item_id", "itm3")
        v.put("parent_id", "fol1")
        v.put("_display_name", "Video 3")
        v.put("mime_type", "video/mp4")
        v.put("media_uri", "https://foo.com/video3")
        v.put("date_added", System.currentTimeMillis())
        mDatabase.insert("upnp_video", null, v)
    }
}