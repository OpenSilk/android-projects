package org.opensilk.video.telly

import android.arch.lifecycle.*
import android.content.Context
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.v17.leanback.app.VerticalGridSupportFragment
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.VerticalGridPresenter
import android.widget.Toast
import dagger.Binds
import dagger.Module
import dagger.Subcomponent
import dagger.multibindings.IntoMap
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import org.opensilk.common.dagger.FragmentScope
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.injectMe
import org.opensilk.media.*
import org.opensilk.video.NoBrowseResultsException
import org.opensilk.video.UpnpFoldersLoader
import org.opensilk.video.ViewModelKey
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by drew on 5/28/17.
 */
@FragmentScope
@Subcomponent
interface FolderComponent: Injector<FolderFragment> {
    @Subcomponent.Builder
    abstract class Builder: Injector.Builder<FolderFragment>()
}

/**
 *
 */
@Module(subcomponents = arrayOf(FolderComponent::class))
abstract class FolderModule {
    @Binds @IntoMap @ViewModelKey(FolderViewModel::class)
    abstract fun folderViewModel(vm: FolderViewModel): ViewModel
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
    @Inject lateinit var mItemClickListener: MediaItemClickListener

    lateinit var mViewModel: FolderViewModel

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        injectMe()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = fetchViewModel(FolderViewModel::class)
        mViewModel.onMediaId(arguments.getString(EXTRA_MEDIAID))
        mViewModel.mediaTitle.observe(this, Observer {
            title = it
        })
        mViewModel.folderItems.observe(this, Observer {
            mFolderAdapter.clear()
            mFolderAdapter.addAll(0, it)
        })
        mViewModel.loadError.observe(this, Observer {
            Toast.makeText(context, "An error occurred. msg=$it", Toast.LENGTH_LONG).show()
        })

        gridPresenter = mFolderPresenter
        gridPresenter.numberOfColumns = 1

        adapter = mFolderAdapter
        onItemViewClickedListener = mItemClickListener

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
class FolderViewModel
@Inject constructor(
        private val mDatabaseClient: MediaProviderClient,
        private val mBrowseLoader: UpnpFoldersLoader
) : ViewModel() {
    val mediaTitle = MutableLiveData<String>()
    val folderItems = MutableLiveData<List<MediaBrowser.MediaItem>>()
    val loadError = MutableLiveData<String>()
    private val disposables = CompositeDisposable()

    fun onMediaId(mediaId: String) {
        val mediaRef = parseMediaId(mediaId)
        when (mediaRef) {
            is UpnpFolderId -> {
                subscribeBrowseItems(mediaRef)
                subscribeTitle(mediaRef)
            }
        }
    }

    fun subscribeBrowseItems(mediaId: UpnpFolderId) {
        val s = mBrowseLoader.observable(mediaId)
                .map { list -> list.map { it.toMediaItem() } }
                .subscribe({
                    folderItems.postValue(it)
                }, {
                    Timber.e(it, "Loader error msg=${it.message}.")
                    loadError.postValue(it.message.elseIfBlank("null"))
                })
        disposables.add(s)
    }

    fun subscribeTitle(mediaId: UpnpFolderId) {
        val s = mDatabaseClient.getMediaMeta(mediaId)
                .map({ it.toMediaItem() })
                .subscribe(Consumer {
                    mediaTitle.postValue(it.description.title?.toString())
                })
        disposables.add(s)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}

/**
 *
 */
class FolderAdapter @Inject constructor(presenter: MediaItemListPresenter) : ArrayObjectAdapter(presenter)

/**
 *
 */
class FolderPresenter @Inject constructor(): VerticalGridPresenter()