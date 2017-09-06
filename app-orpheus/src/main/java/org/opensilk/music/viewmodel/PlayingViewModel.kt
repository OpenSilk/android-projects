package org.opensilk.music.viewmodel

import android.arch.lifecycle.*
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import org.opensilk.dagger2.ForApp
import org.opensilk.media.playback.MediaBrowserCallback
import org.opensilk.media.playback.MediaControllerCallback
import org.opensilk.music.PlaybackService
import javax.inject.Inject

/**
 * Created by drew on 9/5/17.
 */
class PlayingViewModel @Inject constructor(
        @ForApp private val mContext: Context
): ViewModel(), LifecycleObserver, MediaBrowserCallback.Listener, MediaControllerCallback.Listener {

    val playbackState = MutableLiveData<PlaybackState>()
    val metadata = MutableLiveData<MediaMetadata>()

    private lateinit var mBrowser: MediaBrowser
    private lateinit var mController: MediaController

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        mBrowser = MediaBrowser(mContext, ComponentName(mContext, PlaybackService::class.java),
                MediaBrowserCallback(this), null)
        mBrowser.connect()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        mBrowser.disconnect()
    }

    override fun onBrowserConnected() {
        mController = MediaController(mContext, mBrowser.sessionToken)
    }

    override fun onBrowserDisconnected() {
        TODO("not implemented")
    }

    override fun onExtrasChanged(extras: Bundle) {
        TODO("not implemented")
    }

    override fun onSessionEvent(event: String, extras: Bundle) {
        TODO("not implemented")
    }

    override fun onQueueChanged(queue: List<MediaSession.QueueItem>) {
        TODO("not implemented")
    }

    override fun onQueueTitleChanged(title: String) {
        TODO("not implemented")
    }

    override fun onPlaybackStateChanged(state: PlaybackState) {
        playbackState.postValue(state)
    }

    override fun onMetadataChanged(metadata: MediaMetadata) {

    }

    override fun onSessionDestroyed() {
        TODO("not implemented")
    }

    override fun onAudioInfoChanged(info: MediaController.PlaybackInfo) {
        TODO("not implemented")
    }

}