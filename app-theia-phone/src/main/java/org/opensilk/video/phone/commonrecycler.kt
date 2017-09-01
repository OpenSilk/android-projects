package org.opensilk.video.phone

import android.arch.lifecycle.LifecycleFragment
import android.content.Intent
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.util.DiffUtil
import android.support.v7.util.ListUpdateCallback
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.github.luizgrp.sectionedrecyclerviewadapter.Section
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter
import io.reactivex.Single
import io.reactivex.disposables.Disposables
import org.opensilk.media.*
import org.opensilk.video.ACTION_RESUME
import org.opensilk.video.AppSchedulers
import org.opensilk.video.findActivity
import org.opensilk.video.phone.databinding.RecyclerBinding
import org.opensilk.video.phone.databinding.RecyclerHeaderItemBinding
import org.opensilk.video.phone.databinding.RecyclerListItemBinding
import timber.log.Timber
import javax.inject.Inject


open class RecyclerFragment: LifecycleFragment() {
    protected lateinit var mBinding: RecyclerBinding

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.recycler, container, false)
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

    protected fun drawerActivity() = activity as DrawerActivity

}

data class HeaderItem(val title: String, val icon: Int)

abstract class BoundViewHolder(root: View): RecyclerView.ViewHolder(root){
    abstract fun unbind()
}

class HeaderViewHolder(val binding: RecyclerHeaderItemBinding) : BoundViewHolder(binding.root) {

    fun bind(headerItem: HeaderItem) {
        binding.titleString = headerItem.title
    }

    override fun unbind() {
    }

}

class ListItemViewHolder(val binding: RecyclerListItemBinding): BoundViewHolder(binding.root),
        View.OnClickListener, View.OnLongClickListener {

    private lateinit var mediaRef: MediaRef

    fun bind(mediaRef: MediaRef) {
        this.mediaRef = mediaRef
        val desc = mediaRef.toMediaDescription()
        val imageResource = when (mediaRef) {
            is UpnpDeviceRef -> R.drawable.ic_lan_48dp
            is StorageDeviceRef -> if (!mediaRef.id.isPrimary)
                R.drawable.ic_usb_48dp else R.drawable.ic_folder_48dp
            is MediaDeviceRef,
            is FolderRef -> R.drawable.ic_folder_48dp
            is VideoRef -> R.drawable.ic_movie_48dp
            else -> R.drawable.ic_new_box_48dp
        }

        binding.frame.setOnClickListener(this)
        when (mediaRef) {
            is VideoRef -> binding.frame.setOnLongClickListener(this)
        }
        binding.titleString = desc.title.toString()
        binding.subTitleString = desc.subtitle?.toString() ?: ""
        if (!desc.iconUri.isEmpty()) {
            loadArtwork(desc.iconUri)
        } else {
            binding.artworkThumb.setImageResource(imageResource)
        }
    }

    override fun unbind() {
        Glide.with(binding.root.context).clear(binding.artworkThumb)
    }

    private fun loadArtwork(uri: Uri) {
        Glide.with(binding.root.context)
                .asDrawable()
                .apply(RequestOptions().centerCrop())
                .load(uri)
                .into(binding.artworkThumb)
    }

    override fun onClick(v: android.view.View) {
        val ref = mediaRef
        Timber.d("onClick($ref)")
        val activity = v.context.findActivity()
        if (activity is MediaRefClickListener && activity.onClick(ref)) {
            return
        }
        when (ref) {
            is MediaDeviceRef,
            is FolderRef -> {
                val intent = Intent(activity, FolderActivity::class.java)
                        .putMediaIdExtra(ref.id)
                activity.startActivity(intent)
            }
            is VideoRef -> {
                val intent = Intent(activity, PlaybackActivity::class.java)
                        .setAction(ACTION_RESUME)
                        .putMediaIdExtra(ref.id)
                activity.startActivity(intent)
            }
            else -> TODO()
        }
    }

    override fun onLongClick(v: android.view.View): kotlin.Boolean {
        val ref = mediaRef
        Timber.d("onLongClick($ref)")
        val activity = v.context.findActivity()
        if (activity is MediaRefClickListener && activity.onLongClick(ref)) {
            return true
        }
        when (ref) {
            is VideoRef -> {
                val intent = Intent(activity, DetailActivity::class.java)
                        .putMediaIdExtra(ref.id)
                activity.startActivity(intent)
            }
            else -> TODO()
        }
        return false
    }
}

interface MediaRefClickListener {
    fun onClick(mediaRef: MediaRef): Boolean
    fun onLongClick(mediaRef: MediaRef): Boolean
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
            DiffUtil.calculateDiff(object: DiffUtil.Callback() {
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
            ListItemViewHolder(DataBindingUtil.bind(view))

    override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ListItemViewHolder).bind(itemList[position])
    }

    override fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder =
            HeaderViewHolder(DataBindingUtil.bind(view))

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
        (holder as HeaderViewHolder).bind(headerItem)
    }

}
