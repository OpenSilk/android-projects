package org.opensilk.video.telly

import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.v17.leanback.app.VerticalGridFragment
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.TitleView
import android.support.v17.leanback.widget.VerticalGridPresenter
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import mortar.MortarScope
import org.opensilk.common.app.ScopedActivity
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.dagger.ScreenScope
import org.opensilk.common.dagger.getDaggerComponent
import org.opensilk.common.dagger2.withDaggerComponent
import org.opensilk.media._getMediaTitle
import javax.inject.Inject

/**
 * Created by drew on 5/28/17.
 */
@ActivityScope
@Component(
        dependencies = arrayOf(RootComponent::class),
        modules = arrayOf(FolderActivityModule::class)
)
interface FolderActivityComponent {
    fun inject(fragment: FolderFragment)
}

/**
 *
 */
@Module
class FolderActivityModule(val mMediaItem: MediaBrowser.MediaItem) {
    @Provides
    fun provideMediaItem() : MediaBrowser.MediaItem {
        return mMediaItem
    }
}

/**
 *
 */
class FolderActivity: ScopedActivity() {

    override fun onCreateScope(builder: MortarScope.Builder) {
        val mediaItem: MediaBrowser.MediaItem = intent.getParcelableExtra(EXTRA_MEDIAITEM)
        builder.withDaggerComponent(DaggerFolderActivityComponent.builder()
                .rootComponent(rootComponent())
                .folderActivityModule(FolderActivityModule(mediaItem))
                .build())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder)
    }

}

/**
 *
 */
class FolderFragment: VerticalGridFragment() {

    @Inject lateinit var mMediaItem: MediaBrowser.MediaItem
    @Inject lateinit var mPresenter: MediaItemListPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityComponent: FolderActivityComponent = context.getDaggerComponent()
        activityComponent.inject(this)

        title = mMediaItem._getMediaTitle()
        gridPresenter = VerticalGridPresenter()
        gridPresenter.numberOfColumns = 1

        adapter = ArrayObjectAdapter(mPresenter)
        onItemViewClickedListener = MediaItemClickListener()

    }

}