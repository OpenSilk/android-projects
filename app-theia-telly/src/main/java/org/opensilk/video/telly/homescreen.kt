package org.opensilk.video.telly

import android.arch.lifecycle.*
import android.content.Context
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.v17.leanback.app.BrowseFragment
import android.support.v17.leanback.app.BrowseSupportFragment
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.HeaderItem
import android.support.v17.leanback.widget.ListRow
import android.support.v17.leanback.widget.ListRowPresenter
import android.view.View
import dagger.Binds
import dagger.Module
import dagger.Subcomponent
import dagger.multibindings.IntoMap
import org.opensilk.common.dagger.*
import org.opensilk.common.rx.observeOnMainThread
import org.opensilk.media.MediaMeta
import org.opensilk.video.CDSDevicesLoader
import org.opensilk.video.DeviceRemovedException
import org.opensilk.video.UpnpLoadersModule
import org.opensilk.video.ViewModelKey
import rx.exceptions.Exceptions
import javax.inject.Inject

/**
 * extra name
 */
const val EXTRA_MEDIAITEM = "org.opensilk.extra.mediaitem"

/**
 *
 */
@ActivityScope
@Subcomponent(modules = arrayOf(UpnpLoadersModule::class))
interface HomeComponent: Injector<HomeFragment>{
    @Subcomponent.Builder
    abstract class Builder: Injector.Builder<HomeFragment>()
}

/**
 *
 */
@Module(subcomponents = arrayOf(HomeComponent::class))
abstract class HomeModule {
    //@Binds @IntoMap @ClassKey(HomeActivity::class)
    //abstract fun provideBuilderFactory(b: HomeComponent.Builder): Injector.Factory<*>
    @Binds @IntoMap @ViewModelKey(HomeViewModel::class)
    abstract fun homeViewModel(vm: HomeViewModel): ViewModel
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

    @Inject lateinit var mViewModelFactory: ViewModelProvider.Factory
    lateinit var mViewModel: HomeViewModel

    @Inject lateinit var mHomeAdapter: HomeAdapter
    @Inject lateinit var mServersAdapter: ServersAdapter


    @Inject lateinit var mItemClickListener: MediaItemClickListener

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        injectMe()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(HomeViewModel::class.java)

        val foldersHeader = HeaderItem("Media Servers")
        mHomeAdapter.add(ListRow(foldersHeader, mServersAdapter))

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
        brandColor = context.getColor(R.color.fastlane_background)
        // set search icon color
        searchAffordanceColor = context.getColor(R.color.search_opaque)
    }

    private val lifecycleRegistry: LifecycleRegistry by lazy {
        LifecycleRegistry(this)
    }

    override fun getLifecycle(): LifecycleRegistry {
        return lifecycleRegistry
    }
}

/**
 *
 */
class HomeViewModel
@Inject constructor(
        private val mServersLoader: CDSDevicesLoader
): ViewModel() {
    val servers: LiveData<List<MediaBrowser.MediaItem>> = MutableLiveData<List<MediaBrowser.MediaItem>>()
  private fun subscribeServers() {
        mServersLoader.observable
                .observeOnMainThread()
                .subscribe({
                    mServersAdapter.add(it)
                },
                {
                    if (it is DeviceRemovedException) {
                        mServersAdapter.clear()
                        subscribeServers() //recursive
                    } else {
                        Exceptions.propagate(it)
                    }
                }
        )
    }
}

/**
 *
 */
class HomeAdapter @Inject constructor(): ArrayObjectAdapter(ListRowPresenter())

/**
 *
 */
class ServersAdapter @Inject constructor(presenter: MediaItemPresenter) : ArrayObjectAdapter(presenter)

