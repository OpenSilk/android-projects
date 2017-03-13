package org.opensilk.music.playback

import android.content.Context
import android.content.Intent
import android.media.Rating
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.service.media.MediaBrowserService
import org.opensilk.common.dagger.ForApplication
import org.opensilk.media._getMediaMeta
import org.opensilk.media._getMediaUri
import org.opensilk.media.playback.DefaultRenderer
import org.opensilk.media.playback.PlaybackQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by drew on 3/12/17.
 */
class PlaybackServiceService: MediaBrowserService() {

    @Inject internal lateinit var mService: PlaybackSession

    override fun onCreate() {
        super.onCreate()
    }

    override fun onLoadChildren(parentId: String?, result: Result<MutableList<MediaBrowser.MediaItem>>?) {
        TODO("not implemented")
    }

    override fun onGetRoot(clientPackageName: String?, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        TODO("not implemented")
    }
}

@Singleton
class PlaybackSession
@Inject
constructor(
    @ForApplication private val mContext: Context
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
    private val mQueue = PlaybackQueue()


    override fun onMediaButtonEvent(mediaButtonIntent: Intent?): Boolean {
        return super.onMediaButtonEvent(mediaButtonIntent)
    }

    override fun onRewind() {
        TODO()
    }

    override fun onSeekTo(pos: Long) {
        mRenderer.seekTo(pos.toInt())
    }

    override fun onCommand(command: String?, args: Bundle?, cb: ResultReceiver?) {
        TODO()
    }

    override fun onPause() {
        mRenderer.pause()
    }

    override fun onSkipToPrevious() {
        val prev = mQueue.goToPrevious()
        val uri = prev.description._getMediaUri()
        val meta = prev.description._getMediaMeta()
        mRenderer.loadMedia(uri, meta.mediaHeadersMap)
        mRenderer.play()
    }

    override fun onCustomAction(action: String?, extras: Bundle?) {
        TODO()
    }

    override fun onPrepare() {
        TODO()
    }

    override fun onFastForward() {
        TODO()
    }

    override fun onPlay() {
        mRenderer.play()
    }

    override fun onStop() {
        TODO()
    }

    override fun onSkipToQueueItem(id: Long) {
        super.onSkipToQueueItem(id)
    }

    override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
        TODO()
    }

    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        if (mediaId != null) return
        //TODO
    }

    override fun onSkipToNext() {
        val nxt = mQueue.goToNext()
        val uri = nxt.description._getMediaUri()
    }

    override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
        super.onPrepareFromMediaId(mediaId, extras)
    }

    override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
        super.onPrepareFromUri(uri, extras)
    }

    override fun onPlayFromSearch(query: String?, extras: Bundle?) {
        super.onPlayFromSearch(query, extras)
    }

    override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
        super.onPlayFromUri(uri, extras)
    }

    override fun onSetRating(rating: Rating?) {
        super.onSetRating(rating)
    }

}