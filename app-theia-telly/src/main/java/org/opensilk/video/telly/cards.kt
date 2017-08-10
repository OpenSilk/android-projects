package org.opensilk.video.telly

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.v17.leanback.widget.*
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import org.opensilk.reactivex2.subscribeIgnoreError
import org.opensilk.media.*
import org.opensilk.video.*
import org.opensilk.video.telly.databinding.MediaitemListCardBinding
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
 *
 */
class MediaItemPresenter
@Inject constructor() : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val cardView = MediaItemImageCardView(parent.context)

        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        cardView.updateBackgroundColor(false)

        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {

        val mediaItem = item as MediaBrowser.MediaItem
        val description = mediaItem.description

        val cardView = viewHolder.view as MediaItemImageCardView
        val context = cardView.context
        cardView.titleText = description.title
        cardView.contentText = description.subtitle

        val resources = cardView.resources
        val cardWidth = resources.getDimensionPixelSize(R.dimen.card_width)
        val cardHeight = resources.getDimensionPixelSize(R.dimen.card_height)
        cardView.setMainImageDimensions(cardWidth, cardHeight)

        val iconResource = if (mediaItem.isBrowsable) {
            R.drawable.folder_48dp
        } else if (mediaItem.isPlayable) {
            R.drawable.movie_48dp
        } else {
            R.drawable.file_48dp
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

    class ViewHolder(view: MediaItemImageCardView): Presenter.ViewHolder(view)
}

/**
 *
 */
class MediaItemListPresenter
@Inject constructor(
        val mDatabaseClient: DatabaseClient
) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: MediaitemListCardBinding = DataBindingUtil.inflate(inflater,
                R.layout.mediaitem_list_card, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        val vh = viewHolder as ViewHolder
        val mediaItem = item as MediaBrowser.MediaItem
        vh.bind(mediaItem)
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        val vh = viewHolder as ViewHolder
        vh.unbind()
    }

    inner class ViewHolder(val binding: MediaitemListCardBinding) : Presenter.ViewHolder(binding.root) {
        private val disposables = CompositeDisposable()

        internal fun unbind() {
            Glide.with(view.context).clear(binding.icon)
            binding.icon.setImageDrawable(null)
            disposables.clear()
        }

        internal fun bind(mediaItem: MediaBrowser.MediaItem) {
            val description = mediaItem.description
            val ref = parseMediaId(mediaItem.mediaId)

            //set description
            binding.description = description

            //load icon
            val iconResource = if (mediaItem.isBrowsable) {
                R.drawable.folder_48dp
            } else if (mediaItem.isPlayable) {
                R.drawable.movie_48dp
            } else {
                R.drawable.file_48dp
            }
            if (!description.iconUri.isEmpty()) {
                val options = RequestOptions()
                        .centerCrop()
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

            //set progress
            when (ref) {
                is UpnpVideoId -> {
                    disposables.add(subscribeProgress(ref))
                }
            }

        }

        private fun subscribeProgress(videoId: UpnpVideoId): Disposable {
            return mDatabaseClient.upnpVideoChanges(videoId)
                    .startWith(UpnpVideoChange(videoId))
                    .flatMapMaybe {
                        mDatabaseClient.getLastPlaybackCompletion(it.videoId)
                                .subscribeOn(AppSchedulers.diskIo)
                    }
                    .subscribeIgnoreError(Consumer {
                        binding.completion = it
                    })
        }

    }
}

/**
 *
 */
class MediaItemClickListener
@Inject constructor(): OnItemViewClickedListener {

    //VerticalGridView sends null as last 2 params
    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder, item: Any,
                               rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        val context = itemViewHolder.view.findActivity()
        val mediaItem = item as MediaBrowser.MediaItem
        val mediaId = parseMediaId(mediaItem.mediaId)
        when (mediaId) {
            is UpnpDeviceId, is UpnpFolderId -> {
                val intent = Intent(context, FolderActivity::class.java)
                intent.putExtra(EXTRA_MEDIAID, mediaId.json)
                context.startActivity(intent)
            }
            is UpnpVideoId -> {
                val intent = Intent(context, DetailActivity::class.java)
                intent.putExtra(EXTRA_MEDIAID, mediaId.json)
                val bundle = if (itemViewHolder is MediaItemPresenter.ViewHolder) {
                    val view = itemViewHolder.view as MediaItemImageCardView
                    ActivityOptions.makeSceneTransitionAnimation(context,
                            view.mainImageView, SHARED_ELEMENT_NAME).toBundle()
                } else if (itemViewHolder is MediaItemListPresenter.ViewHolder) {
                    val binding = itemViewHolder.binding
                    ActivityOptions.makeSceneTransitionAnimation(context, binding.icon,
                            SHARED_ELEMENT_NAME).toBundle()
                } else {
                    Bundle.EMPTY
                }
                context.startActivity(intent, bundle)
            }
            else -> {
                Toast.makeText(context, "Unhandled ItemClick", Toast.LENGTH_LONG).show()
            }
        }
    }
}
