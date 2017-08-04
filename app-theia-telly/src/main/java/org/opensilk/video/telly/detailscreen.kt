package org.opensilk.video.telly

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v17.leanback.app.BackgroundManager
import android.support.v17.leanback.app.DetailsSupportFragment
import android.support.v17.leanback.widget.*
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.multibindings.IntoMap
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import org.opensilk.common.dagger.ForApplication
import org.opensilk.common.dagger.FragmentScope
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.injectMe
import org.opensilk.common.rx.subscribeIgnoreError
import org.opensilk.media.*
import org.opensilk.video.*
import org.opensilk.video.telly.databinding.DetailsFileInfoRowBinding
import java.lang.ref.WeakReference
import java.util.*
import javax.inject.Inject
import kotlin.properties.Delegates

/*
 ids for detail actions
 */
private const val POS_RESUME = 0
private const val POS_PLAY = 2
private const val POS_RESTART = POS_PLAY //restart replaces play

private const val ACTIONID_RESUME = 100L
private const val ACTIONID_START_OVER = 101L
//
//
private const val ACTIONID_PLAY = 200L
//
//
private const val ACTIONID_GET_DESCRIPTION = 300L
private const val ACTIONID_REMOVE_DESCRIPTION = 301L

const val SHARED_ELEMENT_NAME = "hero"


/**
 * Created by drew on 6/2/17.
 */
@FragmentScope
@Subcomponent(modules = arrayOf(DetailPresenterModule::class))
interface DetailComponent: Injector<DetailFragment> {
    @Subcomponent.Builder
    abstract class Builder: Injector.Builder<DetailFragment>()
}

/**
 *
 */
@Module(subcomponents = arrayOf(DetailComponent::class))
abstract class DetailModule {
    @Binds @IntoMap @ViewModelKey(DetailViewModel::class)
    abstract fun detailViewModel(vm: DetailViewModel): ViewModel
}

/**
 * this is fuckin weird but works, kotlin has no static methods so we put them
 * in the companion object but the compiler complains if no @Module annotation on it so
 * we add that too. this will probably break in the future, if that happens it has to be added
 * to the builder.
 */
@Module
abstract class DetailPresenterModule {
    @Module
    companion object {
        @Provides @JvmStatic
        fun provideDescriptionPresenter(descPresenter: DetailOverviewPresenter): FullWidthDetailsOverviewRowPresenter {
            return FullWidthDetailsOverviewRowPresenter(descPresenter)
        }
        @Provides @JvmStatic
        fun provideDetailsRow(): DetailsOverviewRow {
            return DetailsOverviewRow(VideoDescInfo())
        }
    }
}

/**
 *
 */
class DetailActivity: BaseVideoActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.detail_fragment,
                            newDetailFragment(intent.getStringExtra(EXTRA_MEDIAID)), "detail_frag")
                    .commit()
        }
        BackgroundManager.getInstance(this).attach(window)
    }
}

fun newDetailFragment(mediaId: String): DetailFragment {
    val f = DetailFragment()
    f.arguments = bundle(EXTRA_MEDIAID, mediaId)
    return f
}

/**
 *
 */
class DetailFragment: DetailsSupportFragment(), LifecycleRegistryOwner, OnActionClickedListener {

    @Inject lateinit var mOverviewPresenter: FullWidthDetailsOverviewRowPresenter
    @Inject lateinit var mOverviewRow: DetailsOverviewRow
    @Inject lateinit var mOverviewActionsAdapter: DetailOverviewActionsAdapter
    @Inject lateinit var mFileInfoPresenter: FileInfoRowPresenter
    @Inject lateinit var mFileInfoRow: FileInfoRow

    lateinit var mBackgroundManager: BackgroundManager
    lateinit var mViewModel: DetailViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        injectMe()
        mBackgroundManager = BackgroundManager.getInstance(activity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = fetchViewModel(DetailViewModel::class)
        mViewModel.onMediaId(arguments.getString(EXTRA_MEDIAID))
        mViewModel.videoDescription.observe(this, LiveDataObserver {
            mOverviewRow.item = it
        })
        mViewModel.fileInfo.observe(this, LiveDataObserver {
            mFileInfoRow.fileInfo = it
        })
        mViewModel.resumePosition.observe(this, LiveDataObserver { pos ->
            if (pos > 0) {
                mOverviewActionsAdapter.set(POS_RESUME, Action(ACTIONID_RESUME,
                        getString(R.string.btn_resume) + " (${humanReadableDuration(pos)})"))
                mOverviewActionsAdapter.set(POS_RESTART, Action(ACTIONID_START_OVER,
                        getString(R.string.btn_restart)))
            } else {
                setupDefaultActions()
            }
        })

        mBackgroundManager = BackgroundManager.getInstance(activity)

        mOverviewPresenter.onActionClickedListener = this
        mOverviewRow.actionsAdapter = mOverviewActionsAdapter

        //create row adapter
        val presenterSelector = ClassPresenterSelector()
        val rowsAdapter = ArrayObjectAdapter(presenterSelector)

        presenterSelector.addClassPresenter(DetailsOverviewRow::class.java, mOverviewPresenter)
        rowsAdapter.add(mOverviewRow)
        presenterSelector.addClassPresenter(FileInfoRow::class.java, mFileInfoPresenter)
        rowsAdapter.add(mFileInfoRow)

        adapter = rowsAdapter

        setupDefaultActions()
    }

    override fun onStart() {
        super.onStart()
        loadBackdropImage()
    }

    override fun onStop() {
        super.onStop()
        mBackgroundManager.release()
    }

    override fun onActionClicked(action: Action) {
        when (action.id) {
            ACTIONID_PLAY, ACTIONID_START_OVER -> {
                activity.startActivity(makePlayIntent().setAction(ACTION_PLAY))
            }
            ACTIONID_RESUME -> {
                activity.startActivity(makePlayIntent().setAction(ACTION_RESUME))
            }
            else -> {
                Toast.makeText(activity, "UNIMPLEMENTED", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun makePlayIntent() : Intent {
        return Intent(activity, PlaybackActivity::class.java)
                .putExtra(EXTRA_MEDIAID, arguments.getString(EXTRA_MEDIAID))
    }

    fun loadBackdropImage() {
        val defaultBackground = context.getDrawable(R.drawable.default_background)
        mBackgroundManager.drawable = defaultBackground
        //TODO load image
    }

    fun setupDefaultActions() {
        mOverviewActionsAdapter.clear()
        mOverviewActionsAdapter.set(POS_PLAY, Action(ACTIONID_PLAY, getString(R.string.btn_play)))
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
class DetailViewModel
@Inject constructor(
        val mClient: DatabaseClient
): ViewModel() {
    val videoDescription = MutableLiveData<VideoDescInfo>()
    val fileInfo = MutableLiveData<VideoFileInfo>()
    val resumePosition = MutableLiveData<Long>()

    private val disponables = CompositeDisposable()

    fun onMediaId(mediaId: String) {
        val ref = newMediaRef(mediaId)
        subscribeVideoDescription(ref)
        subscribeFileInfo(ref)
        subscribeLastPosition(ref)
    }

    fun subscribeVideoDescription(mediaRef: MediaRef) {
        val s = Single.zip<MediaMeta, String, VideoDescInfo>(
                mClient.getMediaMeta(mediaRef),
                mClient.getMediaOverview(mediaRef),
                BiFunction { meta, overview ->
                    VideoDescInfo(meta.title.elseIfBlank(meta.displayName),
                            meta.subtitle, overview)
                })
                .subscribeOn(AppSchedulers.diskIo)
                .subscribeIgnoreError(Consumer {
                    videoDescription.postValue(it)
                })
        disponables.add(s)
    }

    fun subscribeFileInfo(mediaRef: MediaRef) {
        val s = mClient.getMediaMeta(mediaRef)
                .subscribeOn(AppSchedulers.diskIo)
                .subscribeIgnoreError(Consumer {
                    fileInfo.postValue(VideoFileInfo(
                            it.displayName,
                            it.size,
                            it.duration
                    ))
                })
        disponables.add(s)
    }

    fun subscribeLastPosition(mediaRef: MediaRef) {
        val s = mClient.changesObservable
                .filter { it is UpnpVideoChange && it.videoId == mediaRef.mediaId }
                .map { true }
                .startWith(true)
                .observeOn(AppSchedulers.diskIo)
                .flatMapSingle {
                    mClient.getLastPlaybackPosition(mediaRef).onErrorReturn { 0 }
                }
                .subscribeIgnoreError(Consumer {
                    resumePosition.postValue(it)
                })
        disponables.add(s)
    }

    override fun onCleared() {
        super.onCleared()
        disponables.clear()
    }
}

/**
 *
 */
class FileInfoRow
@Inject constructor(@ForApplication context: Context): Row(HeaderItem(context.getString(R.string.header_file_info))) {

    interface Listener {
        fun onItemChanged(fileInfoRow: FileInfoRow)
    }

    private val mListeners: ArrayList<WeakReference<Listener>> = ArrayList()
    var fileInfo: VideoFileInfo by Delegates.observable(VideoFileInfo(), { _, _, _ ->
        notifyItemChanged()
    })

    /**
     * Adds listener for the details page.
     */
    internal fun addListener(listener: Listener) {
        var i = 0
        while (i < mListeners.size) {
            val l = mListeners[i].get()
            if (l == null) {
                mListeners.removeAt(i)
            } else {
                if (l === listener) {
                    return
                }
                i++
            }
        }
        mListeners.add(WeakReference(listener))
    }

    /**
     * Removes listener of the details page.
     */
    internal fun removeListener(listener: Listener) {
        var i = 0
        while (i < mListeners.size) {
            val l = mListeners[i].get()
            if (l == null) {
                mListeners.removeAt(i)
            } else {
                if (l === listener) {
                    mListeners.removeAt(i)
                    return
                }
                i++
            }
        }
    }

    /**
     * Notifies listeners for main item change on UI thread.
     */
    internal fun notifyItemChanged() {
        var i = 0
        while (i < mListeners.size) {
            val l = mListeners[i].get()
            if (l == null) {
                mListeners.removeAt(i)
            } else {
                l.onItemChanged(this)
                i++
            }
        }
    }
}

/**
 *
 */
class FileInfoRowPresenter
@Inject constructor() : RowPresenter() {

    override fun createRowViewHolder(parent: ViewGroup): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<DetailsFileInfoRowBinding>(inflater,
                R.layout.details_file_info_row, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindRowViewHolder(vh: RowPresenter.ViewHolder, item: Any) {
        super.onBindRowViewHolder(vh, item)
        val viewHolder = vh as ViewHolder
        val row = item as FileInfoRow
        viewHolder.binding.info = row.fileInfo
        viewHolder.onBind()
    }

    override fun onUnbindRowViewHolder(vh: RowPresenter.ViewHolder) {
        super.onUnbindRowViewHolder(vh)
        val viewHolder = vh as ViewHolder
        viewHolder.onUnbind()
    }

    inner class ViewHolder(
            val binding: DetailsFileInfoRowBinding
    ) : RowPresenter.ViewHolder(binding.root), FileInfoRow.Listener {

        internal fun onBind() {
            (this.row as? FileInfoRow)?.addListener(this)
        }

        internal fun onUnbind() {
            (this.row as? FileInfoRow)?.removeListener(this)
        }

        override fun onItemChanged(fileInfoRow: FileInfoRow) {
            onUnbindRowViewHolder(this)
            onBindRowViewHolder(this, fileInfoRow)
        }
    }
}

/**
 *
 */
class DetailOverviewPresenter
@Inject constructor() : AbstractDetailsDescriptionPresenter() {

    override fun onBindDescription(vh: AbstractDetailsDescriptionPresenter.ViewHolder, item: Any) {
        val info = item as VideoDescInfo
        vh.title.text = info.title
        vh.subtitle.text =info.subtitle
        vh.body.text = info.overview
    }

}

/**
 *
 */
class DetailOverviewActionsAdapter @Inject constructor(): SparseArrayObjectAdapter()