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
import org.opensilk.common.dagger.FragmentScope
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.injectMe
import org.opensilk.video.*
import rx.exceptions.Exceptions
import rx.subscriptions.CompositeSubscription
import javax.inject.Inject


/**
 *
 */
@FragmentScope
@Subcomponent
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

    @Inject lateinit var mHomeAdapter: HomeAdapter
    @Inject lateinit var mServersAdapter: ServersAdapter
    @Inject lateinit var mNewlyAddedAdapter: NewlyAddedAdapter
    @Inject lateinit var mItemClickListener: MediaItemClickListener

    lateinit var mViewModel: HomeViewModel

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        injectMe()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = fetchViewModel(HomeViewModel::class)
        mViewModel.servers.observe(this, LiveDataObserver {
            mServersAdapter.clear()
            mServersAdapter.addAll(0, it)
        })
        mViewModel.newlyAdded.observe(this, LiveDataObserver {
            mNewlyAddedAdapter.clear()
            mNewlyAddedAdapter.addAll(0, it)
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
        private val mServersLoader: UpnpDevicesLoader,
        private val mNewlyAddedLoader: NewlyAddedLoader
): ViewModel() {
    val servers = MutableLiveData<List<MediaBrowser.MediaItem>>()
    val newlyAdded = MutableLiveData<List<MediaBrowser.MediaItem>>()
    private val subscriptions = CompositeSubscription()

    fun fetchData() {
        subscribeServers()
        subscribeNewlyAdded()
    }

    fun subscribeServers() {
        val s = mServersLoader.observable
                .subscribe({
                    servers.postValue(it)
                }, {
                    Exceptions.propagate(it) //TODO handle errors
                }
        )
        subscriptions.add(s)
    }

    fun subscribeNewlyAdded() {
        val s = mNewlyAddedLoader.observable
                .subscribe({
                    newlyAdded.postValue(it)
                }, {
                    Exceptions.propagate(it)
                })
        subscriptions.add(s)
    }

    override fun onCleared() {
        super.onCleared()
        subscriptions.clear()
    }

}

class HomeAdapter @Inject constructor(): ArrayObjectAdapter(ListRowPresenter())
class ServersAdapter @Inject constructor(presenter: MediaItemPresenter) : ArrayObjectAdapter(presenter)
class NewlyAddedAdapter @Inject constructor(presenter: MediaItemPresenter): ArrayObjectAdapter(presenter)
