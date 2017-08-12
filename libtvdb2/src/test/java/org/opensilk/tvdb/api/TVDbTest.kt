package org.opensilk.tvdb.api

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opensilk.tvdb.api.model.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.nio.charset.Charset

/**
 * Created by drew on 7/25/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class TVDbTest {

    lateinit var mServer: MockWebServer
    lateinit var mApi: TVDb
    val mToken = Token("foo")

    @Before
    fun setup() {
        mServer = MockWebServer()
        mServer.start()
        val clinent = OkHttpClient.Builder().addInterceptor { chain ->
            val log = chain.request().newBuilder().build()
            System.out.println(log.url())
            if (log.headers() != null) {
                for (n in log.headers().names()) {
                    System.out.println("$n: ${log.headers().get(n)}")
                }
            }
            val body = log.body()
            if (body != null) {
                val buf = Buffer()
                body.writeTo(buf)
                System.out.println(buf.readString(Charset.forName("UTF-8")))
            }
            chain.proceed(chain.request())
        }.build()
        mApi = Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .validateEagerly(true)
                .client(clinent)
                .build()
                .create(TVDb::class.java)
    }

    @After
    fun teardown() {
        mServer.shutdown()
    }

    @Test
    fun updated() {
        enqueueResponse("updated.json")
        val resp = mApi.updated(mToken, 1).blockingGet()
        assertThat(resp.errors).isNull()
        assertThat(resp.data[0].id).isEqualTo(329559)
    }

    @Test
    fun series_image_poster_archer() {
        enqueueResponse("series-image-poster-archer.json")
        val resp = mApi.seriesImagesQuery(mToken, 1, "poster").blockingGet()
        assertThat(resp.errors).isNull()
        assertThat(resp.data[0].keyType).isEqualTo("poster")
    }

    @Test
    fun series_episodes_archer() {
        enqueueResponse("series-episodes-archer.json")
        val resp = mApi.seriesEpisodes(mToken, 1).blockingGet()
        assertThat(resp.errors).isNull()
        assertThat(resp.data[0].episodeName).isEqualTo("Mole Hunt")
        assertThat(resp.links).isEqualTo(Links(1,1))
    }

    @Test
    fun series_episodes_archer2() {
        enqueueResponse("series-episodes-archer2.json")
        val resp = mApi.seriesEpisodes(mToken, 1).blockingGet()
        assertThat(resp.errors).isNull()
        assertThat(resp.data[0].episodeName).isEqualTo("No Clothes for the Party")
        assertThat(resp.links).isEqualTo(Links(1,1))
    }

    @Test
    fun series_archer() {
        enqueueResponse("series-archer.json")
        val resp = mApi.series(mToken, 1).blockingGet()
        val d = resp.data
        assertThat(d.seriesName).isEqualTo("Archer (2009)")
        assertThat(resp.errors).isNull()
    }

    @Test
    fun series_archer2() {
        enqueueResponse("series-archer2.json")
        val resp = mApi.series(mToken, 1).blockingGet()
        assertThat(resp.data.seriesName).isEqualTo("Meet Corliss Archer")
        assertThat(resp.data.overview).isNull()
    }

    @Test
    fun search_series_archer() {
        enqueueResponse("search-series-archer.json")
        val resp = mApi.searchSeries(mToken, "archer").blockingGet()
        assertThat(resp.data.size).isEqualTo(4)
    }

    @Test
    fun search_series_archer_2009() {
        enqueueResponse("search-series-archer-2009.json")
        val resp = mApi.searchSeries(mToken, "archer").blockingGet()
        assertThat(resp.data.size).isEqualTo(1)
        val data = resp.data[0]
        assertThat(data).isEqualTo(SeriesSearch(
                aliases =  listOf("Archer 2009", "Archer Dreamland"),
                banner = "graphical/110381-g5.jpg",
                firstAired = "2009-09-17",
                id = 110381,
                overview = "Covert black ops and espionage take a back seat to zany personalities and relationships between secret agents and drones.",
                seriesName = "Archer (2009)"
        ))
    }

    @Test
    fun login_token_normal() {
        enqueueResponse("login-token.json")
        val token = mApi.login(Auth("foo")).blockingGet()
        assertThat(token.token).isEqualTo("foobar")
    }

    @Throws(Exception::class)
    private fun enqueueResponse(filename: String) {
        javaClass.classLoader.getResourceAsStream(filename).use { s ->
            mServer.enqueue(MockResponse().setBody(Buffer().readFrom(s)))
        }
    }

}