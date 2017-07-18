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
import org.opensilk.common.dagger.FragmentScope
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.injectMe
import org.opensilk.media.elseIfBlank
import org.opensilk.video.CDSBrowseLoader
import org.opensilk.video.NoBrowseResultsException
import org.opensilk.video.ViewModelKey
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

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
                    .replace(R.id.folder_browse_fragment, FolderFragment(), "folder_frag")
                    .commit()
        }
    }

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

        mViewModel.mediaTitle.observe(this, Observer { title = it })
        mViewModel.noBrowseResults.observe(this, Observer {
            Toast.makeText(context, "This folder is empty", Toast.LENGTH_LONG).show()
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
        private val mBrowseLoader: CDSBrowseLoader
) : ViewModel() {
    val mediaTitle = MutableLiveData<String>()
    val folderItems = MutableLiveData<List<MediaBrowser.MediaItem>>()
    val noBrowseResults = MutableLiveData<String>()
    val loadError = MutableLiveData<String>()
    val subscriptions = CompositeSubscription()
    var mediaId: String by Delegates.observable("", { _, oldValue, newValue ->
        if (newValue != "" && oldValue != newValue) {
            subscribeBrowseItems()
        }
    })

    fun subscribeBrowseItems() {
        val s = mBrowseLoader.observable(mediaId)
                .toList()
                .subscribe({
                    folderItems.postValue(it)
                }, {
                    if (it is NoBrowseResultsException) {
                        noBrowseResults.postValue("This folder is empty")
                    } else {
                        Timber.e(it, "Loader error msg=${it.message}.")
                        loadError.postValue(it.message.elseIfBlank("null"))
                    }
                })
        subscriptions.add(s)
    }

    override fun onCleared() {
        super.onCleared()
        subscriptions.clear()
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