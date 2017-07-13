package org.opensilk.video.telly

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelStores
import android.content.Context
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.v17.leanback.app.VerticalGridFragment
import android.support.v17.leanback.app.VerticalGridSupportFragment
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.VerticalGridPresenter
import android.widget.Toast
import dagger.*
import org.opensilk.common.dagger.*
import org.opensilk.common.rx.observeOnMainThread
import org.opensilk.media._getMediaTitle
import org.opensilk.video.CDSBrowseLoader
import org.opensilk.video.NoBrowseResultsException
import org.opensilk.video.UpnpLoadersModule
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by drew on 5/28/17.
 */
@ActivityScope
@Subcomponent(modules = arrayOf(UpnpLoadersModule::class))
interface FolderComponent: Injector<FolderFragment> {
    @Subcomponent.Builder
    abstract class Builder: Injector.Builder<FolderFragment>() {
        override fun create(t: FolderFragment): Injector<FolderFragment> {
            val mediaItem: MediaBrowser.MediaItem = t.activity.intent.getParcelableExtra(EXTRA_MEDIAITEM)
            return mediaItem(mediaItem).build()
        }
        @BindsInstance abstract fun mediaItem(mediaItem: MediaBrowser.MediaItem): Builder
    }
}

/**
 *
 */
@Module(subcomponents = arrayOf(FolderComponent::class))
abstract class FolderModule

/**
 *
 */
class FolderActivity: BaseVideoActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.folder_browse_fragment, FolderFragment(), "folder_frag")
                    .commit()
        }
    }

}

class FolderViewModel: ViewModel() {

}

/**
 *
 */
class FolderFragment: VerticalGridSupportFragment() {

    @Inject lateinit var mMediaItem: MediaBrowser.MediaItem
    @Inject lateinit var mFoldersAdapter: FoldersAdapter
    @Inject lateinit var mBrowseLoader: CDSBrowseLoader
    @Inject lateinit var mItemClickListener: MediaItemClickListener

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
        onItemViewClickedListener = mItemClickListener

        subscribeBrowseItems()
    }

    fun subscribeBrowseItems() {
        mBrowseLoader.observable
                .onBackpressureBuffer()
                .observeOnMainThread()
                .subscribe({
                    mFoldersAdapter.add(it)
                }, {
                    if (it is NoBrowseResultsException) {
                        Toast.makeText(context, "This folder is empty", Toast.LENGTH_LONG).show()
                    } else {
                        Timber.e(it, "Loader error msg=${it.message}.")
                        Toast.makeText(context, "An error occurred. msg=${it.message}", Toast.LENGTH_LONG).show()
                    }
                })
    }

}

/**
 *
 */
class FoldersAdapter @Inject constructor(presenter: MediaItemListPresenter) : ArrayObjectAdapter(presenter)