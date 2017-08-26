package org.opensilk.video.phone

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import org.opensilk.dagger2.ForApp
import org.opensilk.video.HomeViewModel
import org.opensilk.video.LiveDataObserver
import javax.inject.Inject

const val CODE_NEED_PERMS = 1020

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

        mViewModel.devices.observe(this, LiveDataObserver {
            mAdapter.swapList("devices", it)
        })
        mViewModel.needPermissions.observe(this, LiveDataObserver {
            if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(it, CODE_NEED_PERMS)
            } // else pass
        })

        mViewModel.subscribeData()

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
        deviceSection: DeviceSection
): SwappingSectionedAdapter() {
    init {
        addSection("devices", deviceSection)
    }
}

class DeviceSection @Inject constructor(
        @ForApp context: Context
): SwappingSection(
        SectionParameters.Builder(R.layout.recycler_list_item)
                .headerResourceId(R.layout.recycler_header_item)
                .build(),
        HeaderItem(context.getString(R.string.header_devices),
                R.drawable.ic_server_network_48dp)
)
