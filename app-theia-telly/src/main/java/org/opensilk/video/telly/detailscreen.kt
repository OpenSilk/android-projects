package org.opensilk.video.telly

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.v17.leanback.app.BackgroundManager
import android.support.v17.leanback.app.DetailsSupportFragment
import android.support.v17.leanback.widget.*
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.multibindings.IntoMap
import io.reactivex.Observable
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

        mBackgroundManager = BackgroundManager.getInstance(activity)

        mViewModel = fetchViewModel(DetailViewModel::class)
        mViewModel.onMediaId(arguments.getString(EXTRA_MEDIAID))
        mViewModel.videoDescription.observe(this, LiveDataObserver {
            mOverviewRow.item = it
        })
        mViewModel.fileInfo.observe(this, LiveDataObserver {
            mFileInfoRow.fileInfo = it
        })
        mViewModel.resumeInfo.observe(this, LiveDataObserver { (lastPosition, lastCompletion) ->
            if (lastCompletion in 1..979) {
                mOverviewActionsAdapter.set(POS_RESUME, Action(ACTIONID_RESUME,
                        getString(R.string.btn_resume) + " (${humanReadableDuration(lastPosition)})"))
                mOverviewActionsAdapter.set(POS_RESTART, Action(ACTIONID_START_OVER,
                        getString(R.string.btn_restart)))
            } else {
                setupDefaultActions()
            }
        })
        mViewModel.posterUri.observe(this, LiveDataObserver {
            val width = resources.getDimensionPixelSize(R.dimen.detail_thumb_width)
            val height = resources.getDimensionPixelSize(R.dimen.detail_thumb_height)
            Glide.with(this)
                    .asDrawable()
                    .apply(RequestOptions.centerCropTransform())
                    .load(it)
                    .into(object: SimpleTarget<Drawable>(width, height) {
                        override fun onResourceReady(resource: Drawable?, transition: Transition<in Drawable>?) {
                            mOverviewRow.imageDrawable = resource
                        }
                    })
        })
        mViewModel.backdropUri.observe(this, LiveDataObserver {
            val metrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(metrics)
            Glide.with(this)
                    .asDrawable()
                    .apply(RequestOptions().fitCenter()
                            .placeholder(activity.getDrawable(R.drawable.default_background)))
                    .load(it)
                    .into(object: SimpleTarget<Drawable>(metrics.widthPixels, metrics.heightPixels) {
                        override fun onResourceReady(resource: Drawable?, transition: Transition<in Drawable>?) {
                            mBackgroundManager.drawable = resource
                        }
                    })
        })

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

data class ResumeInfo(val lastPosition: Long = 0, val lastCompletion: Int = 0)

/**
 *
 */
class DetailViewModel
@Inject constructor(
        val mClient: DatabaseClient
): ViewModel() {
    val videoDescription = MutableLiveData<VideoDescInfo>()
    val fileInfo = MutableLiveData<VideoFileInfo>()
    val resumeInfo = MutableLiveData<ResumeInfo>()
    val posterUri = MutableLiveData<Uri>()
    val backdropUri = MutableLiveData<Uri>()

    private val disponables = CompositeDisposable()

    fun onMediaId(mediaId: String) {
        disponables.clear()
        val ref = newMediaRef(mediaId)
        subscribeVideoDescription(ref)
        subscribeFileInfo(ref)
        subscribeLastPosition(ref)
        subscribePosterUri(ref)
        subscribeBackdropUri(ref)
    }

    fun changes(mediaRef: MediaRef): Observable<Boolean> {
        return mClient.changesObservable
                .filter { it is UpnpVideoChange && it.videoId == mediaRef.mediaId }
                .map { true }
                .startWith(true)
    }

    fun cachedMeta(mediaRef: MediaRef): Observable<MediaMeta> {
        return changes(mediaRef)
                .flatMapSingle {
                    mClient.getUpnpVideo(mediaRef.mediaId as UpnpVideoId)
                            .subscribeOn(AppSchedulers.diskIo)
                }
    }

    fun subscribeVideoDescription(mediaRef: MediaRef) {
        val s = cachedMeta(mediaRef).flatMapMaybe { meta ->
            mClient.getMediaOverview(mediaRef).defaultIfEmpty("").map { overview ->
                VideoDescInfo(meta.title.elseIfBlank(meta.displayName), meta.subtitle, overview)
            }
        }.subscribeIgnoreError(Consumer {
            videoDescription.postValue(it)
        })
        disponables.add(s)
    }

    fun subscribeFileInfo(mediaRef: MediaRef) {
        val s = cachedMeta(mediaRef).map { meta ->
            VideoFileInfo(meta.displayName, meta.size, meta.duration)
        }.subscribeIgnoreError(Consumer {
            fileInfo.postValue(it)
        })
        disponables.add(s)
    }

    fun subscribeLastPosition(mediaRef: MediaRef) {
        val s = changes(mediaRef)
                .flatMapSingle {
                    Single.zip<Long, Int, ResumeInfo>(
                            mClient.getLastPlaybackPosition(mediaRef),
                            mClient.getLastPlaybackCompletion(mediaRef),
                            BiFunction { pos, comp -> ResumeInfo(pos, comp) }
                    ).onErrorReturn { ResumeInfo() }.subscribeOn(AppSchedulers.diskIo)
                }
                .subscribeIgnoreError(Consumer {
                    resumeInfo.postValue(it)
                })
        disponables.add(s)
    }

    fun subscribePosterUri(mediaRef: MediaRef) {
        val s = cachedMeta(mediaRef)
                .map { it.artworkUri }
                .filter { it != Uri.EMPTY }
                .subscribeIgnoreError(Consumer {
                    posterUri.postValue(it)
                })
        disponables.add(s)
    }

    fun subscribeBackdropUri(mediaRef: MediaRef) {
        val s = cachedMeta(mediaRef)
                .map { it.backdropUri }
                .filter { it != Uri.EMPTY }
                .subscribeIgnoreError(Consumer {
                    backdropUri.postValue(it)
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