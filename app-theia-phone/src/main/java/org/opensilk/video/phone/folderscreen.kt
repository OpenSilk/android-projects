package org.opensilk.video.phone

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import dagger.Module
import dagger.Subcomponent
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.functions.Consumer
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.injectMe
import org.opensilk.media.MediaRef
import org.opensilk.video.AppSchedulers
import org.opensilk.video.EXTRA_MEDIAID
import org.opensilk.video.FolderViewModel
import org.opensilk.video.LiveDataObserver
import org.opensilk.video.phone.databinding.ActivityDrawerBinding
import javax.inject.Inject

/**
 * Created by drew on 8/7/17.
 */
@Subcomponent
interface FolderScreenComponent: Injector<FolderActivity>{
    @Subcomponent.Builder
    abstract class Builder: Injector.Builder<FolderActivity>()
}

@Module(subcomponents = arrayOf(FolderScreenComponent::class))
abstract class FolderScreenModule

class FolderActivity: DrawerActivity() {

    lateinit var mViewModel: FolderViewModel

    @Inject lateinit var mAdapter: FolderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        injectMe()
        super.onCreate(savedInstanceState)

        mBinding.recycler.layoutManager = LinearLayoutManager(this)
        mBinding.recycler.adapter = mAdapter

        mViewModel = fetchViewModel(FolderViewModel::class)
        mViewModel.folderItems.observe(this, LiveDataObserver {
            mAdapter.setList(it)
        })
        mViewModel.loadError.observe(this, LiveDataObserver {
            Snackbar.make(mBinding.coordinator, it, Snackbar.LENGTH_INDEFINITE).show()
        })
        mViewModel.mediaTitle.observe(this, LiveDataObserver {
            mBinding.toolbar.title = it
        })

        mViewModel.onMediaId(intent.getStringExtra(EXTRA_MEDIAID))

    }

}

class FolderAdapter
@Inject constructor(): RecyclerView.Adapter<ListItemViewHolder>() {

    private val mList = ArrayList<MediaRef>()
    private var mDiffDisposable = Disposables.disposed()

    fun setList(newList: List<MediaRef>){
        mDiffDisposable.dispose()
        if (mList.isEmpty()) {
            mList.addAll(newList)
            notifyDataSetChanged()
            return
        }
        if (newList.isEmpty()) {
            mList.clear()
            notifyDataSetChanged()
            return
        }
        mDiffDisposable = subscribeDiff(ArrayList(mList), newList, Consumer {
            it.dispatchUpdatesTo(this)
        })
    }

    override fun onBindViewHolder(holder: ListItemViewHolder, position: Int) {
        holder.bind(mList[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder {
        return ListItemViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                R.layout.recycler_list_item, parent, false))
    }

    override fun getItemCount(): Int {
        return mList.size
    }
}

fun subscribeDiff(oldList: List<MediaRef>, newList: List<MediaRef>, onSuccess: Consumer<DiffUtil.DiffResult>): Disposable {
    return Single.create<DiffUtil.DiffResult> { s ->
        val res = DiffUtil.calculateDiff(object: DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition].id == newList[newItemPosition].id
            }

            override fun getOldListSize(): Int {
                return oldList.size
            }

            override fun getNewListSize(): Int {
                return newList.size
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition] == newList[newItemPosition]
            }
        })
        s.onSuccess(res)
    }.subscribeOn(AppSchedulers.background).observeOn(AppSchedulers.main).subscribe(onSuccess)
}