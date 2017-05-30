package org.opensilk.video.telly

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.v17.leanback.widget.*
import android.support.v4.app.ActivityOptionsCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import org.opensilk.common.rx.RxUtils
import org.opensilk.media._getMediaMeta
import org.opensilk.media._getMediaTitle
import org.opensilk.video.DataService
import org.opensilk.video.VideoDescInfo
import org.opensilk.video.VideoProgressInfo
import org.opensilk.video.telly.databinding.MediaitemListCardBinding
import rx.Subscription
import rx.functions.Action1
import timber.log.Timber
import javax.inject.Inject

/**
 *
 */
class MediaItemImageCardView(context: Context): ImageCardView(context) {
    override fun setSelected(selected: Boolean) {
        updateBackgroundColor(selected)
        super.setSelected(selected)
    }
    fun updateBackgroundColor(selected: Boolean) {
        val color = context.getColor(if (selected) R.color.selected_background else R.color.default_background)
        // Both background colors should be set because the view's background is temporarily visible
        // during animations.
        setBackgroundColor(color)
        setInfoAreaBackgroundColor(color)
    }
}

/**
 * Supposed to be stoteless so dont need @ActivityScope
 */
class MediaItemPresenter
@Inject constructor() : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val cardView = MediaItemImageCardView(parent.context)

        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        cardView.updateBackgroundColor(false)

        return Presenter.ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {

        val mediaItem = item as MediaBrowser.MediaItem
        val description = mediaItem.description
        val metaExtras = mediaItem._getMediaMeta()

        val cardView = viewHolder.view as MediaItemImageCardView
        val context = cardView.context
        cardView.titleText = description.title
        cardView.contentText = description.subtitle

        val resources = cardView.resources
        val cardWidth = resources.getDimensionPixelSize(R.dimen.card_width)
        val cardHeight = resources.getDimensionPixelSize(R.dimen.card_height)
        cardView.setMainImageDimensions(cardWidth, cardHeight)

        val iconResource: Int
        if (metaExtras.artworkResourceId >= 0) {
            iconResource = metaExtras.artworkResourceId
        } else if (mediaItem.isBrowsable) {
            iconResource = R.drawable.folder_48dp
        } else if (mediaItem.isPlayable) {
            iconResource = R.drawable.movie_48dp
        } else {
            iconResource = R.drawable.file_48dp
        }

        if (description.iconUri != null) {
            val options = RequestOptions()
                    .fitCenter()
                    .fallback(iconResource)
            Glide.with(cardView.context)
                    .asDrawable()
                    .apply(options)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .load(description.iconUri)
                    .into(cardView.mainImageView)
        } else {
            cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_INSIDE)
            cardView.mainImage = context.getDrawable(iconResource)
        }
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        val cardView = viewHolder.view as MediaItemImageCardView
        // Remove references to images so that the garbage collector can free up memory
        cardView.badgeImage = null
        cardView.mainImage = null
    }
}

/**
 * supposed to be stateless so dont need @ActivityScope
 */
class MediaItemListPresenter
@Inject constructor() : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: MediaitemListCardBinding = DataBindingUtil.inflate(inflater,
                R.layout.mediaitem_list_card, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        val vh = viewHolder as ViewHolder
        val mediaItem = item as MediaBrowser.MediaItem
        vh.setMediaItem(mediaItem)
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        val vh = viewHolder as ViewHolder
        Glide.with(vh.view.context).clear(vh.binding.icon)
        vh.binding.icon.setImageDrawable(null)
    }

    class ViewHolder(val binding: MediaitemListCardBinding) : Presenter.ViewHolder(binding.root) {

        internal fun setMediaItem(mediaItem: MediaBrowser.MediaItem) {
            val description = mediaItem.description
            val metaExtras = mediaItem._getMediaMeta()
            //set progress
            binding.progressInfo = VideoProgressInfo(metaExtras.lastPlaybackPosition, metaExtras.duration)
            //set description
            binding.desc = VideoDescInfo(mediaItem._getMediaTitle(),
                    description.subtitle?.toString() ?: "",
                    description.description?.toString() ?: "")
            //load icon
            val iconResource: Int
            if (metaExtras.artworkResourceId >= 0) {
                iconResource = metaExtras.artworkResourceId
            } else if (mediaItem.isBrowsable) {
                iconResource = R.drawable.folder_48dp
            } else if (mediaItem.isPlayable) {
                iconResource = R.drawable.movie_48dp
            } else {
                iconResource = R.drawable.file_48dp
            }

            if (description.iconUri != null) {
                val options = RequestOptions()
                        .fitCenter()
                        .fallback(iconResource)
                Glide.with(view.context)
                        .asDrawable()
                        .apply(options)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .load(description.iconUri)
                        .into(binding.icon)
            } else {
                binding.icon.setImageResource(iconResource)
            }
        }
    }
}

/**
 *
 */
class MediaItemClickListener : OnItemViewClickedListener {

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder, item: Any,
                               rowViewHolder: RowPresenter.ViewHolder, row: Row) {
        val context = itemViewHolder.view.context
        val mediaItem = item as MediaBrowser.MediaItem
        val mediaId = newMediaRef(mediaItem.mediaId)
        when (mediaId.kind) {
            UPNP_DEVICE -> {
                val intent = Intent(context, FolderActivity::class.java)
                intent.putExtra(EXTRA_MEDIAITEM, mediaItem)
                context.startActivity(intent)
            }
            else -> {
                Toast.makeText(context, "Unhandled ItemClick", Toast.LENGTH_LONG).show()
            }
        }
    }
}
