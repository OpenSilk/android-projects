package org.opensilk.video.phone

import android.content.Context
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dagger.Module
import dagger.Subcomponent
import org.opensilk.common.dagger.ForApplication
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.injectMe
import org.opensilk.media.*
import org.opensilk.video.HomeViewModel
import org.opensilk.video.LiveDataObserver
import org.opensilk.video.phone.databinding.ActivityDrawerBinding
import org.opensilk.video.phone.databinding.RecyclerHeaderItemBinding
import org.opensilk.video.phone.databinding.RecyclerListItemBinding
import javax.inject.Inject

@Subcomponent
interface HomeScreenComponent: Injector<HomeActivity> {
    @Subcomponent.Builder
    abstract class Builder: Injector.Builder<HomeActivity>()
}

@Module(subcomponents = arrayOf(HomeScreenComponent::class))
abstract class HomeScreenModule

class HomeActivity : BaseVideoActivity() {

    private lateinit var mBinding: ActivityDrawerBinding
    private lateinit var mViewModel: HomeViewModel

    @Inject lateinit var mAdapter: HomeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        injectMe()
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_drawer)
        mBinding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        mBinding.recycler.layoutManager = LinearLayoutManager(this)
        mBinding.recycler.adapter = mAdapter

        mViewModel = fetchViewModel(HomeViewModel::class)
        mViewModel.servers.observe(this, LiveDataObserver {
            mAdapter.setServers(it)
        })
        mViewModel.newlyAdded.observe(this, LiveDataObserver {
            mAdapter.setNewlyAdded(it)
        })
        mViewModel.fetchData()
    }

    override fun onDestroy() {
        super.onDestroy()
        mBinding.unbind()
    }

}

data class HeaderItem(val title: String, val icon: Int)

class HomeAdapter
@Inject constructor(
        @ForApplication context: Context
): RecyclerView.Adapter<HomeAdapter.ViewHolder>() {

    private val serversHeader = HeaderItem(context.getString(R.string.header_devices),
            R.drawable.ic_server_network_48dp)
    private val servers = ArrayList<UpnpDeviceRef>()
    private val newlyAddedHeader = HeaderItem(context.getString(R.string.header_newly_added),
            R.drawable.ic_new_box_48dp)
    private val newlyAdded = ArrayList<UpnpVideoRef>()

    fun setServers(list: List<UpnpDeviceRef>) {
        servers.clear()
        servers.addAll(list)
        notifyDataSetChanged()
    }

    fun setNewlyAdded(list: List<UpnpVideoRef>) {
        newlyAdded.clear()
        newlyAdded.addAll(list)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (isServersHeader(position)) {
            (holder as HeaderViewHolder).bind(serversHeader)
        } else if (isNewlyAddedHeader(position)) {
            (holder as HeaderViewHolder).bind(newlyAddedHeader)
        } else {
            (holder as ListItemViewHolder).bind(getItemAt(position))
        }
    }

    override fun onViewRecycled(holder: ViewHolder?) {
        super.onViewRecycled(holder)
        holder?.unbind()
    }

    override fun getItemCount(): Int {
        return 2 + servers.size + newlyAdded.size
    }

    override fun getItemViewType(position: Int): Int {
        if (isServersHeader(position) || isNewlyAddedHeader(position)) {
            return R.layout.recycler_header_item
        } else {
            return R.layout.recycler_list_item
        }
    }

    private fun isServersHeader(position: Int): Boolean {
        return position == 0
    }

    private fun isNewlyAddedHeader(position: Int): Boolean {
        return (servers.size + 1) == position
    }

    private fun getItemAt(position: Int): MediaRef {
        if ((position - 1) < servers.size) {
            return servers[position - 1]
        } else {
            return newlyAdded[position - (servers.size - 1)]
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflator = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.recycler_header_item -> {
                HeaderViewHolder(DataBindingUtil.inflate(inflator, viewType, parent, false))
            }
            R.layout.recycler_list_item -> {
                ListItemViewHolder(DataBindingUtil.inflate(inflator, viewType, parent, false))
            }
            else -> TODO("Unhandled viewType")
        }
    }

    abstract class ViewHolder(root: View): RecyclerView.ViewHolder(root) {
        abstract fun unbind()
    }

    class HeaderViewHolder(val binding: RecyclerHeaderItemBinding) : ViewHolder(binding.root) {

        fun bind(headerItem: HeaderItem) {
            binding.titleString = headerItem.title
        }

        override fun unbind() {
        }

    }

    class ListItemViewHolder(val binding: RecyclerListItemBinding): ViewHolder(binding.root),
            View.OnClickListener, View.OnLongClickListener{

        private lateinit var mediaRef: MediaRef

        fun bind(mediaRef: MediaRef) {
            this.mediaRef = mediaRef
            binding.frame.setOnClickListener(this)
            //binding.frame.setOnLongClickListener(this)
            when (mediaRef) {
                is UpnpDeviceRef -> {
                    binding.titleString = mediaRef.meta.title
                    if (mediaRef.meta.artworkUri.isEmpty()) {
                        binding.avatar.setImageResource(R.drawable.ic_lan_48dp)
                    } else {
                        loadArtwork(mediaRef.meta.artworkUri)
                    }
                }
                is UpnpVideoRef -> {
                    binding.titleString = mediaRef.meta.title
                    if (mediaRef.meta.artworkUri.isEmpty()) {
                        binding.avatar.setImageResource(R.drawable.ic_movie_48dp)
                    } else {
                        loadArtwork(mediaRef.meta.artworkUri)
                    }
                }
                else -> TODO("Unhandled mediaRef")
            }
        }

        override fun unbind() {
            Glide.with(binding.root.context).clear(binding.avatar)
        }

        private fun loadArtwork(uri: Uri) {
            Glide.with(binding.root.context)
                    .asDrawable()
                    .apply(RequestOptions().centerCrop())
                    .load(uri)
                    .into(binding.avatar)
        }

        override fun onClick(v: android.view.View) {
            when (this.mediaRef) {
                is UpnpDeviceRef, is UpnpFolderRef -> {

                }
                is UpnpVideoRef -> {

                }
                else -> TODO()
            }
        }

        override fun onLongClick(v: android.view.View?): kotlin.Boolean {
            TODO("not implemented")
        }
    }

}
