package org.opensilk.music.ui

import android.databinding.ViewDataBinding
import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import org.opensilk.common.glide.*
import org.opensilk.common.widget.LetterTileDrawable
import org.opensilk.music.BR
import org.opensilk.music.databinding.*
import rx.subscriptions.CompositeSubscription

class GridArtworkVH(
        binding: RecyclerMediaGridArtworkBinding
) : MediaItemVH<RecyclerMediaGridArtworkBinding>(binding) {

    override fun onBind(mediaTile: MediaTile) {
        super.onBind(mediaTile)

        val uri = mediaTile.iconUri
        if (uri == null || Uri.EMPTY == uri) {
            setLetterDrawableAsArtwork(binding.artworkThumb, mediaTile.letterTileText)
        } else {
            val target = PalettableImageViewTarget.builder()
                    .into(binding.artworkThumb)
                    .intoTextView(TextViewTextColorTarget.builder()
                            .forTitleText(PaletteSwatchType.VIBRANT, PaletteSwatchType.MUTED)
                            .into(binding.tileTitle).build())
                    .intoTextView(TextViewTextColorTarget.builder()
                            .forBodyText(PaletteSwatchType.VIBRANT, PaletteSwatchType.MUTED)
                            .into(binding.tileSubtitle).build())
                    .intoBackground(ViewBackgroundDrawableTarget.builder()
                            .using(PaletteSwatchType.VIBRANT, PaletteSwatchType.MUTED)
                            .into(binding.description).build()).build()
            loadIconUri(uri, target)
        }

    }

    override fun onUnbind() {
        super.onUnbind()
        Glide.with(binding.artworkThumb.context).clear(binding.artworkThumb)
        binding.artworkThumb.setImageBitmap(null)
    }

    override val iconView: View = binding.artworkThumb

}

class ListArtworkVH(
        binding: RecyclerMediaListArtworkBinding
) : MediaItemVH<RecyclerMediaListArtworkBinding>(binding) {

    override fun onBind(mediaTile: MediaTile) {
        super.onBind(mediaTile)

        val uri = mediaTile.iconUri
        if (uri == null || uri == Uri.EMPTY) {
            setLetterDrawableAsArtwork(binding.artworkThumb, mediaTile.letterTileText)
        } else {
            loadIconUri(uri, binding.artworkThumb)
        }
    }

    override fun onUnbind() {
        super.onUnbind()
        Glide.with(binding.artworkThumb.context).clear(binding.artworkThumb)
        binding.artworkThumb.setImageBitmap(null)
    }

    override val iconView: View = binding.artworkThumb

    override val isIconViewCircular: Boolean = true
}

class ListArtworkOnelineVH(
        binding: RecyclerMediaListArtworkOnelineBinding
) : MediaItemVH<RecyclerMediaListArtworkOnelineBinding>(binding) {

    override fun onBind(mediaTile: MediaTile) {
        super.onBind(mediaTile)

        val uri = mediaTile.iconUri
        if (uri == null || uri == Uri.EMPTY) {
            setLetterDrawableAsArtwork(binding.artworkThumb, mediaTile.letterTileText)
        } else {
            loadIconUri(uri, binding.artworkThumb)
        }
    }

    override val iconView: View = binding.artworkThumb

    override val isIconViewCircular: Boolean = true
}

class ListArtworkOnelineInfoVH(
        binding: RecyclerMediaListArtworkOnelineInfoBinding
) : MediaItemVH<RecyclerMediaListArtworkOnelineInfoBinding>(binding) {

    override fun onBind(mediaTile: MediaTile) {
        super.onBind(mediaTile)

        val uri = mediaTile.iconUri
        if (uri == null || uri == Uri.EMPTY) {
            setLetterDrawableAsArtwork(binding.artworkThumb, mediaTile.letterTileText)
        } else {
            loadIconUri(uri, binding.artworkThumb)
        }
        binding.infoBtn.setOnClickListener({ v ->
            val ctx = v.context
            Toast.makeText(ctx, "TODO unimplemented", Toast.LENGTH_SHORT).show()
        })
    }

    override val iconView: View = binding.artworkThumb

    override val isIconViewCircular: Boolean = true
}

open class MediaItemVH<out T : ViewDataBinding>(val binding: T) : RecyclerView.ViewHolder(binding.root) {

    protected val mSubscriptions: CompositeSubscription by lazy {
        CompositeSubscription()
    }

    /**
     * @return ImageView or viewgroup containing imageviews
     */
    open val iconView: View? = null

    open protected val isIconViewCircular: Boolean = false

    open fun onBind(mediaTile: MediaTile) {
        onUnbind()
        binding.setVariable(BR.item, mediaTile)
    }

    open fun onUnbind() {
        mSubscriptions.clear()
    }

    internal fun loadIconUri(iconUri: Uri?, target: PalettableImageViewTarget) {
        val context = target.view.context
        val opts = RequestOptions()
        if (isIconViewCircular) {
            opts.circleCrop(context)
        } else {
            opts.centerCrop(context)
        }
        Glide.with(context)
                .`as`(PalettizedBitmapDrawable::class.java)
                .apply(opts)
                .transition(DrawableTransitionOptions.withCrossFade())
                .load(iconUri)
                .into(target)
    }

    internal fun loadIconUri(iconUri: Uri?, imageView: ImageView) {
        val context = imageView.context
        val opts = RequestOptions()
        if (isIconViewCircular) {
            opts.circleCrop(context)
        } else {
            opts.centerCrop(context)
        }
        Glide.with(context)
                .asDrawable()
                .apply(opts)
                .transition(DrawableTransitionOptions.withCrossFade())
                .load(iconUri)
                .into(imageView)
    }

    internal fun setLetterDrawableAsArtwork(imageView: ImageView, text: String) {
        val resources = imageView.resources
        val drawable = LetterTileDrawable.fromText(resources, text)
        drawable.setIsCircular(isIconViewCircular)
        imageView.setImageDrawable(drawable)
    }
}

class MediaErrorVH(val binding: RecyclerMediaErrorBinding): RecyclerView.ViewHolder(binding.root)