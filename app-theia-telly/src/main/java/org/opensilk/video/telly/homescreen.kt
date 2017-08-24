package org.opensilk.video.telly

import android.app.Activity
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.v17.leanback.app.BrowseFragment
import android.support.v17.leanback.app.BrowseSupportFragment
import android.support.v17.leanback.app.PermissionHelper
import android.support.v17.leanback.widget.*
import android.support.v4.content.ContextCompat
import android.view.View
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import org.opensilk.media.*
import org.opensilk.video.HomeViewModel
import org.opensilk.video.LiveDataObserver
import timber.log.Timber
import javax.inject.Inject

const val REQUEST_CODE_PERMS = 10302

/**
 *
 */
@Module
abstract class HomeScreenModule {
    @ContributesAndroidInjector
    abstract fun injector(): HomeFragment
}

/**
 *
 */
class HomeActivity : BaseVideoActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.home_browse_fragment, HomeFragment(), "home_frag")
                    .commit()
        }
    }

}

/**
 *
 */
class HomeFragment : BrowseSupportFragment(), LifecycleRegistryOwner, OnItemViewClickedListener {

    @Inject lateinit var mHomeAdapter: HomeAdapter
    @Inject lateinit var mServersAdapter: ServersAdapter
    @Inject lateinit var mNewlyAddedAdapter: NewlyAddedAdapter
    @Inject lateinit var mItemClickListener: MediaItemClickListener

    lateinit var mViewModel: HomeViewModel

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = fetchViewModel(HomeViewModel::class)
        mViewModel.servers.observe(this, LiveDataObserver { items ->
            mServersAdapter.clear()
            mServersAdapter.addAll(0, items.map { it.toMediaItem() })
        })
        mViewModel.newlyAdded.observe(this, LiveDataObserver { items ->
            mNewlyAddedAdapter.clear()
            mNewlyAddedAdapter.addAll(0, items.map { it.toMediaItem() })
        })
        mViewModel.needPermissions.observe(this, LiveDataObserver { perms ->
            //TODO handleRationale
            requestPermissions(perms, REQUEST_CODE_PERMS)
        })
        mViewModel.fetchData()

        val foldersHeader = HeaderItem("Media Servers")
        mHomeAdapter.add(ListRow(foldersHeader, mServersAdapter))
        val newlyAddedHeader = HeaderItem("Newly Added")
        mHomeAdapter.add(ListRow(newlyAddedHeader, mNewlyAddedAdapter))

        adapter = mHomeAdapter
        onItemViewClickedListener = this

    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Badge, when set, takes precedent over title
        title = getString(R.string.landing_title)
        headersState = BrowseFragment.HEADERS_DISABLED
        isHeadersTransitionOnBackEnabled = true
        // set fastLane (or headers) background color
        brandColor = ContextCompat.getColor(context, R.color.fastlane_background)
        // set search icon color
        searchAffordanceColor = ContextCompat.getColor(context, R.color.search_opaque)
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder, item: Any,
                               rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        val mediaItem = item as MediaBrowser.MediaItem
        val mediaId = parseMediaId(mediaItem.mediaId)
        when (mediaId) {
            is StorageDeviceId -> {
                val intent = mediaItem.description.extras.getParcelable<Intent>("intent")
                if (!mediaId.isPrimary && intent != EMPTY_INTENT) {
                    //startActivityForResult(intent, Activity.RESULT_FIRST_USER + 1)
                }
            }
            else -> mItemClickListener.onItemClicked(itemViewHolder, item,
                    rowViewHolder, row)
        }
        TODO("not implemented")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        when (requestCode - Activity.RESULT_FIRST_USER) {
            1 -> {
                Timber.e("intent=$data")
                Timber.d("uri=${data?.data}")
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults.isEmpty()) {
            return
        }
        when (requestCode) {
            REQUEST_CODE_PERMS -> {
                mViewModel.onGrantedPermissions(permissions.filterIndexed { index, _ ->
                    grantResults[index] == PackageManager.PERMISSION_GRANTED
                })
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private val lifecycleRegistry: LifecycleRegistry by lazy {
        LifecycleRegistry(this)
    }

    override fun getLifecycle(): LifecycleRegistry = lifecycleRegistry
}

class HomeAdapter @Inject constructor(): ArrayObjectAdapter(ListRowPresenter())
class ServersAdapter @Inject constructor(presenter: MediaItemPresenter) : ArrayObjectAdapter(presenter)
class NewlyAddedAdapter @Inject constructor(presenter: MediaItemPresenter): ArrayObjectAdapter(presenter)
