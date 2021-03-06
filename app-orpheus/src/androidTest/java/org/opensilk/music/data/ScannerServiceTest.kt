package org.opensilk.music.data

import android.support.test.InstrumentationRegistry
import android.support.test.rule.ServiceTestRule
import android.support.test.runner.AndroidJUnit4
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Created by drew on 6/27/16.
 */
@RunWith(AndroidJUnit4::class)
class ScannerServiceTest {

    @Rule @JvmField val mWebServer = MockWebServer()
    @Rule @JvmField val mServiceRule = ServiceTestRule()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        Timber.uprootAll()
        Timber.plant(Timber.DebugTree())
    }

    @Test
    @Throws(Exception::class)
    fun test_scanItem() {
        val files = arrayOf(
                "Music/Aurora-Album1/01 Runaway.mp3",
                "Music/Aurora-Album1/02 Conqueror.mp3",
                "Music/Aurora-Album1/03 Running With the Wolves.mp3",
                "Music/Aurora-Album1/04 Lucky.mp3",
                "Music/Aurora-Album1/05 Winter Bird.mp3",
                "Music/Aurora-Album1/06 I Went Too Far.mp3"
        )
    }

    private fun enqueueItem(file: String) {
        val `is` = InstrumentationRegistry.getContext().assets.open(file)
        mWebServer.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().readFrom(`is`)))
        `is`.close()
    }
}
