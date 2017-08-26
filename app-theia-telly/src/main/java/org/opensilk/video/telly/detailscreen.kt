package org.opensilk.video.telly

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.drawable.Drawable
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
import com.bumptech.glide.request.transition.Transition
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import org.opensilk.dagger2.ForApp
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
private const val POS_GET_DESC = 3
private const val POS_REM_DESC = POS_GET_DESC

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
 *
 */
@Module(includes = arrayOf(DetailPresenterModule::class))
abstract class DetailScreenModule {
    @ContributesAndroidInjector
    abstract fun injector(): DetailFragment
}


@Module
object DetailPresenterModule {
    @Provides @JvmStatic
    fun provideDescriptionPresenter(descPresenter: DetailOverviewPresenter): FullWidthDetailsOverviewRowPresenter {
        return FullWidthDetailsOverviewRowPresenter(descPresenter)
    }
    @Provides @JvmStatic
    fun provideDetailsRow(): DetailsOverviewRow {
        return DetailsOverviewRow(VideoDescInfo())
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
                            newDetailFragment(intent.getMediaIdExtra()), "detail_frag")
                    .commit()
        }
        BackgroundManager.getInstance(this).attach(window)
    }
}

fun newDetailFragment(mediaId: MediaId): DetailFragment {
    val f = DetailFragment()
    f.arguments = mediaId.asBundle()
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

    var mHasOverview = false

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        mBackgroundManager = BackgroundManager.getInstance(activity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBackgroundManager = BackgroundManager.getInstance(activity)

        mViewModel = fetchViewModel(DetailViewModel::class)
        mViewModel.setMediaId(arguments.getMediaId())
        mViewModel.videoDescription.observe(this, LiveDataObserver {
            mOverviewRow.item = it
        })
        mViewModel.fileInfo.observe(this, LiveDataObserver {
            mFileInfoRow.fileInfo = it
        })
        mViewModel.resumeInfo.observe(this, LiveDataObserver { (lastPosition, lastCompletion) ->
            if (lastCompletion in 1..979) {
                mOverviewActionsAdapter.set(POS_RESUME, Action(ACTIONID_RESUME,
                        getString(R.string.btn_resume, humanReadableDuration(lastPosition))))
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
        mViewModel.hasDescription.observe(this, LiveDataObserver {
            if (mHasOverview != it) {
                mHasOverview = it
                if (it) {
                    mOverviewActionsAdapter.set(POS_GET_DESC, Action(ACTIONID_GET_DESCRIPTION,
                            getString(R.string.btn_get_desc)))
                } else {
                    mOverviewActionsAdapter.set(POS_REM_DESC, Action(ACTIONID_REMOVE_DESCRIPTION,
                            getString(R.string.btn_rem_desc)))
                }
            }
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
            ACTIONID_GET_DESCRIPTION -> {
                mViewModel.doLookup(activity)
            }
            else -> {
                Toast.makeText(activity, "UNIMPLEMENTED", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun makePlayIntent() : Intent {
        return Intent(activity, PlaybackActivity::class.java)
                .putMediaIdExtra(arguments.getMediaId())
    }

    fun setupDefaultActions() {
        mOverviewActionsAdapter.clear()
        mOverviewActionsAdapter.set(POS_PLAY, Action(ACTIONID_PLAY, getString(R.string.btn_play)))
        mOverviewActionsAdapter.set(POS_GET_DESC, Action(ACTIONID_GET_DESCRIPTION,
                getString(R.string.btn_get_desc)))
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
class FileInfoRow
@Inject constructor(@ForApp context: Context): Row(HeaderItem(context.getString(R.string.header_file_info))) {

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