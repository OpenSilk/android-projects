package org.opensilk.media.playback

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import org.opensilk.media.bundle

/**
 * Created by drew on 7/2/17.
 */
class MediaControllerCallback(private val listener: Listener): MediaController.Callback() {

    interface Listener {
        fun onExtrasChanged(extras: Bundle)
        fun onSessionEvent(event: String, extras: Bundle)
        fun onQueueChanged(queue: List<MediaSession.QueueItem>)
        fun onQueueTitleChanged(title: String)
        fun onPlaybackStateChanged(state: PlaybackState)
        fun onMetadataChanged(metadata: MediaMetadata)
        fun onSessionDestroyed()
        fun onAudioInfoChanged(info: MediaController.PlaybackInfo)
    }

    override fun onExtrasChanged(extras: Bundle?) {
        listener.onExtrasChanged(extras ?: bundle())
    }

    override fun onSessionEvent(event: String, extras: Bundle?) {
        listener.onSessionEvent(event, extras ?: bundle())
    }

    override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) {
        listener.onQueueChanged(queue ?: emptyList())
    }

    override fun onQueueTitleChanged(title: CharSequence?) {
        listener.onQueueTitleChanged(title?.toString() ?: "")
    }

    override fun onPlaybackStateChanged(state: PlaybackState) {
        listener.onPlaybackStateChanged(state)
    }

    override fun onMetadataChanged(metadata: MediaMetadata) {
        listener.onMetadataChanged(metadata)
    }

    override fun onSessionDestroyed() {
        listener.onSessionDestroyed()
    }

    override fun onAudioInfoChanged(info: MediaController.PlaybackInfo) {
        listener.onAudioInfoChanged(info)
    }
}