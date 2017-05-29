package org.opensilk.video.telly

import android.os.Bundle
import android.support.v17.leanback.app.BrowseFragment
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.HeaderItem
import android.support.v17.leanback.widget.ListRow
import android.support.v17.leanback.widget.ListRowPresenter
import android.view.View
import dagger.Component
import dagger.Module
import mortar.MortarScope
import org.opensilk.common.app.ScopedActivity
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.dagger.getDaggerComponent
import org.opensilk.common.dagger2.withDaggerComponent
import javax.inject.Inject

/**
 * extra name
 */
const val EXTRA_MEDIAITEM = "org.opensilk.extra.mediaitem"

/**
 *
 */
@ActivityScope
@Component(
        dependencies = arrayOf(RootComponent::class),
        modules = arrayOf(HomeActivityModule::class)
)
interface HomeActivityComponent {
    fun inject(fragment: HomeFragment)
}

/**
 *
 */
@Module
class HomeActivityModule

/**
 *
 */
class HomeActivity : ScopedActivity() {

    override fun onCreateScope(builder: MortarScope.Builder) {
        builder.withDaggerComponent(DaggerHomeActivityComponent.builder()
                .rootComponent(rootComponent())
                .build())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
    }

}

/**
 *
 */
class HomeFragment : BrowseFragment() {

    @Inject lateinit var mServersAdapter: ServersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityComponent: HomeActivityComponent = context.getDaggerComponent()
        activityComponent.inject(this)

        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

        val foldersHeader = HeaderItem("Media Servers")
        rowsAdapter.add(ListRow(foldersHeader, mServersAdapter))
        //TODO load

        adapter = rowsAdapter
        //onItemViewClickedListener = MediaItemClickListener()
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Badge, when set, takes precedent over title
        title = getString(R.string.landing_title)
        headersState = BrowseFragment.HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        // set fastLane (or headers) background color
        brandColor = context.getColor(R.color.fastlane_background)
        // set search icon color
        searchAffordanceColor = context.getColor(R.color.search_opaque)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}

/**
 *
 */
@ActivityScope
class ServersAdapter @Inject constructor(presenter: MediaItemPresenter) : ArrayObjectAdapter(presenter)

