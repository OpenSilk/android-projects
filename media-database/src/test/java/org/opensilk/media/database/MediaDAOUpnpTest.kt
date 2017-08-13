package org.opensilk.media.database

import android.content.ContentResolver
import android.content.pm.ProviderInfo
import android.net.Uri
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opensilk.media.testdata.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Created by drew on 7/19/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, application = TestApp::class)
class MediaDAOUpnpTest {

    lateinit var mClient: MediaDAO
    lateinit var mProvider: MediaProvider
    lateinit var mResolver: ContentResolver
    lateinit var mApiHelper: ApiHelper

    @Before
    fun setupProvider() {
        mProvider = MediaProvider()

        val providerInfo = ProviderInfo()
        providerInfo.authority = "foo.authority"
        mProvider = Robolectric.buildContentProvider(MediaProvider::class.java)
                .create(providerInfo).get()
        mProvider.mMediaDB = MediaDB(RuntimeEnvironment.application)
        mProvider.mUris = MediaDBUris("foo.authority")

        mResolver = RuntimeEnvironment.application.contentResolver

        mApiHelper = object : ApiHelper {
            override fun tvImagePosterUri(path: String): Uri {
                return Uri.parse(TVDB_BANNER_ROOT).buildUpon().appendPath(path).build()
            }

            override fun tvImageBackdropUri(path: String): Uri {
                return Uri.parse(TVDB_BANNER_ROOT).buildUpon().appendPath(path).build()
            }

            override fun movieImagePosterUri(path: String): Uri {
                return Uri.parse(TVDB_BANNER_ROOT).buildUpon().appendPath(path).build()
            }

            override fun movieImageBackdropUri(path: String): Uri {
                return Uri.parse(TVDB_BANNER_ROOT).buildUpon().appendPath(path).build()
            }
        }
        mClient = MediaDAO(mResolver, MediaDBUris("foo.authority"), mApiHelper)
    }

    @After
    fun teardown() {
        mProvider.shutdown()
    }

    @Test
    fun upnpdevice_add_ignores_update_id() {
        val dev = upnpDevice_all_meta()
        val dev_ret = dev.copy(meta = dev.meta.copy(updateId = 0))
        assertThat(mClient.addUpnpDevice(dev)).isTrue()
        assertThat(mClient.getUpnpDevice(dev.id).blockingGet()).isEqualTo(dev_ret)

        val dev2 = upnpDevice_minimal_meta()
        assertThat(mClient.addUpnpDevice(dev2)).isTrue()
        assertThat(mClient.getUpnpDevice(dev2.id).blockingGet()).isEqualTo(dev2)
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
    fun adding_upnp_video_returns_with_proper_title() {
        val video = upnpVideo_folder_1_no_association()
        mClient.addUpnpVideo(video)
        val video_ret = video.copy(meta = video.meta.copy(title = video.meta.title, originalTitle = video.meta.title))
        assertThat(mClient.getUpnpVideo(video.id).blockingGet()).isEqualTo(video_ret)
    }

    @Test
    fun upnp_video_change_title_reflected_as_original_title() {
        val video = upnpVideo_folder_2_episode_id()
        val episode = tvEpisode()
        val series = tvSeries()
        assertThat(video.tvEpisodeId).isEqualTo(episode.id)
        mClient.addUpnpVideo(video)
        mClient.addTvEpisodes(listOf(episode))
        mClient.addTvSeries(series)
        mClient.setUpnpVideoTvEpisodeId(video.id, episode.id)
        assertThat(mClient.getUpnpVideo(video.id).blockingGet()).isEqualTo(video)
        val video2_insert = video.copy(meta = video.meta.copy(title = "a new title", originalTitle = ""))
        val video2_return = video.copy(meta = video.meta.copy(originalTitle = "a new title"))
        mClient.addUpnpVideo(video2_insert)
        assertThat(mClient.getUpnpVideo(video2_insert.id).blockingGet()).isEqualTo(video2_return)
    }

}