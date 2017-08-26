package org.opensilk.video.phone

import android.arch.lifecycle.LifecycleFragment
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.multibindings.IntoMap
import io.reactivex.Single
import io.reactivex.disposables.Disposables
import org.opensilk.media.*
import org.opensilk.video.AppSchedulers
import org.opensilk.video.FolderViewModel
import org.opensilk.video.LiveDataObserver
import org.opensilk.video.ViewModelKey
import javax.inject.Inject

/**
 * Created by drew on 8/7/17.
 */
@Module
abstract class FolderScreenModule {
    @ContributesAndroidInjector
    abstract fun folderFragment(): FolderFragment
    @Binds @IntoMap @ViewModelKey(FolderActivityViewModel::class)
    abstract fun viewModel(vm: FolderActivityViewModel): ViewModel
}

class FolderActivity: DrawerActivity(), MediaRefClickListener {

    lateinit var mViewModel: FolderActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mViewModel = fetchViewModel(FolderActivityViewModel::class)

        mViewModel.loadError.observe(this, LiveDataObserver {
            Snackbar.make(mBinding.coordinator, it, Snackbar.LENGTH_INDEFINITE).show()
        })
        mViewModel.mediaTitle.observe(this, LiveDataObserver {
            title = it
        })

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.coordinator, newFolderFragment(intent.getMediaIdExtra()))
                    .commit()
        }
    }

    override fun onClick(mediaRef: MediaRef): Boolean = when (mediaRef) {
        is MediaDeviceRef,
        is FolderRef -> {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.coordinator, newFolderFragment(mediaRef.id))
                    .addToBackStack(null)
                    .commit()
            true
        }
        else -> false
    }

    override fun onLongClick(mediaRef: MediaRef): Boolean = false

}

class FolderActivityViewModel @Inject constructor(): ViewModel() {
    val mediaTitle = MutableLiveData<String>()
    val loadError = MutableLiveData<String>()
}

fun newFolderFragment(mediaId: MediaId): FolderFragment {
    val f = FolderFragment()
    f.arguments = mediaId.asBundle()
    return f
}

class FolderFragment: RecyclerFragment() {

    lateinit var mViewModel: FolderViewModel
    lateinit var mActivityViewModel: FolderActivityViewModel

    @Inject lateinit var mAdapter: FolderAdapter

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = fetchViewModel(FolderViewModel::class)
        mActivityViewModel = fetchViewModel(FolderActivityViewModel::class)
        mViewModel.onMediaId(arguments.getMediaId())
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.recycler.adapter = mAdapter

        mViewModel.folderItems.observe(this, LiveDataObserver {
            mAdapter.swapList(it)
        })
        mViewModel.loadError.observe(this, LiveDataObserver {
            mActivityViewModel.loadError.value = it
        })
        mViewModel.mediaTitle.observe(this, LiveDataObserver {
            mActivityViewModel.mediaTitle.value = it
        })
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
