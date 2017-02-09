/*
 * Copyright (c) 2016 OpenSilk Productions LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.tmdb.api

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opensilk.tmdb.BuildConfig
import org.opensilk.tmdb.api.model.ImageList
import org.opensilk.tmdb.api.model.Movie
import org.opensilk.tmdb.api.model.MovieList
import org.opensilk.tmdb.api.model.TMDbConfig
import org.opensilk.tmdb.api.model.TvEpisode
import org.opensilk.tmdb.api.model.TvSeason
import org.opensilk.tmdb.api.model.TvSeries
import org.opensilk.tmdb.api.model.TvSeriesList
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config

import java.io.InputStream
import java.util.Arrays

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import rx.Single

/**
 * Created by drew on 3/20/16.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class,
        sdk = intArrayOf(21),
        manifest = "build/intermediates/manifests/aapt/debug/AndroidManifest.xml")
class TMDbParsingTest {

    internal lateinit var mApi: TMDb
    internal lateinit var mServer: MockWebServer

    @Before
    @Throws(Exception::class)
    fun setup() {
        mServer = MockWebServer()
        mServer.start()

        val client = OkHttpClient.Builder()
                .addInterceptor(ApiKeyInterceptor.create("fooooooo"))
                .build()

        mApi = Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .client(client)
                .build()
                .create(TMDb::class.java)
    }

    @After
    @Throws(Exception::class)
    fun teardown() {
        mServer.shutdown()
    }

    @Test
    @Throws(Exception::class)
    fun testConfigParses() {
        enqueueResponse("tmdb-config.json")

        val s = mApi.configuration()
        s.subscribe { c -> println(c.images.toString()) }
    }

    @Test
    @Throws(Exception::class)
    fun testMovieSearchParses() {
        enqueueResponse("tmdb-moviesearch-hungergames.json")

        val s = mApi.searchMovie("hunger games", "en")
        s.subscribe { c -> println(Arrays.toString(c.results.toTypedArray())) }
    }

    @Test
    @Throws(Exception::class)
    fun testMovieSearchWithYearParses() {
        enqueueResponse("tmdb-moviesearch-hungergames-2012.json")

        val s = mApi.searchMovie("hunger games", "2012", "en")
        s.subscribe { c -> println(Arrays.toString(c.results.toTypedArray())) }
    }

    @Test
    @Throws(Exception::class)
    fun testMovieParses() {
        enqueueResponse("tmdb-movie-hungergames.json")

        val s = mApi.movie(1111, "en")
        s.subscribe { m -> println(m.toString()) }
    }

    @Test
    @Throws(Exception::class)
    fun testMovieImagesParses() {
        enqueueResponse("tmdb-movieimages-hungergames.json")

        val s = mApi.movieImages(1111, "en")
        s.subscribe { m ->
            //
        }
    }

    @Test
    @Throws(Exception::class)
    fun testTvSearchParses() {
        enqueueResponse("tmdb-search-archer.json")

        val s = mApi.searchTv("archer", "en")
        s.subscribe { m ->

        }
    }

    @Test
    @Throws(Exception::class)
    fun testTvSeriesParses() {
        enqueueResponse("tmdb-tv-archer.json")

        val s = mApi.tvSeries(1111, "en")
        s.subscribe { m -> }
    }

    @Test
    @Throws(Exception::class)
    fun testTvSeasonParses() {
        enqueueResponse("tmdb-tv-archer-s1.json")

        val s = mApi.tvSeason(111, 1, "en")
        s.subscribe { m ->

        }
    }

    @Test
    @Throws(Exception::class)
    fun testTvEpisodeParses() {
        enqueueResponse("tmdb-tv-archer-s1e1.json")

        val s = mApi.tvEpisode(111, 1, 1, "en")
        s.subscribe { m ->

        }
    }

    @Throws(Exception::class)
    private fun enqueueResponse(filename: String) {
        val mr = MockResponse()
        var `is`: InputStream? = null
        try {
            `is` = javaClass.classLoader.getResourceAsStream(filename)
            val b = Buffer()
            b.readFrom(`is`!!)
            mr.body = b
            mServer.enqueue(mr)
        } finally {
            `is`!!.close()
        }
    }

}
