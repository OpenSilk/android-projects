package org.opensilk.video.phone

import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.transition.Slide
import android.view.*
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Single
import io.reactivex.disposables.Disposables
import org.opensilk.media.*
import org.opensilk.video.AppSchedulers
import org.opensilk.video.FolderAction
import org.opensilk.video.FolderViewModel
import org.opensilk.video.LiveDataObserver
import javax.inject.Inject

/**
 * Created by drew on 8/7/17.
 */
@Module
abstract class FolderScreenModule {
    @ContributesAndroidInjector
    abstract fun folderFragment(): FolderFragment
}

class FolderActivity: DrawerActivity(), MediaRefClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(DRAWER_FRAGMENT_CONTAINER,
                            newFolderFragment(intent.getMediaIdExtra()))
                    .commit()
        }
    }

    override fun onClick(mediaRef: MediaRef): Boolean = when (mediaRef) {
        is MediaDeviceRef,
        is FolderRef -> {
            supportFragmentManager.beginTransaction()
                    .replace(DRAWER_FRAGMENT_CONTAINER,
                            newFolderFragment(mediaRef.id))
                    .addToBackStack(null)
                    .commit()
            true
        }
        else -> false
    }

    override fun onLongClick(mediaRef: MediaRef): Boolean = false

}

fun newFolderFragment(mediaId: MediaId): FolderFragment {
    val f = FolderFragment()
    f.arguments = mediaId.asBundle()
    f.reenterTransition = Slide(Gravity.START)
    f.enterTransition = Slide(Gravity.END)
    f.exitTransition = Slide(Gravity.START)
    f.returnTransition = Slide(Gravity.END)
    return f
}

class FolderFragment: RecyclerFragment(), Toolbar.OnMenuItemClickListener {

    lateinit var mViewModel: FolderViewModel

    @Inject lateinit var mAdapter: FolderAdapter

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mViewModel = fetchViewModel(FolderViewModel::class)

        mViewModel.setMediaId(arguments.getMediaId())

        mViewModel.folderItems.observe(this, LiveDataObserver {
            mBinding.swipeRefresh.isRefreshing = false
            mAdapter.swapList(it)
        })
        mViewModel.loadError.observe(this, LiveDataObserver {
            mBinding.swipeRefresh.isRefreshing = false
            Snackbar.make(mBinding.coordinator, "Error: $it", Snackbar.LENGTH_INDEFINITE)
        })
        mViewModel.mediaTitle.observe(this, LiveDataObserver {
            mBinding.toolbar.title = it
        })
        mViewModel.actions.observe(this, LiveDataObserver { list ->
            mBinding.toolbar.menu.clear()
            list.forEach { action ->
                mBinding.toolbar.inflateMenu(when (action) {
                    FolderAction.PIN -> R.menu.pin_item
                    FolderAction.UNPIN -> R.menu.unpin_item
                })
            }
        })

    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.recycler.adapter = mAdapter
        mBinding.toolbar.setOnMenuItemClickListener(this)
        if (savedInstanceState == null) {
            mBinding.swipeRefresh.isRefreshing = true
        }
        mBinding.swipeRefresh.setOnRefreshListener {
            mViewModel.runPrefetch()
        }
        mViewModel.mediaTitle.value?.let {
            mBinding.toolbar.title = it
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_pin -> mViewModel.pinItem()
            R.id.menu_unpin -> mViewModel.unpinItem()
            else -> TODO()
        }
        return true
    }

}

class FolderAdapter @Inject constructor(): RecyclerView.Adapter<ListItemViewHolder>() {

    private var mList = emptyList<MediaRef>()
    private var mDisposable = Disposables.disposed()

    fun swapList(newList: List<MediaRef>) {
        handleSwap(ArrayList(mList), newList)
    }

    private fun handleSwap(oldList: List<MediaRef>, newList: List<MediaRef>) {
        mDisposable.dispose()
        if (oldList.isEmpty()) {
            mList = newList
            notifyItemRangeInserted(0, newList.size)
            return
        }
        if (newList.isEmpty()) {
            mList = emptyList()
            notifyItemRangeRemoved(0, oldList.size)
            return
        }
        mDisposable = Single.fromCallable {
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                        oldList[oldItemPosition].id == newList[newItemPosition].id
                override fun getOldListSize(): Int = oldList.size
                override fun getNewListSize(): Int = newList.size
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                        oldList[oldItemPosition] == newList[newItemPosition]
            })
        }.subscribeOn(AppSchedulers.background).observeOn(AppSchedulers.main).subscribe { result ->
            mList = newList
            result.dispatchUpdatesTo(this)
        }
    }

    override fun onBindViewHolder(holder: ListItemViewHolder, position: Int) {
        holder.bind(mList[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder {
        return ListItemViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                R.layout.recycler_list_item, parent, false))
    }

    override fun getItemCount(): Int = mList.size
}
