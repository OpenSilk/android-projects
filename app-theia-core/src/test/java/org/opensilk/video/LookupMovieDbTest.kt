package org.opensilk.video

import android.net.Uri
import com.nhaarman.mockito_kotlin.*
import io.reactivex.Maybe
import io.reactivex.Observable
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.opensilk.media.MovieRef
import org.opensilk.media.database.MediaDAO
import org.opensilk.tmdb.api.TMDb
import org.opensilk.tmdb.api.model.ImageList
import org.opensilk.tmdb.api.model.Movie
import org.opensilk.tmdb.api.model.MovieList
import org.opensilk.tmdb.api.model.TMDbConfig
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Created by drew on 7/26/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class LookupMovieDbTest {

    private lateinit var mServer: MockWebServer
    private lateinit var mLookup: LookupMovieDb
    private lateinit var mClient: MediaDAO
    private lateinit var mVideoClient: VideoAppDAO
    private lateinit var mApi : TMDb

    @Before
    fun setup() {
        mServer = MockWebServer()
        mServer.start()

        mClient = mock()
        mVideoClient = mock()

        mApi = mock()

        mLookup = LookupMovieDb(mClient, mVideoClient, mApi)
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
        verify(mVideoClient).setMovieImageBaseUrl(config.images.baseUrl)

        val ret2 = mLookup.mConfigObservable.blockingFirst()
        assertThat(ret2).isSameAs(config)

        verifyNoMoreInteractions(mApi)
        verifyNoMoreInteractions(mClient)
        verifyNoMoreInteractions(mVideoClient)
    }

    @Test
    fun test_lookup_no_association() {
        val name = "hunger games"
        val year = ""
        val lookup = LookupRequest(upnpVideo_folder_1_no_association())
        lookup.lookupName = name
        lookup.releaseYear = year

        val config = TMDbConfig(TMDbConfig.Images("/foo", null, null, null, null, null, null))
        val movie = Movie(1, name, name, null, null, null, null)
        val movieUri = Uri.parse("/foo/1")
        val imageList = ImageList(1, emptyList(), emptyList())

        val movieRef = movie.toMovieRef()

        whenever(mApi.configurationObservable())
                .thenReturn(Observable.just(config))
        whenever(mApi.searchMovieObservable(name, "en"))
                .thenReturn(Observable.just(MovieList(1, 1, 1, listOf(movie))))
        whenever(mApi.movieObservable(1, "en"))
                .thenReturn(Observable.just(movie))
        whenever(mApi.movieImagesObservable(1, "en"))
                .thenReturn(Observable.just(imageList))
        whenever(mClient.addMovie(movieRef))
                .thenReturn(true)
        whenever(mClient.getMovie(movieRef.id)).thenAnswer(object : Answer<Maybe<MovieRef>> {
            var times = 0
            override fun answer(invocation: InvocationOnMock?): Maybe<MovieRef> {
                if (times++ == 0) {
                    return Maybe.empty()
                } else {
                    return Maybe.just(movieRef)
                }
            }
        })

        val list = mLookup.lookupObservable(lookup).toList().blockingGet()
        assertThat(list.size).isEqualTo(1)
        assertThat(list[0]).isSameAs(movieRef)

        verify(mApi).configurationObservable()
        verify(mApi).searchMovieObservable(name, "en")
        verify(mApi).movieObservable(1, "en")
        verify(mApi).movieImagesObservable(1, "en")
        verify(mClient).addMovie(movieRef)
        verify(mClient, times(2)).getMovie(movieRef.id)
        //additional interactions not mocked
        verify(mVideoClient).setMovieImageBaseUrl(config.images.baseUrl)
        verify(mClient, times(2)).addMovieImages(emptyList())

        verifyNoMoreInteractions(mApi)
        verifyNoMoreInteractions(mClient)
        verifyNoMoreInteractions(mVideoClient)
    }

    @Test
    fun test_lookup_cache_no_network() {
        val name = "hunger games"
        val year = ""
        val meta = LookupRequest(upnpVideo_folder_1_no_association())
        meta.lookupName = name
        meta.releaseYear = year

        val config = TMDbConfig(TMDbConfig.Images("/foo", null, null, null, null, null, null))
        val movieRef = movie()

        whenever(mApi.configurationObservable())
                .thenReturn(Observable.just(config))
        //whenever(mClient)
        //        .thenReturn(DatabaseUris("foo"))
        whenever(mClient.getMovie(movieRef.id))
                .thenReturn(Maybe.just(movieRef))

        val list = mLookup.lookupObservable(meta).toList().blockingGet()
        assertThat(list.size).isEqualTo(1)
        assertThat(list[0]).isSameAs(movieRef)

        verify(mApi).configurationObservable()
        verify(mClient).getMovie(movieRef.id)
        //verify(mClient).uris
        //additional interactions not mocked
        verify(mVideoClient).setMovieImageBaseUrl(config.images.baseUrl)

        verifyNoMoreInteractions(mApi)
        verifyNoMoreInteractions(mClient)
        verifyNoMoreInteractions(mVideoClient)
    }

}