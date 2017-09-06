package org.opensilk.music.ui

import android.media.session.PlaybackState
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import org.opensilk.common.glide.PalettableImageViewTarget
import org.opensilk.common.glide.PalettizedBitmapDrawable
import org.opensilk.media._icon
import org.opensilk.media._iconUri
import org.opensilk.media._title
import org.opensilk.music.R
import org.opensilk.music.data.LiveDataObserver
import org.opensilk.music.data.autoClearedValue
import org.opensilk.music.databinding.SheetPlayingBinding
import org.opensilk.music.viewmodel.PlayingViewModel
import org.opensilk.music.viewmodel.fragmentViewModel

/**
 * Created by drew on 9/5/17.
 */
class PlayingSheetFragment: BaseMusicFragment() {

    private var mBinding: SheetPlayingBinding by autoClearedValue()
    private val mViewModel: PlayingViewModel by fragmentViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel.playbackState.observe(this, LiveDataObserver { state ->
            when (state.state) {
                PlaybackState.STATE_PLAYING,
                PlaybackState.STATE_BUFFERING -> {
                    mBinding.peekPlaypause.setImageResource(R.drawable.ic_pause_circle_48dp)
                }
                else -> {
                    mBinding.peekPlaypause.setImageResource(R.drawable.ic_play_circle_48dp)
                }
            }
        })
        mViewModel.metadata.observe(this, LiveDataObserver { meta ->
            //set title
            mBinding.peekTitle.text = meta._title()
            // set icon
            val icon = meta._iconUri()
            val bitmap = meta._icon()
            val thumbnail = mBinding.peekThumbnail
            when {
                icon != null -> {
                    Glide.with(this)
                            .`as`(PalettizedBitmapDrawable::class.java)
                            .apply(RequestOptions().centerCrop())
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .load(icon)
                            .into(PalettableImageViewTarget.builder()
                                    .into(thumbnail)
                                    .build())
                }
                bitmap != null -> thumbnail.setImageBitmap(bitmap)
                else -> thumbnail.setImageResource(R.drawable.ic_music_note_circle_black_48dp)
            }
        })

    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = SheetPlayingBinding.inflate(inflater, container, false)
        return mBinding.root
    }

}