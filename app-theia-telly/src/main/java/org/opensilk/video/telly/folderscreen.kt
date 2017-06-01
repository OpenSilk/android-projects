package org.opensilk.video.telly

import android.content.Context
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.v17.leanback.app.VerticalGridFragment
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.TitleView
import android.support.v17.leanback.widget.VerticalGridPresenter
import dagger.*
import mortar.MortarScope
import org.opensilk.common.app.ScopedActivity
import org.opensilk.common.dagger.*
import org.opensilk.common.dagger2.withDaggerComponent
import org.opensilk.media._getMediaTitle
import javax.inject.Inject

/**
 * Created by drew on 5/28/17.
 */
@ActivityScope
@Subcomponent(modules = arrayOf(UpnpLoadersModule::class))
interface FolderComponent: Injector<FolderFragment> {
    @Subcomponent.Builder
    abstract class Builder: Injector.Builder<FolderFragment>() {
        @BindsInstance abstract fun mediaItem(mediaItem: MediaBrowser.MediaItem): Builder
    }
}

/**
 *
 */
@Module(subcomponents = arrayOf(FolderComponent::class))
class FolderModule

/**
 *
 */
class FolderActivity: BaseVideoActivity() {

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
    @Inject lateinit var mFoldersAdapter: FoldersAdapter

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        injectMe()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = mMediaItem._getMediaTitle()
        gridPresenter = VerticalGridPresenter()
        gridPresenter.numberOfColumns = 1

        adapter = mFoldersAdapter
        onItemViewClickedListener = MediaItemClickListener()

    }

}

/**
 *
 */
class FoldersAdapter @Inject constructor(presenter: MediaItemListPresenter) : ArrayObjectAdapter(presenter)