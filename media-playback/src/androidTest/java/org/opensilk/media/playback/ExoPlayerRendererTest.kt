package org.opensilk.media.playback

import android.app.Instrumentation
import android.content.Context
import android.media.session.PlaybackState
import android.net.Uri
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Created by drew on 6/24/17.
 */
@RunWith(AndroidJUnit4::class)
class ExoPlayerRendererTest {

    lateinit var mTestUri: Uri
    lateinit var mContext: Context
    lateinit var mRenderer: ExoPlayerRenderer

    @Before
    fun setup() {
        mContext = InstrumentationRegistry.getTargetContext()
        mRenderer = ExoPlayerRenderer(mContext)
        mTestUri = Uri.parse("file:///android_asset/Broke For Free - 01 - Night Owl.mp3")
    }

    @After
    fun teardown() {
        mRenderer.release()
    }

    @Test(timeout = 30000)
    fun test_prepare() {
        val datasourcefactory = DefaultDataSourceFactory(mContext, "RendererTest")
        val extractorfactor = DefaultExtractorsFactory()
        val eventListener = EventLogger(DefaultTrackSelector())
        val source = ExtractorMediaSource(mTestUri, datasourcefactory, extractorfactor, null, eventListener)
        //do prepare
        mRenderer.prepare(source)
        val latch = CountDownLatch(1)
        val state = AtomicReference<PlaybackState>()
        mRenderer.stateChanges.subscribe {
            Log.e("TEST", it.toString())
            when (it.state) {
                PlaybackState.STATE_PAUSED -> {
                    latch.countDown()
                    state.set(it)
                }
            }
        }
        //wait for async prepare
        while (true) {
            try {
                if (latch.await(1000, TimeUnit.MILLISECONDS))break
            } catch (e: InterruptedException) { }
        }
        //assertions
        assertThat(mRenderer.player.playWhenReady).isEqualTo(false)
        assertThat(mRenderer.player.playbackState).isEqualTo(ExoPlayer.STATE_READY)
        assertThat(state.get().state).isEqualTo(PlaybackState.STATE_PAUSED)
        assertThat(state.get().position).isEqualTo(0L)
        assertThat(state.get().playbackSpeed).isEqualTo(1.0f)
    }

    @Test
    fun test_prepare_play() {
        val datasourcefactory = DefaultDataSourceFactory(mContext, "RendererTest")
        val extractorfactor = DefaultExtractorsFactory()
        val eventListener = EventLogger(DefaultTrackSelector())
        val source = ExtractorMediaSource(mTestUri, datasourcefactory, extractorfactor, null, eventListener)
        mRenderer.prepare(source)
        mRenderer.play()
        val state = AtomicReference<PlaybackState>()
        mRenderer.stateChanges.subscribe {
            when (it.state) {
                PlaybackState.STATE_PLAYING -> {
                    state.set(it)
                }
            }
        }
        while (state.get() == null) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException){}
        }
        assertThat(mRenderer.player.playWhenReady).isEqualTo(true)
        assertThat(mRenderer.player.playbackState).isEqualTo(ExoPlayer.STATE_READY)
        assertThat(state.get().state).isEqualTo(PlaybackState.STATE_PLAYING)
        assertThat(state.get().playbackSpeed).isEqualTo(1.0f)
    }

}