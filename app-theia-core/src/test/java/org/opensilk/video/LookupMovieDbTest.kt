package org.opensilk.video

import android.net.Uri
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opensilk.media.MediaMeta
import org.opensilk.media.MediaRef
import org.opensilk.media.UPNP_VIDEO
import org.opensilk.media.UpnpVideoId
import org.opensilk.tmdb.api.TMDb
import org.opensilk.tmdb.api.model.ImageList
import org.opensilk.tmdb.api.model.Movie
import org.opensilk.tmdb.api.model.MovieList
import org.opensilk.tmdb.api.model.TMDbConfig
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Created by drew on 7/26/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class LookupMovieDbTest {

    private lateinit var mServer: MockWebServer
    private lateinit var mLookup: LookupMovieDb
    private lateinit var mClient: DatabaseClient
    private lateinit var mApi : TMDb

    @Before
    fun setup() {
        mServer = MockWebServer()
        mServer.start()

        mClient = mock()

        mApi = mock()

        mLookup = LookupMovieDb(mClient, mApi)
    }

    @After
    fun after() {
        mServer.shutdown()
    }

    @Test
    fun test_config_only_hits_network_once() {
        val config = TMDbConfig(TMDbConfig.Images("/foo", null, null, null, null, null, null))
        whenever(mApi.configurationObservable())
                .thenReturn(Observable.just(config))

        val ret = mLookup.mConfigObservable.blockingFirst()
        assertThat(ret).isSameAs(config)

        verify(mApi).configurationObservable()
        verify(mClient).setMovieImageBaseUrl(config.images.baseUrl)

        val ret2 = mLookup.mConfigObservable.blockingFirst()
        assertThat(ret2).isSameAs(config)

        verifyNoMoreInteractions(mApi)
        verifyNoMoreInteractions(mClient)
    }

    @Test
    fun test_lookup_no_association() {
        val name = "hunger games"
        val year = ""
        val meta = MediaMeta()
        meta.mediaId = MediaRef(UPNP_VIDEO, UpnpVideoId("foo", "1")).toJson()
        meta.extras.putString(LOOKUP_NAME, name)

        val config = TMDbConfig(TMDbConfig.Images("/foo", null, null, null, null, null, null))
        val movie = Movie(1, name, name, null, null, null, null)
        val movieUri = Uri.parse("/foo/1")
        val imageList = ImageList(1, emptyList(), emptyList())

        val movieMeta = MediaMeta()
        movieMeta.rowId = 1
        movieMeta.title = name

        whenever(mApi.configurationObservable())
                .thenReturn(Observable.just(config))
        whenever(mApi.searchMovieObservable(name, "en"))
                .thenReturn(Observable.just(MovieList(1, 1, 1, listOf(movie))))
        whenever(mApi.movieObservable(1, "en"))
                .thenReturn(Observable.just(movie))
        whenever(mApi.movieImagesObservable(1, "en"))
                .thenReturn(Observable.just(imageList))
        whenever(mClient.getMovieAssociation(name, year))
                .thenReturn(Single.error(NoSuchItemException()))
        whenever(mClient.addMovie(movie))
                .thenReturn(movieUri)
        whenever(mClient.getMovie(1))
                .thenReturn(Single.just(movieMeta))

        val list = mLookup.lookupObservable(meta).toList().blockingGet()
        assertThat(list.size).isEqualTo(1)
        assertThat(list[0]).isSameAs(movieMeta)

        verify(mApi).configurationObservable()
        verify(mApi).searchMovieObservable(name, "en")
        verify(mApi).movieObservable(1, "en")
        verify(mApi).movieImagesObservable(1, "en")
        verify(mClient).getMovieAssociation(name, year)
        verify(mClient).addMovie(movie)
        verify(mClient).getMovie(1)
        //additional interactions not mocked
        verify(mClient).setMovieImageBaseUrl(config.images.baseUrl)
        verify(mClient).addMovieImages(imageList)

        verifyNoMoreInteractions(mApi)
        verifyNoMoreInteractions(mClient)
    }

    @Test
    fun test_lookup_cache_no_network() {
        val name = "hunger games"
        val year = ""
        val meta = MediaMeta()
        meta.mediaId = MediaRef(UPNP_VIDEO, UpnpVideoId("foo", "1")).toJson()
        meta.extras.putString(LOOKUP_NAME, name)

        val config = TMDbConfig(TMDbConfig.Images("/foo", null, null, null, null, null, null))

        val movieMeta = MediaMeta()
        movieMeta.rowId = 1
        movieMeta.title = name

        whenever(mApi.configurationObservable())
                .thenReturn(Observable.just(config))
        whenever(mClient.getMovieAssociation(name, year))
                .thenReturn(Single.just(1))
        whenever(mClient.uris)
                .thenReturn(DatabaseUris("foo"))
        whenever(mClient.getMovie(1))
                .thenReturn(Single.just(movieMeta))

        val list = mLookup.lookupObservable(meta).toList().blockingGet()
        assertThat(list.size).isEqualTo(1)
        assertThat(list[0]).isSameAs(movieMeta)

        verify(mApi).configurationObservable()
        verify(mClient).getMovieAssociation(name, year)
        verify(mClient).getMovie(1)
        verify(mClient).uris
        //additional interactions not mocked
        verify(mClient).setMovieImageBaseUrl(config.images.baseUrl)

        verifyNoMoreInteractions(mApi)
        verifyNoMoreInteractions(mClient)
    }

}