package org.opensilk.music.ui.activities

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import org.opensilk.common.glide.PalettizedBitmapDrawable
import org.opensilk.media.MediaBrowserCallback
import org.opensilk.media._icon
import org.opensilk.media._iconUri
import org.opensilk.media._title
import org.opensilk.music.PlaybackService
import org.opensilk.music.R
import org.opensilk.music.data.DataService
import org.opensilk.music.databinding.SheetPlayingBinding
import javax.inject.Inject

/**
 *
 */
interface BaseComponent

/**
 *
 */
@dagger.Module
class BaseModule

/**
 * Created by drew on 6/28/16.
 */
abstract class BaseSlidingActivity: AppCompatActivity(),
        View.OnClickListener, MediaBrowserCallback.Listener {

    protected abstract fun injectSelf()
    protected abstract val mSheetBinding: SheetPlayingBinding

    protected lateinit var mMainWorker: Scheduler.Worker
    protected lateinit var mBrowser: MediaBrowser

    @Inject protected lateinit var mDataService: DataService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injectSelf()

        mMainWorker = AndroidSchedulers.mainThread().createWorker()
        mBrowser = MediaBrowser(this, ComponentName(this, PlaybackService::class.java),
                MediaBrowserCallback(this), null)
        mBrowser.connect()

    }

    override fun onDestroy() {
        super.onDestroy()
        mMainWorker.unsubscribe()
        mBrowser.disconnect()
    }

    override fun onResume() {
        super.onResume()
        if (mBrowser.isConnected && !mMediaCallbackRegistered) {
            mediaController.registerCallback(mPlayingSheetCallback)
            mMediaCallbackRegistered = true
        }
    }

    override fun onPause() {
        super.onPause()
        if (mMediaCallbackRegistered) {
            mediaController.unregisterCallback(mPlayingSheetCallback)
            mMediaCallbackRegistered = false
        }
    }

    override fun onClick(v: View?) {
        if (v == mSheetBinding.peekPlaypause && mBrowser.isConnected) {
            mediaController.transportControls.sendCustomAction(PlaybackActions.TOGGLE, Bundle())
        }
    }

    override fun onBrowserConnected() {
        mediaController = MediaController(this, mBrowser.sessionToken)
        //we get this callback after onResume
        if (!mMediaCallbackRegistered) {
            mediaController.registerCallback(mPlayingSheetCallback)
            mMediaCallbackRegistered = false
        }
    }

    override fun onBrowserDisconnected() {
        if (mMediaCallbackRegistered) {
            mediaController.unregisterCallback(mPlayingSheetCallback)
            mMediaCallbackRegistered = false
        }
        mediaController = null
    }

    private var mMediaCallbackRegistered = false
    private val mPlayingSheetCallback = object: MediaController.Callback() {
        override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) {

        }

        override fun onQueueTitleChanged(title: CharSequence?) {
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            if (state == null) return
            when (state.state) {
                PlaybackState.STATE_PLAYING, PlaybackState.STATE_BUFFERING -> {
                    mSheetBinding.peekPlaypause
                            .setImageResource(R.drawable.ic_pause_black_48dp)
                }
                else -> {
                    mSheetBinding.peekPlaypause
                            .setImageResource(R.drawable.ic_play_black_48dp)
                }
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            if (metadata == null) return
            //set title
            mSheetBinding.peekTitle.text = metadata._title()
            // set icon
            val icon = metadata._iconUri()
            val bitmap = metadata._icon()
            val thumbnail = mSheetBinding.peekThumbnail
            if (icon != null) {
                val opts = RequestOptions().centerCrop(this@BaseSlidingActivity)
                Glide.with(this@BaseSlidingActivity)
                        .`as`(PalettizedBitmapDrawable::class.java)
                        .apply(opts)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .load(icon)
                        .into(thumbnail)
            } else if (bitmap != null) {
                thumbnail.setImageBitmap(bitmap)
            } else {
                thumbnail.setImageResource(R.drawable.ic_music_note_circle_black_48dp)
            }
        }
    }

}