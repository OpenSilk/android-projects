package org.opensilk.music.ui.presenters

import android.databinding.BaseObservable
import android.databinding.Bindable
import android.databinding.BindingAdapter
import android.databinding.BindingMethod
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import mortar.MortarScope
import org.opensilk.common.glide.PalettizedBitmapDrawable
import org.opensilk.music.BR
import org.opensilk.music.R
import org.opensilk.music.databinding.SheetPlayingBinding
import java.util.*
import kotlin.properties.Delegates

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
