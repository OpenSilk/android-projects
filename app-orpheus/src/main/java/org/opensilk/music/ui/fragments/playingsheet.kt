package org.opensilk.music.ui.fragments

import android.content.ComponentName
import android.databinding.DataBindingUtil
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.media.session.PlaybackState.*
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import dagger.Component
import org.opensilk.common.glide.PalettizedBitmapDrawable
import org.opensilk.media._icon
import org.opensilk.media._iconUri
import org.opensilk.media._title
import org.opensilk.music.R
import org.opensilk.music.databinding.SheetPlayingBinding
import org.opensilk.music.playback.PlaybackManager
import org.opensilk.music.playback.PlaybackService
import org.opensilk.music.ui.activities.BaseComponent
import org.opensilk.music.ui.activities.BaseSlidingActivity
import org.opensilk.music.ui.presenters.BindingPresenter
import java.util.*

@Component(
        dependencies = arrayOf(BaseComponent::class)
)
interface PlayingSheetComponent {
    fun inject(fragment: PlayingSheetFragment)
}

/**
 *
 */
class PlayingSheetPresenter: BindingPresenter() {

    val binding: SheetPlayingBinding
        get() = view!! as SheetPlayingBinding

    val maybeBinding: SheetPlayingBinding?
        get() = view as? SheetPlayingBinding

    private val mDeferredUntilLoad: Deque<() -> Unit> = ArrayDeque()

    private lateinit var mMediaBrowser: MediaBrowser

    override fun onLoad(savedInstanceState: Bundle?) {
        super.onLoad(savedInstanceState)

        while (mDeferredUntilLoad.peek() != null) {
            mDeferredUntilLoad.poll().invoke()
        }
    }

    fun onCurrentIconUri(uri: Uri?) {
        val view = maybeBinding?.peekThumbnail
        view?.let {
            uri?.let {
                val opts = RequestOptions().centerCrop(view.context)
                Glide.with(view.context)
                        .`as`(PalettizedBitmapDrawable::class.java)
                        .apply(opts)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .load(uri)
                        .into(view)
            } ?: view.setImageResource(R.drawable.ic_music_note_circle_black_48dp)
        } ?: mDeferredUntilLoad.add { onCurrentIconUri(uri) }
    }

}

/**
 * Created by drew on 8/15/16.
 */
class PlayingSheetFragment: Fragment() {

    private var mBinding: SheetPlayingBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.sheet_playing, container, false)
        return mBinding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }

    override fun onResume() {
        super.onResume()
        myActivity.mediaController.registerCallback(mCallback)
    }

    override fun onPause() {
        super.onPause()
        myActivity.mediaController.unregisterCallback(mCallback)
    }

    val myActivity: BaseSlidingActivity
        get() = activity as BaseSlidingActivity

    val mCallback = object: MediaController.Callback() {
        override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) {

        }

        override fun onQueueTitleChanged(title: CharSequence?) {
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            if (state == null) return
            when (state.state) {
                STATE_PLAYING, STATE_BUFFERING -> {
                    mBinding?.peekPlaypause?.setImageResource(R.drawable.ic_pause_black_48dp)
                }
                else -> {
                    mBinding?.peekPlaypause?.setImageResource(R.drawable.ic_play_black_48dp)
                }
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            if (metadata == null) return
            //set title
            mBinding?.peekTitle?.text = metadata._title()
            // set icon
            val icon = metadata._iconUri()
            val thumbnail = mBinding?.peekThumbnail
            if (icon != null && thumbnail != null) {
                Glide.with(this@PlayingSheetFragment)
                        .load(icon)
                        .into(thumbnail)
            } else {
                val bitmap = metadata._icon()
                if (bitmap != null) {
                    thumbnail?.setImageBitmap(bitmap)
                } else {
                    thumbnail?.setImageResource(R.drawable.ic_music_note_circle_black_48dp)
                }
            }
        }
    }

}