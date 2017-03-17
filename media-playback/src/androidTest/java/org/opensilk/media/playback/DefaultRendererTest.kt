package org.opensilk.media.playback

import android.content.Context
import android.media.session.PlaybackState
import android.net.Uri
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4

import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import java.util.Collections
import java.util.concurrent.CountDownLatch

/**
 *
 */
@RunWith(AndroidJUnit4::class)
class DefaultRendererTest {

    private lateinit var appContext: Context
    private lateinit var renderer: DefaultRenderer

    @Before
    fun setup() {
        appContext = InstrumentationRegistry.getTargetContext()
        renderer = DefaultRenderer(appContext)
    }

    @After
    fun destroy() {
        renderer.release()
    }

    @Test
    fun testLoad() {
        val latch = CountDownLatch(1)
        renderer.stateChanges.subscribe { state ->
            when (state) {
                PlaybackState.STATE_PAUSED -> {
                    latch.countDown()
                }
            }
        }
        renderer.loadMedia(Uri.parse("http://datashat.net/music_for_programming_1-datassette.mp3"),
                emptyMap<String, String>())
        latch.await()
        Assertions.assertThat(renderer.state).isEqualTo(PlaybackState.STATE_PAUSED)
    }

    @Test
    fun testLoadPlay() {
        val latch = CountDownLatch(1)
        renderer.stateChanges.subscribe { state ->
            when (state) {
                PlaybackState.STATE_PLAYING -> {
                    latch.countDown()
                }
            }
        }
        renderer.loadMedia(Uri.parse("http://datashat.net/music_for_programming_1-datassette.mp3"),
                emptyMap<String, String>())
        renderer.play()
        latch.await()
        Assertions.assertThat(renderer.state).isEqualTo(PlaybackState.STATE_PLAYING)
//        Assertions.assertThat(renderer.mCurrentPlayer.isPlaying).isTrue()
    }

    @Test
    fun testLoadPlayPause() {
        val latch = CountDownLatch(1)
        val latch2 = CountDownLatch(1)
        renderer.stateChanges.subscribe { state ->
            when (state) {
                PlaybackState.STATE_PLAYING -> {
                    latch.countDown()
                }
                PlaybackState.STATE_PAUSED -> {
                    latch2.countDown()
                }
            }
        }
        renderer.loadMedia(Uri.parse("http://datashat.net/music_for_programming_1-datassette.mp3"),
                emptyMap<String, String>())
        renderer.play()
        latch.await()
        Assertions.assertThat(renderer.state).isEqualTo(PlaybackState.STATE_PLAYING)
//        Assertions.assertThat(renderer.mCurrentPlayer.isPlaying).isTrue()
        renderer.pause()
        latch2.await()
        Assertions.assertThat(renderer.state).isEqualTo(PlaybackState.STATE_PAUSED)
//        Assertions.assertThat(renderer.mCurrentPlayer.isPlaying).isFalse()
    }

}
