package org.opensilk.music

import android.app.Service
import android.content.Intent
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.support.v4.content.WakefulBroadcastReceiver
import org.opensilk.common.dagger.getDaggerComponent
import org.opensilk.media.playback.PlaybackSession
import javax.inject.Inject

/**
 * Created by drew on 3/12/17.
 */
class PlaybackService: MediaBrowserService() {

    @Inject internal lateinit var mService: PlaybackSession

    override fun onCreate() {
        super.onCreate()
         val cmp = DaggerPlaybackServiceComponent.builder()
                .rootComponent(applicationContext.getDaggerComponent<RootComponent>())
                .build()
        cmp.inject(this)
        sessionToken = mService.sessionToken
    }

    override fun onDestroy() {
        super.onDestroy()
        mService.release()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Intent.ACTION_MEDIA_BUTTON -> {
                mService.onMediaButtonEvent(intent)
                if (intent.hasExtra("orpheus.FROM_MEDIA_BUTTON")) {
                    WakefulBroadcastReceiver.completeWakefulIntent(intent)
                }
            }
        }
        return Service.START_STICKY
    }

    override fun onLoadChildren(parentId: String?, result: Result<MutableList<MediaBrowser.MediaItem>>?) {
        TODO("not implemented")
    }

    override fun onGetRoot(clientPackageName: String?, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot("0", null)
    }
}