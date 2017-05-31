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

package org.opensilk.tvdb.api

import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opensilk.tvdb.BuildConfig
import org.robolectric.annotation.Config
import org.simpleframework.xml.core.Persister

import java.io.InputStream

import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.opensilk.tvdb.api.model.*
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import rx.Observable

/**
 * Created by drew on 3/19/16.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class,
        sdk = intArrayOf(21),
        manifest = "build/intermediates/manifests/aapt/debug/AndroidManifest.xml")
class TVDbParsingTest {

    internal lateinit var mServer: MockWebServer
    internal lateinit var mApi: TVDb

    @Before
    @Throws(Exception::class)
    fun setup() {
        mServer = MockWebServer()
        mServer.start()

        mApi = Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(AllZippedConverter.Factory.instance())
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .validateEagerly(true)
                .build()
                .create(TVDb::class.java)
    }

    @After
    @Throws(Exception::class)
    fun teardown() {
        mServer.shutdown()
    }

    @Test
    @Throws(Exception::class)
    fun testGetSeriesParses() {
        enqueueResponse("getseries-archer.xml")

        val lst = mApi.getSeriesObservable("archer", "en")
        lst.subscribe { l -> Assertions.assertThat(l.series.size).isEqualTo(3) }
    }

    @Test
    @Throws(Exception::class)
    fun testSeriesParses() {
        enqueueResponse("series-archer.xml")

        val s = mApi.seriesInfoObservable("foo", 111, "en")
        s.subscribe { si ->
            Assertions.assertThat(si.series.id).isEqualTo(110381)
            Assertions.assertThat<Episode>(si.episodes).isNull()
        }
    }

    @Test
    @Throws(Exception::class)
    fun testAllZippedParses() {
        enqueueResponse("series-archer.zip")

        val s = mApi.allZippedObservable("foo", 111, "en")
        s.subscribe { az ->
            Assertions.assertThat(az.series.id).isEqualTo(110381)
            Assertions.assertThat(az.episodes.size).isEqualTo(84)
            Assertions.assertThat(az.banners.size).isEqualTo(80)
            Assertions.assertThat(az.actors.size).isEqualTo(9)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testUpdatesNoneParses() {
        enqueueResponse("updates-none.xml")

        val s = mApi.updatesObservable("none")
        s.subscribe { u ->
            Assertions.assertThat(u.time).isEqualTo(1461009686)
            Assertions.assertThat(u.series).isNull()
            Assertions.assertThat(u.episodes).isNull()
        }
    }

    @Test
    @Throws(Exception::class)
    fun testUpdatesAllParses() {
        enqueueResponse("updates-all.xml")

        val s = mApi.updatesObservable("all", 11111)
        s.subscribe { u ->
            Assertions.assertThat(u.time).isEqualTo(1461008948)
            Assertions.assertThat(u.series.size).isEqualTo(10)
            Assertions.assertThat(u.episodes.size).isEqualTo(21)
        }
    }

    @Test
    @Throws(Exception::class)
    fun test_parseBrokenActorsList() {
        val `is` = openResource("actors-broken.xml")
        try {
            val p = Persister()
            val al = p.read(ActorList::class.java, `is`)
            Assertions.assertThat(al.actors.size).isEqualTo(23)
        } finally {
            `is`.close()
        }
    }

    @Throws(Exception::class)
    private fun enqueueResponse(filename: String) {
        val mr = MockResponse()
        var `is`: InputStream? = null
        try {
            `is` = openResource(filename)
            val b = Buffer()
            b.readFrom(`is`)
            mr.body = b
            mServer.enqueue(mr)
        } finally {
            `is`!!.close()
        }
    }

    @Throws(Exception::class)
    private fun openResource(filename: String): InputStream {
        return javaClass.classLoader.getResourceAsStream(filename)
    }
}
