package org.opensilk.video.phone

import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import dagger.Module
import dagger.Subcomponent
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.injectMe
import org.opensilk.dagger2.ForApp
import org.opensilk.media.MediaRef
import org.opensilk.media.UpnpDeviceRef
import org.opensilk.media.UpnpVideoRef
import org.opensilk.video.HomeViewModel
import org.opensilk.video.LiveDataObserver
import org.opensilk.video.phone.databinding.ActivityDrawerBinding
import javax.inject.Inject

@Module
abstract class HomeScreenModule {
    @ContributesAndroidInjector
    abstract fun injector(): HomeActivity
}

class HomeActivity : DrawerActivity() {

    private lateinit var mViewModel: HomeViewModel

    @Inject lateinit var mAdapter: HomeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        mBinding.recycler.layoutManager = LinearLayoutManager(this)
        mBinding.recycler.adapter = mAdapter

        mBinding.toolbar.title = getString(R.string.title_my_library)

        mViewModel = fetchViewModel(HomeViewModel::class)
        mViewModel.servers.observe(this, LiveDataObserver {
            mAdapter.setServers(it)
        })
        mViewModel.newlyAdded.observe(this, LiveDataObserver {
            mAdapter.setNewlyAdded(it)
        })
        mViewModel.fetchData()
    }

}

class HomeAdapter
@Inject constructor(
        @ForApp context: Context
): RecyclerView.Adapter<BoundViewHolder>() {

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
        }
        return newlyAdded[position - servers.size - 2]
    }

    override fun onBindViewHolder(holder: BoundViewHolder, position: Int) {
        if (isServersHeader(position)) {
            (holder as HeaderViewHolder).bind(serversHeader)
        } else if (isNewlyAddedHeader(position)) {
            (holder as HeaderViewHolder).bind(newlyAddedHeader)
        } else {
            (holder as ListItemViewHolder).bind(getItemAt(position))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoundViewHolder {
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

    override fun onViewRecycled(holder: BoundViewHolder?) {
        super.onViewRecycled(holder)
        holder?.unbind()
    }

}
