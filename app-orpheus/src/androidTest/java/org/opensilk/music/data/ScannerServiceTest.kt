package org.opensilk.music.data

import android.content.Intent
import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ServiceTestRule
import android.support.test.runner.AndroidJUnit4

import org.assertj.core.api.Assertions.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import java.io.IOException
import java.io.InputStream

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import rx.exceptions.Exceptions
import rx.schedulers.Schedulers
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
        ScannerService.sObserveOn = Schedulers.immediate()
        ScannerService.sSubscribeOn = Schedulers.immediate()
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

        val mConnection = IScannerService.Stub.asInterface(mServiceRule.bindService(
                Intent(InstrumentationRegistry.getTargetContext(), ScannerService::class.java)))
        assertThat(mConnection).isNotNull()

        for (file in files) {
            enqueueItem(file)
            val item = TestDocuments.getItem(mWebServer.url("/testItem").toString(), file)
            assertThat(item).isNotNull()
            val goodMeta = item._getMediaMeta()
            val subscriber = object : ScannerService.ScannerSubscriber() {
                var successCalled = false
                override fun onSuccess(value: MediaBrowser.MediaItem) {
                    successCalled = true
                    val scannedMeta = value._getMediaMeta()
                    assertThat(scannedMeta.albumName).isEqualTo(goodMeta.albumName)
                    assertThat(scannedMeta.albumArtistName).isEqualTo(goodMeta.albumArtistName)
                    assertThat(scannedMeta.artistName).isEqualTo(goodMeta.artistName)
                    assertThat(scannedMeta.bitrate).isEqualTo(goodMeta.bitrate)
                    assertThat(scannedMeta.trackNumber).isEqualTo(goodMeta.trackNumber)
                    assertThat(scannedMeta.isCompilation).isEqualTo(goodMeta.isCompilation)
                    assertThat(scannedMeta.discNumber).isEqualTo(goodMeta.discNumber)
                    assertThat(scannedMeta.duration).isEqualTo(goodMeta.duration)
                    assertThat(scannedMeta.genreName).isEqualTo(goodMeta.genreName)
                    assertThat(scannedMeta.mimeType).isEqualTo(goodMeta.mimeType)
                    assertThat(scannedMeta.title).isEqualTo(goodMeta.title)
                }

                override fun onError(error: Throwable) {
                    throw Exceptions.propagate(error)
                }
            }
            val s = ScannerService.ScannerSubscription(mConnection.scanItem(item, subscriber.cb))
            assertThat(s).isNotNull()
            assertThat(subscriber.successCalled).isTrue()
        }
    }

    private fun enqueueItem(file: String) {
        val `is` = InstrumentationRegistry.getContext().assets.open(file)
        mWebServer.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().readFrom(`is`)))
        `is`.close()
    }
}
