package org.opensilk.video.telly

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v17.leanback.app.VerticalGridSupportFragment
import android.support.v17.leanback.widget.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import org.opensilk.media.*
import org.opensilk.video.FolderViewModel
import org.opensilk.video.LiveDataObserver
import javax.inject.Inject
import android.support.v17.leanback.widget.TitleViewAdapter.*

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
        mViewModel.setMediaId(arguments.getMediaId())
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

    override fun onInflateTitleView(inflater: LayoutInflater, parent: ViewGroup,
                                    savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.folder_title, parent, false)

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

class FolderTitleView(context: Context, attrs: AttributeSet) :
        FrameLayout(context, attrs), TitleViewAdapter.Provider {

    private val mTextView: TextView
    private val mSearchOrbView: SearchOrbView
    private var flags = FULL_VIEW_VISIBLE
    private var mHasSearchListener = false
    init {
        val inflater = LayoutInflater.from(context)
        val rootView = inflater.inflate(R.layout.folder_title_view, this)

        mTextView = rootView.findViewById(R.id.title_text) as TextView
        mSearchOrbView = rootView.findViewById(R.id.title_orb) as SearchOrbView

        clipToPadding = false
        clipChildren = false
    }

    var title: CharSequence?
        set(value) {
            mTextView.text = value
        }
        get() = mTextView.text

    fun setOnSearchClickedListener(listener: View.OnClickListener?) {
        mHasSearchListener = listener != null
        mSearchOrbView.setOnOrbClickedListener(listener)
        updateSearchOrbViewVisiblity()
    }

    fun getSearchAffordanceView(): View = mSearchOrbView

    fun setSearchAffordanceColors(colors: SearchOrbView.Colors) {
        mSearchOrbView.orbColors = colors
    }

    fun getSearchAffordanceColors(): SearchOrbView.Colors = mSearchOrbView.orbColors

    fun enableAnimation(enable: Boolean) {
        mSearchOrbView.enableOrbColorAnimation(enable && mSearchOrbView.hasFocus())
    }

    fun updateComponentsVisibility(flags: Int) {
        this.flags = flags
        updateSearchOrbViewVisiblity()
    }

    private fun updateSearchOrbViewVisiblity() {
        val visibility = if (mHasSearchListener && flags and SEARCH_VIEW_VISIBLE == SEARCH_VIEW_VISIBLE)
            View.VISIBLE
        else
            View.INVISIBLE
        mSearchOrbView.visibility = visibility
    }

    override fun getTitleViewAdapter(): TitleViewAdapter = mTitleViewAdapter

    private val mTitleViewAdapter = object: TitleViewAdapter() {
        override fun getSearchAffordanceColors(): SearchOrbView.Colors =
                this@FolderTitleView.getSearchAffordanceColors()

        override fun setSearchAffordanceColors(colors: SearchOrbView.Colors) {
            this@FolderTitleView.setSearchAffordanceColors(colors)
        }

        override fun setOnSearchClickedListener(listener: OnClickListener?) {
            this@FolderTitleView.setOnSearchClickedListener(listener)
        }

        override fun updateComponentsVisibility(flags: Int) {
            this@FolderTitleView.updateComponentsVisibility(flags)
        }

        override fun setAnimationEnabled(enable: Boolean) {
            this@FolderTitleView.enableAnimation(enable)
        }

        override fun setTitle(titleText: CharSequence?) {
            this@FolderTitleView.title = titleText
        }

        override fun getTitle(): CharSequence? = this@FolderTitleView.title

        override fun getSearchAffordanceView(): View =
                this@FolderTitleView.getSearchAffordanceView()
    }

}