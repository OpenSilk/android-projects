package org.opensilk.video.telly

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.v17.leanback.app.BackgroundManager
import android.support.v17.leanback.app.DetailsFragment
import android.support.v17.leanback.app.DetailsSupportFragment
import android.support.v17.leanback.widget.*
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import dagger.*
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.injectMe
import org.opensilk.media._getMediaMeta
import org.opensilk.video.VideoDescInfo
import org.opensilk.video.VideoFileInfo
import org.opensilk.video.telly.databinding.DetailsFileInfoRowBinding
import java.lang.ref.WeakReference
import java.util.ArrayList
import javax.inject.Inject
import kotlin.properties.Delegates

/*
 ids for detail actions
 */
const val ACTIONID_RESUME = 100L
const val ACTIONID_START_OVER = 101L
//
//
const val ACTIONID_PLAY = 200L
//
//
const val ACTIONID_GET_DESCRIPTION = 300L
const val ACTIONID_REMOVE_DESCRIPTION = 301L

const val SHARED_ELEMENT_NAME = "hero"


/**
 * Created by drew on 6/2/17.
 */
@ActivityScope
@Subcomponent(modules = arrayOf(DetailPresenterModule::class))
interface DetailComponent: Injector<DetailFragment> {
    @Subcomponent.Builder
    abstract class Builder: Injector.Builder<DetailFragment>() {
        override fun create(t: DetailFragment): Injector<DetailFragment> {
            val mediaItem: MediaBrowser.MediaItem = t.activity.intent.getParcelableExtra(EXTRA_MEDIAITEM)
            return mediaItem(mediaItem).build()
        }
        @BindsInstance
        abstract fun mediaItem(mediaItem: MediaBrowser.MediaItem): Builder
    }
}

/**
 *
 */
@Module(subcomponents = arrayOf(DetailComponent::class))
abstract class DetailModule

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
        fun provideVideoFileInfo(mediaItem: MediaBrowser.MediaItem): VideoFileInfo {
            val desc = mediaItem.description
            val meta = mediaItem._getMediaMeta()
            return VideoFileInfo(meta.mediaUri, desc.title.toString(), meta.size, meta.duration * 1000)
        }
        @Provides @JvmStatic
        fun provideDetailOverviewRow(mediaItem: MediaBrowser.MediaItem): DetailsOverviewRow {
            val desc = mediaItem.description
            val row = DetailsOverviewRow(VideoDescInfo(desc.title.toString(),
                    desc.subtitle.toString(), desc.description?.toString() ?: ""))
            return row
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
                    .replace(R.id.detail_fragment, DetailFragment(), "detail_frag")
                    .commit()
        }
        BackgroundManager.getInstance(this).attach(window)
    }
}

/**
 *
 */
class DetailFragment: DetailsSupportFragment(), OnActionClickedListener {

    @Inject lateinit var mMediaItem: MediaBrowser.MediaItem
    @Inject lateinit var mOverviewPresenter: FullWidthDetailsOverviewRowPresenter
    @Inject lateinit var mOverviewRow: DetailsOverviewRow
    @Inject lateinit var mOverviewActionsAdapter: DetailOverviewActionsAdapter
    @Inject lateinit var mFileInfoPresenter: FileInfoRowPresenter
    @Inject lateinit var mFileInfoRow: FileInfoRow

    lateinit var mBackgroundManager: BackgroundManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        injectMe()
        mBackgroundManager = BackgroundManager.getInstance(activity)
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        setupActions()
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
            ACTIONID_PLAY, ACTIONID_RESUME, ACTIONID_START_OVER -> {
                val intent = Intent(activity, PlaybackActivity::class.java)
                intent.action = ACTION_PLAY
                intent.putExtra(EXTRA_MEDIAITEM, mMediaItem)
                activity.startActivity(intent)
            }
            else -> {
                Toast.makeText(activity, "UNIMPLEMENTED", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun loadBackdropImage() {
        val defaultBackground = context.getDrawable(R.drawable.default_background)
        mBackgroundManager.drawable = defaultBackground
        //TODO load image
    }

    fun setupActions() {
        mOverviewActionsAdapter.set(ACTIONID_PLAY.toInt(), Action(ACTIONID_PLAY, "Play"))
    }
}

/**
 *
 */
class FileInfoRow
@Inject constructor(
        initialFileInfo: VideoFileInfo
): Row(HeaderItem("File Info")) { //TODO use string resource

    interface Listener {
        fun onItemChanged(fileInfoRow: FileInfoRow)
    }

    private val mListeners: ArrayList<WeakReference<Listener>> = ArrayList()
    var fileInfo: VideoFileInfo by Delegates.observable(initialFileInfo, { _, _, _ ->
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