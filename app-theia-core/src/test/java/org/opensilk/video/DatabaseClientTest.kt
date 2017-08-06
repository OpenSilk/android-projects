package org.opensilk.video

import android.content.ContentResolver
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.CancellationSignal
import android.test.mock.MockContentResolver
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opensilk.media.*
import org.opensilk.tmdb.api.model.Movie
import org.opensilk.tmdb.api.model.TMDbConfig
import org.opensilk.tvdb.api.model.Series
import org.opensilk.tvdb.api.model.SeriesEpisode
import org.opensilk.tvdb.api.model.Token
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

/**
 * Created by drew on 7/19/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, application = TestApp::class)
class DatabaseClientTest {

    lateinit var mClient: DatabaseClient
    lateinit var mProvider: DatabaseProvider
    lateinit var mResolver: ContentResolver

    @Before
    fun setupProvider() {
        mProvider = DatabaseProvider()

        val providerInfo = ProviderInfo()
        providerInfo.authority = "foo.authority"
        mProvider = Robolectric.buildContentProvider(DatabaseProvider::class.java).create(providerInfo).get()
        mProvider.mDatabase = Database(RuntimeEnvironment.application)
        mProvider.mUris = DatabaseUris("foo.authority")

        mResolver = RuntimeEnvironment.application.contentResolver

        mClient = DatabaseClient(DatabaseUris("foo.authority"), TVDB_BANNER_ROOT, mResolver)
    }

    @After
    fun teardown() {
        Robolectric.reset()
        mProvider.mDatabase.close()
    }

    fun add_upnp_video_shows_in_recent() {
        val video = upnpVideo_folder_1_no_association()
        mClient.addUpnpVideo(video)
        //just check to make sure it works
        assertThat(mClient.getRecentUpnpVideos().blockingFirst()).isEqualTo(video)
    }

    @Test
    fun setting_movie_id_fetches_proper_overview() {
        val video = upnpVideo_folder_3_movie_id()
        val movie = movie()
        assertThat(video.movieId).isEqualTo(movie.id)
        mClient.addUpnpVideo(video)
        mClient.addMovie(movie)
        mClient.setUpnpVideoMovieId(video.id, video.movieId!!)
        val overview = mClient.getUpnpVideoOverview(video.id).blockingGet()
        assertThat(overview).isNotBlank()
        assertThat(overview).isEqualTo(movie.meta.overview)
    }

    @Test
    fun setting_episode_id_fetches_proper_overview() {
        val video = upnpVideo_folder_2_episode_id()
        val episode = tvEpisode()
        assertThat(video.tvEpisodeId).isEqualTo(episode.id)
        mClient.addUpnpVideo(video)
        mClient.addTvEpisodes(listOf(episode))
        mClient.setUpnpVideoTvEpisodeId(video.id, video.tvEpisodeId!!)
        val overview = mClient.getUpnpVideoOverview(video.id).blockingGet()
        assertThat(overview).isNotBlank()
        assertThat(overview).isEqualTo(episode.meta.overview)
    }

    @Test
    fun adding_upnp_video_returns_same_video() {
        val video = upnpVideo_folder_1_no_association()
        mClient.addUpnpVideo(video)
        assertThat(mClient.getUpnpVideo(video.id).blockingGet()).isEqualTo(video)
    }

    @Test
    fun upnp_video_duplicate_add_changes_name_but_not_association() {
        var video = upnpVideo_folder_2_episode_id()
        val episode = tvEpisode()
        val series = tvSeries()
        assertThat(video.tvEpisodeId).isEqualTo(episode.id)
        mClient.addUpnpVideo(video)
        mClient.addTvEpisodes(listOf(episode))
        mClient.addTvSeries(series)
        mClient.setUpnpVideoTvEpisodeId(video.id, episode.id)
        assertThat(mClient.getUpnpVideo(video.id).blockingGet()).isEqualTo(video)
        val video2 = video.copy(meta = video.meta.copy(mediaTitle = "a new title"))
        mClient.addUpnpVideo(video2)
        assertThat(mClient.getUpnpVideo(video2.id).blockingGet()).isEqualTo(video2)
    }

    @Test
    fun upnp_video_duplicate_add_does_not_replace() {
        val video = upnpVideo_folder_1_no_association()
        val uri = mClient.addUpnpVideo(video)
        val uri2 = mClient.addUpnpVideo(video)
        assertThat(uri2).isEqualTo(uri)
    }

    @Test
    fun upnp_device_add_after_increment_scanning_resets_value() {
        val dev = upnpDevices()[0]
        mClient.addUpnpDevice(dev)
        val changed = mClient.incrementUpnpDeviceScanning(dev.id)
        assertThat(changed).isTrue()
        val num = mClient.getUpnpDeviceScanning(dev.id).blockingGet()
        assertThat(num).isEqualTo(1)
        mClient.addUpnpDevice(dev)
        val num2 = mClient.getUpnpDeviceScanning(dev.id).blockingGet()
        assertThat(num2).isEqualTo(0)
    }

    @Test
    fun upnp_device_increment_decrement_scanning() {
        val dev = upnpDevices()[0]
        mClient.addUpnpDevice(dev)
        val changed = mClient.incrementUpnpDeviceScanning(dev.id)
        assertThat(changed).isTrue()
        assertThat(mClient.getUpnpDeviceScanning(dev.id).blockingGet()).isEqualTo(1)
        val changed2 = mClient.incrementUpnpDeviceScanning(dev.id)
        assertThat(changed2).isTrue()
        assertThat(mClient.getUpnpDeviceScanning(dev.id).blockingGet()).isEqualTo(2)

        val changed3 = mClient.decrementUpnpDeviceScanning(dev.id)
        assertThat(changed3).isTrue()
        assertThat(mClient.getUpnpDeviceScanning(dev.id).blockingGet()).isEqualTo(1)

    }

    @Test
    fun upnpdevice_add_twice_returns_same_id() {
        val dev = upnpDevices()[0]
        val uri = mClient.addUpnpDevice(dev)
        val uri2 = mClient.addUpnpDevice(dev)
        assertThat(uri2).isEqualTo(uri)
    }

    @Test
    fun TV_setLastUpdate() {
        mClient.setTvLastUpdate(11111)
        val ret = mClient.getTvLastUpdate().blockingGet()
        assertThat(ret).isEqualTo(11111)
    }

    @Test
    fun TV_setToken() {
        val tok = Token("foounoetu")
        mClient.setTvToken(tok)
        val ret = mClient.getTvToken().blockingGet()
        assertThat(ret).isEqualTo(tok)
    }

    @Test
    fun tv_series_returns_same_as_added() {
        val series = tvSeries()
        mClient.addTvSeries(series)
        assertThat(mClient.getTvSeries(series.id).blockingGet()).isEqualTo(series)
    }

    @Test
    fun tv_epiode_returns_same_as_added(){
        val episode = tvEpisode()
        mClient.addTvEpisodes(listOf(episode))
        assertThat(mClient.getTvEpisode(episode.id).blockingGet()).isEqualTo(episode)
    }

    @Test
    fun movie_returns_same_as_added() {
        val movie = movie()
        mClient.addMovie(movie)
        assertThat(mClient.getMovie(movie.id).blockingGet()).isEqualTo(movie)
    }

    @Test
    fun movie_image_base_url_returs_same_as_added() {
        val url = "http://foo.com/foo"
        mClient.setMovieImageBaseUrl(url)
        assertThat(mClient.getMovieImageBaseUrl()).isEqualTo(url)
    }
}