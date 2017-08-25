package org.opensilk.video.telly

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.content.Context
import android.os.Bundle
import android.support.v17.leanback.app.VerticalGridSupportFragment
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.VerticalGridPresenter
import android.widget.Toast
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import org.opensilk.media.bundle
import org.opensilk.video.EXTRA_MEDIAID
import org.opensilk.video.FolderViewModel
import org.opensilk.video.LiveDataObserver
import javax.inject.Inject

/**
 *
 */
@Module
abstract class FolderScreenModule {
    @ContributesAndroidInjector
    abstract fun injector(): FolderFragment
}

/**
 *
 */
class FolderActivity: BaseVideoActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.folder_browse_fragment,
                            newFolderFragment(intent.getStringExtra(EXTRA_MEDIAID)), "folder_frag")
                    .commit()
        }
    }

}

fun newFolderFragment(mediaId: String): FolderFragment {
    val f = FolderFragment()
    f.arguments = bundle(EXTRA_MEDIAID, mediaId)
    return f
}

/**
 *
 */
class FolderFragment: VerticalGridSupportFragment(), LifecycleRegistryOwner {

    @Inject lateinit var mFolderAdapter: FolderAdapter
    @Inject lateinit var mFolderPresenter: FolderPresenter
    @Inject lateinit var mRefClickListener: MediaRefClickListener

    lateinit var mViewModel: FolderViewModel

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = fetchViewModel(FolderViewModel::class)
        mViewModel.onMediaId(arguments.getString(EXTRA_MEDIAID))
        mViewModel.mediaTitle.observe(this, LiveDataObserver {
            title = it
        })
        mViewModel.folderItems.observe(this, LiveDataObserver { items ->
            mFolderAdapter.clear()
            mFolderAdapter.addAll(0, items)
        })
        mViewModel.loadError.observe(this, LiveDataObserver {
            Toast.makeText(context, "An error occurred. msg=$it", Toast.LENGTH_LONG).show()
        })

        gridPresenter = mFolderPresenter
        gridPresenter.numberOfColumns = 1

        adapter = mFolderAdapter
        onItemViewClickedListener = mRefClickListener

    }

    private val lifecycleRegistry: LifecycleRegistry by lazy { LifecycleRegistry(this) }

    override fun getLifecycle(): LifecycleRegistry = lifecycleRegistry

}

/**
 *
 */
class FolderAdapter @Inject constructor(presenter: MediaRefListPresenter) : ArrayObjectAdapter(presenter)

/**
 *
 */
class FolderPresenter @Inject constructor(): VerticalGridPresenter()