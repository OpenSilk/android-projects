package org.opensilk.video.telly

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.content.Context
import android.os.Bundle
import android.support.v17.leanback.app.BrowseFragment
import android.support.v17.leanback.app.BrowseSupportFragment
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.HeaderItem
import android.support.v17.leanback.widget.ListRow
import android.support.v17.leanback.widget.ListRowPresenter
import android.support.v4.content.ContextCompat
import android.view.View
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import org.opensilk.media.toMediaItem
import org.opensilk.video.HomeViewModel
import org.opensilk.video.LiveDataObserver
import javax.inject.Inject


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
class HomeFragment : BrowseSupportFragment(), LifecycleRegistryOwner {

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
        mViewModel.fetchData()

        val foldersHeader = HeaderItem("Media Servers")
        mHomeAdapter.add(ListRow(foldersHeader, mServersAdapter))
        val newlyAddedHeader = HeaderItem("Newly Added")
        mHomeAdapter.add(ListRow(newlyAddedHeader, mNewlyAddedAdapter))

        adapter = mHomeAdapter
        onItemViewClickedListener = mItemClickListener

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

    private val lifecycleRegistry: LifecycleRegistry by lazy {
        LifecycleRegistry(this)
    }

    override fun getLifecycle(): LifecycleRegistry = lifecycleRegistry
}

class HomeAdapter @Inject constructor(): ArrayObjectAdapter(ListRowPresenter())
class ServersAdapter @Inject constructor(presenter: MediaItemPresenter) : ArrayObjectAdapter(presenter)
class NewlyAddedAdapter @Inject constructor(presenter: MediaItemPresenter): ArrayObjectAdapter(presenter)
