package org.opensilk.video.phone

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import org.opensilk.dagger2.ForApp
import org.opensilk.video.HomeViewModel
import org.opensilk.video.LiveDataObserver
import javax.inject.Inject

const val CODE_NEED_PERMS = 1020

@Module
abstract class HomeScreenModule {
    @ContributesAndroidInjector
    abstract fun homeFragment(): HomeFragment
}

class HomeActivity : DrawerActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(DRAWER_FRAGMENT_CONTAINER, HomeFragment())
                    .commit()
        }
    }

}

class HomeFragment: RecyclerFragment() {

    private lateinit var mViewModel: HomeViewModel
    @Inject lateinit var mAdapter: HomeAdapter

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = fetchViewModel(HomeViewModel::class)

        mViewModel.subscribeData(includeDocuments = true)

        mViewModel.pinnedContainers.observe(this, LiveDataObserver {
            mAdapter.swapList("pinned", it)
        })
        mViewModel.devices.observe(this, LiveDataObserver {
            mAdapter.swapList("devices", it)
        })
        mViewModel.recentlyPlayed.observe(this, LiveDataObserver {
            mAdapter.swapList("recent", it)
        })
        mViewModel.needPermissions.observe(this, LiveDataObserver {
            requestPermissions(it, CODE_NEED_PERMS)
        })
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.recycler.adapter = mAdapter
        mBinding.swipeRefresh.isEnabled = false
        mBinding.toolbar.title = getString(R.string.title_my_library)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            CODE_NEED_PERMS -> {
                mViewModel.onGrantedPermissions(permissions.filterIndexed { index, _ ->
                    grantResults[index] == PackageManager.PERMISSION_GRANTED
                })
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}

class HomeAdapter @Inject constructor(
        pinnedSection: PinnedSection,
        deviceSection: DeviceSection,
        recentSection: RecentSection
): SwappingSectionedAdapter() {
    init {
        addSection("pinned", pinnedSection)
        addSection("devices", deviceSection)
        addSection("recent", recentSection)
    }
}

class PinnedSection @Inject constructor(
        @ForApp context: Context
): SwappingSection(
        SectionParameters.Builder(R.layout.recycler_list_item)
                .headerResourceId(R.layout.recycler_header_item)
                .build(),
        HeaderItem(context.getString(R.string.header_pinned_items),
                R.drawable.ic_pin_48dp)
)

class DeviceSection @Inject constructor(
        @ForApp context: Context
): SwappingSection(
        SectionParameters.Builder(R.layout.recycler_list_item)
                .headerResourceId(R.layout.recycler_header_item)
                .build(),
        HeaderItem(context.getString(R.string.header_devices),
                R.drawable.ic_server_network_48dp)
)

class RecentSection @Inject constructor(
        @ForApp context: Context
): SwappingSection(
        SectionParameters.Builder(R.layout.recycler_list_item)
                .headerResourceId(R.layout.recycler_header_item)
                .build(),
        HeaderItem(context.getString(R.string.header_recently_played),
                R.drawable.ic_movie_48dp)
)
