package org.opensilk.music.ui

import android.content.Intent
import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.net.Uri
import android.os.Bundle
import android.support.v7.util.DiffUtil
import android.support.v7.util.ListUpdateCallback
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import io.github.luizgrp.sectionedrecyclerviewadapter.Section
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter
import io.reactivex.Single
import io.reactivex.disposables.Disposables
import org.opensilk.common.glide.*
import org.opensilk.common.widget.LetterTileDrawable
import org.opensilk.media.*
import org.opensilk.music.AppSchedulers
import org.opensilk.music.R
import org.opensilk.music.databinding.RecyclerBinding
import org.opensilk.music.databinding.RecyclerItemGridArtworkBinding
import org.opensilk.music.databinding.RecyclerItemListArtworkBinding
import org.opensilk.music.databinding.RecyclerItemListHeaderBinding
import org.opensilk.music.findActivity
import timber.log.Timber
import javax.inject.Inject

open class RecyclerFragment: BaseMusicFragment() {
    protected lateinit var mBinding: RecyclerBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mBinding = RecyclerBinding.inflate(inflater, container, false)
        mBinding.recycler.setHasFixedSize(true)
        return mBinding.root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated()")
        drawerActivity().setToolbar(mBinding.toolbar)
    }

    override fun onDestroyView() {
        Timber.d("onDestroyView()")
        super.onDestroyView()
        drawerActivity().clearToolbar(mBinding.toolbar)
        mBinding.swipeRefresh.isRefreshing = false
        //this prevents ghosting when popping the backstack while
        //the loading indicator is showing
        mBinding.swipeRefresh.removeAllViews()
        mBinding.unbind()
    }

    protected fun drawerActivity() = activity as DrawerSlidingActivity

}

data class HeaderItem(val title: String, val icon: Int)

interface MediaRefClickListener {
    fun onClick(mediaRef: MediaRef): Boolean
    fun onLongClick(mediaRef: MediaRef): Boolean
}

class GridArtworkVH(
        override val binding: RecyclerItemGridArtworkBinding
) : MediaItemVH(binding) {

    override val iconView: ImageView? = binding.artworkThumb

    override fun loadIconUri(iconUri: Uri) {
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
        loadIconUri(target, iconUri)
    }

}

class ListArtworkVH(
        override val binding: RecyclerItemListArtworkBinding
) : MediaItemVH(binding) {

    override val iconView: ImageView? = binding.artworkThumb

    override val isIconViewCircular: Boolean = true

}

abstract class BoundViewHolder(root: View): RecyclerView.ViewHolder(root){
    abstract fun onUnbind()
}

open class MediaItemVH(
        open val binding: ViewDataBinding
) : BoundViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener {

    open val iconView: ImageView? = null

    open protected val isIconViewCircular: Boolean = false

    protected var mediaRef: MediaRef = NoMediaRef

    open fun onBind(mediaRef: MediaRef) {
        onUnbind()
        this.mediaRef = mediaRef
        val desc = mediaRef.toMediaDescription()
        binding.setVariable(org.opensilk.music.BR.item, desc)
        val imageResource = when (mediaRef) {
            is UpnpDeviceRef -> R.drawable.ic_lan_48dp
            is StorageDeviceRef -> if (!mediaRef.id.isPrimary)
                R.drawable.ic_usb_48dp else R.drawable.ic_folder_48dp
            is MediaDeviceRef,
            is FolderRef -> R.drawable.ic_folder_48dp
            else -> R.drawable.ic_folder_48dp
        }
        iconView?.let { artwork ->
            if (!desc.iconUri.isEmpty()) {
                loadIconUri(desc.iconUri)
            } else {
                artwork.setImageResource(imageResource)
            }
        }
        binding.root.setOnClickListener(this)
    }

    override fun onUnbind() {
        binding.unbind()
        iconView?.let { icon ->
            Glide.with(icon.context).clear(icon)
            icon.setImageBitmap(null)
        }
    }

    override fun onClick(v: View) {
        val ref = mediaRef
        Timber.d("onClick($ref)")
        val activity = v.findActivity()
        if (activity is MediaRefClickListener && activity.onClick(ref)) {
            return
        }
        when (ref) {
            is MediaDeviceRef,
            is FolderRef -> {
                val intent = Intent(activity, FolderSlidingActivity::class.java)
                        .putMediaIdExtra(ref.id)
                activity.startActivity(intent)
            }
            else -> TODO()
        }
    }

    override fun onLongClick(v: View): kotlin.Boolean {
        val ref = mediaRef
        Timber.d("onLongClick($ref)")
        val activity = v.findActivity()
        if (activity is MediaRefClickListener && activity.onLongClick(ref)) {
            return true
        }
        when (ref) {
            is VideoRef -> {
                val intent = Intent(activity, DetailSlidingActivity::class.java)
                        .putMediaIdExtra(ref.id)
                activity.startActivity(intent)
            }
            else -> TODO()
        }
        return false
    }

    open fun loadIconUri(iconUri: Uri) {
        loadIconUri(PalettableImageViewTarget.builder().into(iconView).build(), iconUri)
    }

    internal fun loadIconUri(target: PalettableImageViewTarget, iconUri: Uri) {
        val context = target.view.context
        val opts = RequestOptions()
        if (isIconViewCircular) {
            opts.circleCrop()
        } else {
            opts.centerCrop()
        }
        Glide.with(context)
                .`as`(PalettizedBitmapDrawable::class.java)
                .apply(opts)
                .transition(DrawableTransitionOptions.withCrossFade())
                .load(iconUri)
                .into(target)
    }

    internal fun setLetterDrawableAsArtwork(imageView: ImageView, text: String) {
        val resources = imageView.resources
        val drawable = LetterTileDrawable.fromText(resources, text)
        drawable.setIsCircular(isIconViewCircular)
        imageView.setImageDrawable(drawable)
    }

}

class HeaderVH(val binding: RecyclerItemListHeaderBinding) : BoundViewHolder(binding.root) {

    fun bind(headerItem: HeaderItem) {
        binding.header = headerItem
        binding.executePendingBindings()
    }

    override fun onUnbind() {
    }

}

class SwappingAdapter @Inject constructor(): RecyclerView.Adapter<MediaItemVH>() {

    private var mList = emptyList<MediaRef>()
    private var mDisposable = Disposables.disposed()

    fun swapList(newList: List<MediaRef>) {
        handleSwap(ArrayList(mList), newList)
    }

    private fun handleSwap(oldList: List<MediaRef>, newList: List<MediaRef>) {
        mDisposable.dispose()
        if (oldList.isEmpty()) {
            mList = newList
            notifyItemRangeInserted(0, newList.size)
            return
        }
        if (newList.isEmpty()) {
            mList = emptyList()
            notifyItemRangeRemoved(0, oldList.size)
            return
        }
        mDisposable = Single.fromCallable {
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                        oldList[oldItemPosition].id == newList[newItemPosition].id
                override fun getOldListSize(): Int = oldList.size
                override fun getNewListSize(): Int = newList.size
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                        oldList[oldItemPosition] == newList[newItemPosition]
            })
        }.subscribeOn(AppSchedulers.background).observeOn(AppSchedulers.main).subscribe { result ->
            mList = newList
            result.dispatchUpdatesTo(this)
        }
    }

    override fun onBindViewHolder(holder: MediaItemVH, position: Int) {
        holder.onBind(mList[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemVH = when (viewType) {
        R.layout.recycler_item_list_artwork ->
            ListArtworkVH(RecyclerItemListArtworkBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        else -> TODO()
    }

    override fun getItemCount(): Int = mList.size

    override fun getItemViewType(position: Int): Int {
        return R.layout.recycler_item_list_artwork
    }
}

open class SwappingSectionedAdapter @Inject constructor(): SectionedRecyclerViewAdapter() {

    private var mDisposable = Disposables.disposed()

    fun swapList(tag: String, itemList: List<MediaRef>) {
        val section = getSection(tag) as SwappingSection
        handleSwap(section, ArrayList(section.itemList), ArrayList(itemList))
    }

    private fun handleSwap(section: SwappingSection, oldList: List<MediaRef>, newList: List<MediaRef>) {
        mDisposable.dispose()
        if (oldList.isEmpty()) {
            section.itemList = newList
            notifyItemRangeInsertedInSection(section, 0, newList.size)
            return
        }
        if (newList.isEmpty()) {
            section.itemList = emptyList()
            notifyItemRangeRemovedFromSection(section, 0, oldList.size)
            return
        }
        mDisposable = Single.fromCallable {
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                        oldList[oldItemPosition].id == newList[newItemPosition].id

                override fun getOldListSize(): Int = oldList.size
                override fun getNewListSize(): Int = newList.size
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                        oldList[oldItemPosition] == newList[newItemPosition]
            })
        }.subscribeOn(AppSchedulers.background).observeOn(AppSchedulers.main).subscribe { result ->
            section.itemList = newList
            result.dispatchUpdatesTo(object: ListUpdateCallback {
                override fun onChanged(position: Int, count: Int, payload: Any?) {
                    notifyItemRangeChangedInSection(section, position, count)
                }
                override fun onMoved(fromPosition: Int, toPosition: Int) {
                    notifyItemMovedInSection(section, fromPosition, toPosition)
                }
                override fun onInserted(position: Int, count: Int) {
                    notifyItemRangeInsertedInSection(section, position, count)
                }
                override fun onRemoved(position: Int, count: Int) {
                    notifyItemRangeRemovedFromSection(section, position, count)
                }
            })
        }
    }
}

abstract class SwappingSection(
        params: SectionParameters,
        private val headerItem: HeaderItem
): Section(params) {

    var itemList: List<MediaRef> = emptyList()

    override fun getContentItemsTotal(): Int = itemList.size

    override fun getItemViewHolder(view: View): RecyclerView.ViewHolder =
            ListArtworkVH(DataBindingUtil.bind(view))

    override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ListArtworkVH).onBind(itemList[position])
    }

    override fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder =
            HeaderVH(DataBindingUtil.bind(view))

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
        (holder as HeaderVH).bind(headerItem)
    }

}