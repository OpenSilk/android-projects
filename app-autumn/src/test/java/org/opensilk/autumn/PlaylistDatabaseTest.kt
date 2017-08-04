package org.opensilk.autumn

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Created by drew on 8/4/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class PlaylistDatabaseTest {

    lateinit var mDb: PlaylistDatabase
    lateinit var mPlaylistDao: PlaylistDao

    @Before
    fun before() {
        mDb = AppModule.provideDatabase(RuntimeEnvironment.application)
        mPlaylistDao = mDb.playlistDoa()
    }

    @After
    fun after() {
        mDb.close()
    }


    @Test
    fun add_playlist_then_asset() {
        val playlist = Playlist("foo")
        val asset = PlaylistAsset("bar", "foo", "http://foo.com", "day", "place", "video", 0)
        mPlaylistDao.addPlaylist(playlist)
        mPlaylistDao.addPlaylistAsset(asset)
        val retrievedPlaylist = mPlaylistDao.getAll()[0]
        val retrievedAsset = mPlaylistDao.getAssets("foo")[0]
        assertThat(retrievedPlaylist).isEqualTo(playlist)
        assertThat(retrievedAsset).isEqualTo(asset)
    }
}