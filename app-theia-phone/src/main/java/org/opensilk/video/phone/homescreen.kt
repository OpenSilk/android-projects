package org.opensilk.video.phone

import android.arch.lifecycle.ViewModel
import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.util.DiffUtil
import android.support.v7.util.ListUpdateCallback
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import io.github.luizgrp.sectionedrecyclerviewadapter.Section
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter
import org.opensilk.dagger2.ForApp
import org.opensilk.media.MediaDeviceId
import org.opensilk.media.MediaDeviceRef
import org.opensilk.media.UpnpDeviceRef
import org.opensilk.video.LiveDataObserver
import org.opensilk.video.ViewModelKey
import javax.inject.Inject

@Module(includes = arrayOf(HomeScreenHelperModule::class))
abstract class HomeScreenModule {
    @ContributesAndroidInjector
    abstract fun injector(): HomeActivity
    @Binds @IntoMap @ViewModelKey(HomeScreenViewModel::class)
    abstract fun viewModel(impl: HomeScreenViewModel): ViewModel
}

/**
 * Extra module required for @jvmStatic
 */
@Module
object HomeScreenHelperModule {
    @Provides @JvmStatic
    fun recyclerAdapter(): SectionedRecyclerViewAdapter = SectionedRecyclerViewAdapter()
}

class HomeActivity : DrawerActivity() {

    private lateinit var mViewModel: HomeScreenViewModel

    @Inject lateinit var mDeviceSection: DeviceSection
    @Inject lateinit var mAdapter: SectionedRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        mBinding.recycler.layoutManager = LinearLayoutManager(this)
        mBinding.recycler.adapter = mAdapter

        mBinding.toolbar.title = getString(R.string.title_my_library)

        mAdapter.addSection(mDeviceSection)

        mViewModel = fetchViewModel(HomeScreenViewModel::class)

        mViewModel.servers.observe(this, LiveDataObserver {
            handleNewServersList(mDeviceSection.devices, it)
        })

        mViewModel.fetchData()

    }

    private fun handleNewServersList(oldList: List<MediaDeviceRef>, newList: List<MediaDeviceRef>) {
        mDeviceSection.devices = newList
        DiffUtil.calculateDiff(object: DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                    oldList[oldItemPosition].id == newList[newItemPosition].id
            override fun getOldListSize(): Int = oldList.size
            override fun getNewListSize(): Int = newList.size
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                    oldList[oldItemPosition].meta == newList[newItemPosition].meta
        }).dispatchUpdatesTo(object: ListUpdateCallback {
            override fun onChanged(position: Int, count: Int, payload: Any?) {
                mAdapter.notifyItemRangeChangedInSection(mDeviceSection, position, count)
            }
            override fun onMoved(fromPosition: Int, toPosition: Int) {
                mAdapter.notifyItemMovedInSection(mDeviceSection, fromPosition, toPosition)
            }
            override fun onInserted(position: Int, count: Int) {
                mAdapter.notifyItemRangeInsertedInSection(mDeviceSection, position, count)
            }
            override fun onRemoved(position: Int, count: Int) {
                mAdapter.notifyItemRangeRemovedFromSection(mDeviceSection, position, count)
            }
        })
    }

}

class DeviceSection @Inject constructor(
        @ForApp context: Context
): Section(
        SectionParameters.Builder(R.layout.recycler_list_item)
                .headerResourceId(R.layout.recycler_header_item)
                .build()
) {

    private val serversHeader = HeaderItem(context.getString(R.string.header_devices),
            R.drawable.ic_server_network_48dp)

    var devices = emptyList<MediaDeviceRef>()

    override fun getContentItemsTotal(): Int = devices.size

    override fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder =
            HeaderViewHolder(DataBindingUtil.bind(view))

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
        (holder as HeaderViewHolder).bind(serversHeader)
    }

    override fun getItemViewHolder(view: View): RecyclerView.ViewHolder =
            ListItemViewHolder(DataBindingUtil.bind(view))

    override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ListItemViewHolder).bind(devices[position])
    }

}
