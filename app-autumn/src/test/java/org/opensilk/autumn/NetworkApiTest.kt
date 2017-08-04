package org.opensilk.autumn

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Created by drew on 8/4/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class NetworkApiTest {


    lateinit var mMockServer: MockWebServer
    lateinit var mNetworkApi: NetworkApi

    @Before
    fun setup() {
        mMockServer = MockWebServer()
        mMockServer.start()
        mNetworkApi = AppModule.provideNetworkApi(OkHttpClient(), mMockServer.url("/").toString())
    }

    @After
    fun after() {
        mMockServer.shutdown()
    }


    @Test
    fun test_json_parses() {
        enqueueResponse("entries.json")
        val list = mNetworkApi.getPlaylists().blockingGet()
        assertThat(list.size).isEqualTo(12)
        val playlist = list[0]
        assertThat(playlist.id).isEqualToIgnoringCase("73F3F654-9EC5-4876-8BF6-474E22029A49")
        assertThat(playlist.assets.size).isEqualTo(4)
        val asset = playlist.assets[0]
        assertThat(asset.id).isEqualToIgnoringCase("D388F00A-5A32-4431-A95C-38BF7FF7268D")
        assertThat(asset.accessibilityLabel).isEqualToIgnoringCase("Greenland")
        assertThat(asset.timeOfDay).isEqualToIgnoringCase("day")
        assertThat(asset.type).isEqualToIgnoringCase("video")
    }

    private fun enqueueResponse(filename: String) {
        javaClass.classLoader.getResourceAsStream(filename).use { s ->
            mMockServer.enqueue(MockResponse().setBody(Buffer().readFrom(s)))
        }
    }
}