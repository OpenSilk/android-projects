package org.opensilk.video.telly

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v17.leanback.app.BrowseFragment
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.HeaderItem
import android.support.v17.leanback.widget.ListRow
import android.support.v17.leanback.widget.ListRowPresenter
import android.view.View
import dagger.Binds
import dagger.Module
import dagger.Subcomponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import org.opensilk.common.app.ScopedActivity
import org.opensilk.common.dagger.*
import org.opensilk.common.lifecycle.terminateOnDestroy
import org.opensilk.common.rx.observeOnMainThread
import rx.exceptions.Exceptions
import timber.log.Timber
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
    @Binds @IntoMap @ClassKey(HomeActivity::class)
    abstract fun provideBuilderFactory(b: HomeComponent.Builder): Injector.Factory<*>
}

/**
 *
 */
class HomeActivity : ScopedActivity() {

    override val activityComponent: Any = DaggerServiceReference()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        startService(Intent(this, UpnpHolderService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, UpnpHolderService::class.java))
    }

}

/**
 *
 */
class HomeFragment : BrowseFragment() {

    @Inject lateinit var mServersAdapter: ServersAdapter
    @Inject lateinit var mServersLoader: CDSDevicesLoader

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        injectMe()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

        val foldersHeader = HeaderItem("Media Servers")
        rowsAdapter.add(ListRow(foldersHeader, mServersAdapter))
        subscribeServers()

        adapter = rowsAdapter
        //onItemViewClickedListener = MediaItemClickListener()
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

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun subscribeServers() {
        mServersLoader.observable
                .observeOnMainThread()
                .terminateOnDestroy(context)
                .subscribe({
                    Timber.d("Found Server")
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
@ActivityScope
class ServersAdapter @Inject constructor(presenter: MediaItemPresenter) : ArrayObjectAdapter(presenter)

