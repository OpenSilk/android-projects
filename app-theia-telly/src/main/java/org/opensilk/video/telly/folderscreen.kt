package org.opensilk.video.telly

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v17.leanback.app.VerticalGridSupportFragment
import android.support.v17.leanback.widget.*
import android.widget.Toast
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import org.opensilk.media.*
import org.opensilk.video.FolderViewModel
import org.opensilk.video.LiveDataObserver
import javax.inject.Inject

/**
 *
 */
@Module
abstract class FolderScreenModule {
    @ContributesAndroidInjector
    abstract fun folderActivity(): FolderActivity
    @ContributesAndroidInjector
    abstract fun folderFragment(): FolderFragment
}

/**
 *
 */
class FolderActivity: BaseVideoActivity(), OnItemViewClickedListener {

    @Inject lateinit var mDefaultClickListener: MediaRefClickListener

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.folder_browse_fragment, newFolderFragment(intent.getMediaIdExtra()))
                    .commit()
        }
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder, item: Any,
                               rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        val mediaRef = item as MediaRef
        when (mediaRef) {
            is MediaDeviceRef,
            is FolderRef -> {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.folder_browse_fragment, newFolderFragment(mediaRef.id))
                        .addToBackStack(null)
                        .commit()
            }
            else -> mDefaultClickListener.onItemClicked(itemViewHolder, item, rowViewHolder, row)
        }
    }
}

fun newFolderFragment(mediaId: MediaId): FolderFragment {
    val f = FolderFragment()
    f.arguments = mediaId.asBundle()
    return f
}

/**
 *
 */
class FolderFragment: VerticalGridSupportFragment(), LifecycleRegistryOwner {

    @Inject lateinit var mFolderAdapter: FolderAdapter
    @Inject lateinit var mFolderPresenter: FolderPresenter

    lateinit var mViewModel: FolderViewModel

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = fetchViewModel(FolderViewModel::class)
        mViewModel.onMediaId(arguments.getMediaId())
        mViewModel.mediaTitle.observe(this, LiveDataObserver {
            title = it
        })
        mViewModel.folderItems.observe(this, LiveDataObserver { items ->
            mFolderAdapter.swapList(items)
        })
        mViewModel.loadError.observe(this, LiveDataObserver {
            Toast.makeText(context, "An error occurred. msg=$it", Toast.LENGTH_LONG).show()
        })

        gridPresenter = mFolderPresenter
        gridPresenter.numberOfColumns = 1

        adapter = mFolderAdapter
        onItemViewClickedListener = activity as OnItemViewClickedListener

    }

    private val lifecycleRegistry: LifecycleRegistry by lazy { LifecycleRegistry(this) }

    override fun getLifecycle(): LifecycleRegistry = lifecycleRegistry

}

/**
 *
 */
class FolderAdapter @Inject constructor(presenter: MediaRefListPresenter) : SwappingObjectAdapter(presenter)

/**
 *
 */
class FolderPresenter @Inject constructor(): VerticalGridPresenter()