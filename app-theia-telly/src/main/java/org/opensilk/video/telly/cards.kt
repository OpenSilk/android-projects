package org.opensilk.video.telly

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v17.leanback.widget.*
import android.support.v4.content.ContextCompat
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
import org.opensilk.media.*
import org.opensilk.media.database.MediaDAO
import org.opensilk.reactivex2.subscribeIgnoreError
import org.opensilk.video.AppSchedulers
import org.opensilk.video.findActivity
import org.opensilk.video.telly.databinding.MediaitemListCardBinding
import timber.log.Timber
import javax.inject.Inject

/**
 *
 */
class MediaDescImageCardView(context: Context): ImageCardView(context) {
    override fun setSelected(selected: Boolean) {
        updateBackgroundColor(selected)
        super.setSelected(selected)
    }
    fun updateBackgroundColor(selected: Boolean) {
        val color = ContextCompat.getColor(context,
                if (selected) R.color.selected_background else R.color.default_background)
        // Both background colors should be set because the view's background is temporarily visible
        // during animations.
        setBackgroundColor(color)
        setInfoAreaBackgroundColor(color)
    }
}

/**
 *
 */
class MediaRefPresenter
@Inject constructor() : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val cardView = MediaDescImageCardView(parent.context)

        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        cardView.updateBackgroundColor(false)

        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {

        val mediaRef = item as MediaRef
        val description = mediaRef.toMediaDescription()

        val cardView = viewHolder.view as MediaDescImageCardView
        val context = cardView.context
        cardView.titleText = description.title
        cardView.contentText = description.subtitle

        val resources = cardView.resources
        val cardWidth = resources.getDimensionPixelSize(R.dimen.card_width)
        val cardHeight = resources.getDimensionPixelSize(R.dimen.card_height)
        cardView.setMainImageDimensions(cardWidth, cardHeight)

        val iconResource = when (mediaRef) {
            is MediaDeviceRef,
            is FolderRef -> R.drawable.folder_48dp
            is VideoRef -> R.drawable.movie_48dp
            else -> R.drawable.file_48dp
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
        val cardView = viewHolder.view as MediaDescImageCardView
        // Remove references to images so that the garbage collector can free up memory
        cardView.badgeImage = null
        cardView.mainImage = null
    }

    class ViewHolder(view: MediaDescImageCardView): Presenter.ViewHolder(view)
}

/**
 *
 */
class MediaRefListPresenter
@Inject constructor(
        val mDatabaseClient: MediaDAO
) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: MediaitemListCardBinding = DataBindingUtil.inflate(inflater,
                R.layout.mediaitem_list_card, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        val vh = viewHolder as ViewHolder
        vh.bind(item as MediaRef)
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

        internal fun bind(mediaRef: MediaRef) {
            val description = mediaRef.toMediaDescription()

            //set description
            binding.description = description

            //load icon
            val iconResource = when (mediaRef) {
                is MediaDeviceRef,
                is FolderRef -> R.drawable.folder_48dp
                is VideoRef -> R.drawable.movie_48dp
                else -> R.drawable.file_48dp
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
            binding.completion = 0
            when (mediaRef) {
                is VideoRef -> {
                    disposables.add(getCompletionProgress(mediaRef.id))
                }
            }

        }

        private fun getCompletionProgress(videoId: VideoId): Disposable {
            return mDatabaseClient.getLastPlaybackCompletion(videoId)
                    .subscribeOn(AppSchedulers.diskIo)
                    .subscribeIgnoreError(Consumer {
                        binding.completion = it
                    })
        }

    }
}

/**
 *
 */
class MediaRefClickListener
@Inject constructor(): OnItemViewClickedListener {

    //VerticalGridView sends null as last 2 params
    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder, item: Any,
                               rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        val context = itemViewHolder.view.findActivity()
        val mediaRef = item as MediaRef
        when (mediaRef) {
            is MediaDeviceRef,
            is FolderRef -> {
                val intent = Intent(context, FolderActivity::class.java)
                intent.putMediaIdExtra(mediaRef.id)
                context.startActivity(intent)
            }
            is VideoRef -> {
                val intent = Intent(context, DetailActivity::class.java)
                intent.putMediaIdExtra(mediaRef.id)
                val bundle = when (itemViewHolder) {
                    is MediaRefPresenter.ViewHolder -> {
                        val view = itemViewHolder.view as MediaDescImageCardView
                        ActivityOptions.makeSceneTransitionAnimation(context,
                                view.mainImageView, SHARED_ELEMENT_NAME).toBundle()
                    }
                    is MediaRefListPresenter.ViewHolder -> {
                        val binding = itemViewHolder.binding
                        ActivityOptions.makeSceneTransitionAnimation(context, binding.icon,
                                SHARED_ELEMENT_NAME).toBundle()
                    }
                    else -> Bundle.EMPTY
                }
                context.startActivity(intent, bundle)
            }
            else -> {
                Timber.e("Unhandled ItemClick $mediaRef")
                Toast.makeText(context, "Unhandled ItemClick $mediaRef", Toast.LENGTH_LONG).show()
            }
        }
    }
}
