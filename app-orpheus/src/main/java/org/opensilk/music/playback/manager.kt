package org.opensilk.music.playback

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import org.opensilk.common.dagger.ForApplication
import org.opensilk.common.loader.RxLoader
import rx.Observable
import rx.subjects.BehaviorSubject
import rx.subjects.Subject
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Created by drew on 2/25/17.
 */
class PlaybackManager
constructor(
        @ForApplication private val mContext: Context,
        private val mBrowser: MediaBrowser
) {

    private val mController = MediaController(mContext, mBrowser.sessionToken)
    private val mTransport = mController.transportControls
    private val mStateChangeSubject = BehaviorSubject.create<PlaybackState>()
    private val mMetadataSubject = BehaviorSubject.create<MediaMetadata>()
    private val mQueueSubject = BehaviorSubject.create<List<MediaSession.QueueItem>>()


    fun play(mediaItem: MediaBrowser.MediaItem) {
        mTransport.playFromMediaId(mediaItem.mediaId, Bundle.EMPTY)
    }

    fun resume() {
        mTransport.play()
    }

    fun pause() {
        mTransport.pause()
    }

    fun dispose() {
        if (mBrowser.isConnected) mBrowser.disconnect()
    }

    val stateChanges: Observable<PlaybackState>
        get() = mStateChangeSubject.asObservable()

    val metaChanges: Observable<MediaMetadata>
        get() = mMetadataSubject.asObservable()

    val queueChanges: Observable<List<MediaSession.QueueItem>>
        get() = mQueueSubject.asObservable()

    private val mCallback = object : MediaController.Callback() {
        override fun onExtrasChanged(extras: Bundle?) {
            super.onExtrasChanged(extras)
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
        }

        override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) {
            mQueueSubject.onNext(queue?.toList() ?: emptyList())
        }

        override fun onQueueTitleChanged(title: CharSequence?) {
            super.onQueueTitleChanged(title)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            if (state != null) {
                mStateChangeSubject.onNext(state)
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            if (metadata != null) {
                mMetadataSubject.onNext(metadata)
            }
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
        }

        override fun onAudioInfoChanged(info: MediaController.PlaybackInfo?) {
            super.onAudioInfoChanged(info)
        }
    }
}

/**
 *
 */
@Singleton
class PlaybackManagerFactory
@Inject constructor(
        @ForApplication private val mContext: Context,
        @Named("MusicService") private val mComponent: ComponentName

): RxLoader<PlaybackManager> {

    override val observable: Observable<PlaybackManager>
        get() {
            val subject = BehaviorSubject.create<MediaBrowser>()
            val callback = object : MediaBrowser.ConnectionCallback() {
                val browser: MediaBrowser = MediaBrowser(mContext, mComponent, this, null)

                override fun onConnected() {
                    subject.onNext(browser)
                }

                override fun onConnectionSuspended() {
                    subject.onError(PlaybackManagerConnectionSuspended())
                }

                override fun onConnectionFailed() {
                    subject.onError(PlaybackManagerConnectionFailed())
                }
            }
            callback.browser.connect()
            return subject.map { PlaybackManager(mContext, it) }.doOnUnsubscribe {
                if (callback.browser.isConnected) callback.browser.disconnect()
            }
        }
}

/**
 * Exception sent if connection is suspended
 */
class PlaybackManagerConnectionSuspended: Exception()

/**
 * Exception sent if connection failed
 */
class PlaybackManagerConnectionFailed: Exception()