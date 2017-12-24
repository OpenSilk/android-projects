package org.opensilk.video.telly

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v17.leanback.app.VerticalGridSupportFragment
import android.support.v17.leanback.widget.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import org.opensilk.media.*
import org.opensilk.video.FolderViewModel
import org.opensilk.video.LiveDataObserver
import javax.inject.Inject
import android.support.v4.content.ContextCompat
import android.widget.*
import org.opensilk.video.FolderAction
import org.opensilk.video.telly.databinding.FolderTitleBinding

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
    lateinit var mTitleBinding: FolderTitleBinding

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
        mViewModel.actions.observe(this, LiveDataObserver { list ->
            list.forEach { action ->
                when (action) {
                    FolderAction.PIN -> mTitleBinding.browseTitleGroup.setPinned(false)
                    FolderAction.UNPIN -> mTitleBinding.browseTitleGroup.setPinned(true)
                }
            }
        })

        gridPresenter = mFolderPresenter
        gridPresenter.numberOfColumns = 1

        adapter = mFolderAdapter
        onItemViewClickedListener = activity as OnItemViewClickedListener

    }

    override fun onInflateTitleView(inflater: LayoutInflater, parent: ViewGroup,
                                    savedInstanceState: Bundle?): View {
        mTitleBinding = DataBindingUtil.inflate(inflater, R.layout.folder_title, parent, false)
        return mTitleBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnSearchClickedListener {
            Toast.makeText(view.context, "TODO search", Toast.LENGTH_LONG).show()
        }
        mTitleBinding.browseTitleGroup.setPinClickListener(View.OnClickListener {
            if (mTitleBinding.browseTitleGroup.isPinned()) {
                mViewModel.unpinItem()
            } else {
                mViewModel.pinItem()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mTitleBinding.unbind()
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

class FolderTitleView(context: Context, attrs: AttributeSet) :
        RelativeLayout(context, attrs), TitleViewAdapter.Provider {

    private val mTextView: TextView
    private val mSearchOrbView: SearchOrbView
    private val mPinView: SearchOrbView
    private var mHasSearchListener = false
    private var mIsPinned = false
    init {
        val inflater = LayoutInflater.from(context)
        val rootView = inflater.inflate(R.layout.folder_title_view, this)

        mTextView = rootView.findViewById(R.id.title_text)
        mSearchOrbView = rootView.findViewById(R.id.title_orb)
        mPinView = rootView.findViewById(R.id.title_pin_orb)

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
    }

    fun getSearchAffordanceView(): View = mSearchOrbView

    fun setSearchAffordanceColors(colors: SearchOrbView.Colors) {
        mSearchOrbView.orbColors = colors
    }

    fun getSearchAffordanceColors(): SearchOrbView.Colors = mSearchOrbView.orbColors

    fun setPinClickListener(listener: View.OnClickListener?) {
        mPinView.setOnOrbClickedListener(listener)
    }

    fun setPinned(pinned: Boolean) {
        mIsPinned = pinned
        mPinView.orbIcon = ContextCompat.getDrawable(mPinView.context,
                if (pinned) R.drawable.unpin_36dp else R.drawable.pin_36dp)
    }

    fun isPinned(): Boolean = mIsPinned

    fun enableAnimation(enable: Boolean) {
        mSearchOrbView.enableOrbColorAnimation(enable && mSearchOrbView.hasFocus())
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