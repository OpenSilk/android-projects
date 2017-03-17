package org.opensilk.music.playback

import android.content.Context
import android.content.Intent
import android.media.MediaDescription
import android.media.Rating
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.media.session.PlaybackState.*
import android.net.Uri
import android.os.*
import android.service.media.MediaBrowserService
import dagger.Component
import dagger.Module
import org.opensilk.common.dagger.AppContextModule
import org.opensilk.common.dagger.ForApplication
import org.opensilk.common.dagger.ServiceScope
import org.opensilk.common.dagger2.getDaggerComponent
import org.opensilk.media._getMediaMeta
import org.opensilk.media._getMediaUri
import org.opensilk.media.playback.DefaultRenderer
import org.opensilk.media.playback.PlaybackQueue
import org.opensilk.music.RootComponent
import org.opensilk.music.RootModule
import org.opensilk.music.data.*
import org.opensilk.music.data.ref.DocumentRef
import org.opensilk.music.data.ref.MediaRef
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers
import rx.android.schedulers.HandlerScheduler
import rx.subscriptions.CompositeSubscription
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.properties.Delegates

/**
 *
 */
@ServiceScope
@Component(
        dependencies = arrayOf(
                RootComponent::class
        ),
        modules = arrayOf(
                PlaybackServiceModule::class,
                MusicAuthorityModule::class
        )
)
interface PlaybackServiceComponent {
    fun inject(service: PlaybackService)
}

/**
 *
 */
@Module
class PlaybackServiceModule {

}


/**
 * Created by drew on 3/12/17.
 */
class PlaybackService: MediaBrowserService() {

    @Inject internal lateinit var mService: PlaybackSession

    override fun onCreate() {
        super.onCreate()
         val cmp = DaggerPlaybackServiceComponent.builder()
                .rootComponent(getDaggerComponent<RootComponent>(applicationContext))
                .build()
        cmp.inject(this)
        sessionToken = mService.sessionToken
    }

    override fun onDestroy() {
        super.onDestroy()
        mService.release()
    }

    override fun onLoadChildren(parentId: String?, result: Result<MutableList<MediaBrowser.MediaItem>>?) {
        TODO("not implemented")
    }

    override fun onGetRoot(clientPackageName: String?, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        TODO("not implemented")
    }
}

@ServiceScope
class PlaybackSession
@Inject
constructor(
    @ForApplication private val mContext: Context,
    private val mClient: MusicProviderClient,
    @Named("MainThread") private val mObserveOn: Scheduler
) : MediaSession.Callback() {

    private val mSession = MediaSession(mContext, "Orpheus")
    init {
        mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mSession.setCallback(this)
        //TODO mSession.setSessionActivity()
        //TODO mSession.setMediaButtonReceiver()
    }
    private val mRenderer = DefaultRenderer(mContext)
    private val mRendererChanges = mRenderer.stateChanges.subscribe { state ->
        when (state) {
            STATE_NONE -> {

            }
            STATE_BUFFERING -> {
                if (mState != STATE_BUFFERING) {
                    mState = STATE_BUFFERING
                }
            }
            STATE_PAUSED -> {
                mState = STATE_PAUSED
            }
            STATE_PLAYING -> {
                mState = STATE_PLAYING
            }
        }
    }
    private val mQueue = PlaybackQueue()
    private val mWorkerThread = HandlerThread("Orpheus-SessionWorker", Process.THREAD_PRIORITY_MORE_FAVORABLE)
    private val mSubscribeOn: Scheduler
    init {
        mWorkerThread.start()
        mSubscribeOn = HandlerScheduler.from(Handler(mWorkerThread.looper))
    }
    private val mWorkerSubscriptions = CompositeSubscription()
    private var mState: Int by Delegates.observable(STATE_NONE, { p, o, n ->
        val bob = PlaybackState.Builder()
        when (n) {
            STATE_BUFFERING -> {
                bob.setActions(ACTION_PAUSE)
            }
        }
        mSession.setPlaybackState(bob.build())
    })

    internal val sessionToken by lazy {
        mSession.sessionToken
    }

    fun release() {
        mWorkerSubscriptions.unsubscribe()
        mWorkerThread.quit()
        mSession.release()
        mRendererChanges.unsubscribe()
    }

    /*
     * Start Callback methods
     */

    override fun onSeekTo(pos: Long) {
        mRenderer.seekTo(pos.toInt())
    }

    override fun onPause() {
        mRenderer.pause()
    }

    override fun onSkipToPrevious() {
        //synchronous
        mQueue.goToPrevious().subscribe { prev ->
            playItem(prev.description)
        }
    }

    override fun onPlay() {
        mRenderer.play()
    }

    override fun onStop() {
        mRenderer.pause()
    }

    override fun onSkipToQueueItem(id: Long) {
        //synchronous
        mQueue.goToItem(id).subscribe { nxt ->
            playItem(nxt.description)
        }
    }

    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        if (mediaId == null) return
        val ref = MediaRef.parse(mediaId)
        if (ref is DocumentRef) {
            val sub = mClient.getMediaRecursive(ref)
                    .subscribeOn(mSubscribeOn)
                    .observeOn(mObserveOn)
                    .subscribe({ item ->
                        mQueue.add(mQueue.newItem(item.description))
                    }, {
                        TODO()
                    }, {
                        //if they sent us a starting uri we move the queue to that item
                        extras?.getString("startwith")?.let { firstId ->
                            mQueue.get().firstOrNull { it.description.mediaId == firstId }?.let {
                                mQueue.goToItem(it.queueId)
                            }
                        }
                        //synchronous
                        mQueue.getCurrent().subscribe {
                            playItem(it.description)
                        }
                    })
            mWorkerSubscriptions.clear()
            mWorkerSubscriptions.add(sub)
            mState = STATE_BUFFERING
            mQueue.clear()
        }
    }

    override fun onSkipToNext() {
        //synchronous
        mQueue.goToNext().subscribe { nxt ->
            playItem(nxt.description)
        }
    }

    override fun onPlayFromSearch(query: String?, extras: Bundle?) {
        TODO()
    }

    override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
        TODO()
    }

    /*
     * End Callback methods
     */

    private fun playItem(description: MediaDescription) {
        val uri = description._getMediaUri()
        val meta = description._getMediaMeta()
        mRenderer.loadMedia(uri, meta.mediaHeadersMap)
        mRenderer.play()
    }

}