package org.opensilk.video.telly

import android.arch.lifecycle.*
import android.support.v17.leanback.widget.ObjectAdapter
import android.support.v17.leanback.widget.Presenter
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.util.DiffUtil
import android.support.v7.util.ListUpdateCallback
import io.reactivex.Single
import io.reactivex.disposables.Disposables
import org.opensilk.media.MediaRef
import org.opensilk.video.AppSchedulers
import kotlin.reflect.KClass

/**
 *
 */
fun <T: ViewModel> Fragment.fetchViewModel(clazz: KClass<T>): T {
    val vm = ViewModelProviders.of(this, (activity.application as ViewModelProvider.Factory)).get(clazz.java)
    if (this is LifecycleRegistryOwner && vm is LifecycleObserver) {
        this.lifecycle.addObserver(vm)
    }
    return vm
}

fun <T: ViewModel> BaseVideoActivity.fetchViewModel(clazz: KClass<T>): T {
    val vm = ViewModelProviders.of(this, (application as ViewModelProvider.Factory)).get(clazz.java)
    if (vm is LifecycleObserver) {
        this.lifecycle.addObserver(vm)
    }
    return vm
}

/**
 * Created by drew on 6/1/17.
 */
abstract class BaseVideoActivity: FragmentActivity(), LifecycleRegistryOwner {

    private val lifecycleRegistry by lazy {
        LifecycleRegistry(this)
    }

    override fun getLifecycle(): LifecycleRegistry = lifecycleRegistry

}

open class SwappingObjectAdapter(presenter: Presenter): ObjectAdapter(presenter), ListUpdateCallback {
    private var mDisposable = Disposables.disposed()
    private var mCurrentList = emptyList<MediaRef>()
    private var mPendingList = emptyList<MediaRef>()

    override fun size(): Int = mCurrentList.size

    override fun get(position: Int): Any = mCurrentList[position]

    fun swapList(itemList: List<MediaRef>) {
        mDisposable.dispose()
        if (mCurrentList.isEmpty()) {
            mCurrentList = itemList
            mPendingList = emptyList()
            notifyChanged()
            return
        }
        if (itemList.isEmpty()) {
            mCurrentList = emptyList()
            mPendingList = emptyList()
            notifyChanged()
            return
        }
        mPendingList = itemList
        mDisposable = Single.fromCallable {
            DiffUtil.calculateDiff(DiffCallback(ArrayList(itemList), ArrayList(mCurrentList)))
        }.subscribeOn(AppSchedulers.background).observeOn(AppSchedulers.main)
                .subscribe({ result ->
                    mCurrentList = mPendingList
                    result.dispatchUpdatesTo(this)
                })
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        notifyItemRangeChanged(position, count)
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        notifyChanged() //TODO
    }

    override fun onInserted(position: Int, count: Int) {
        notifyItemRangeInserted(position, count)
    }

    override fun onRemoved(position: Int, count: Int) {
        notifyItemRangeRemoved(position, count)
    }

    class DiffCallback(val itemList: List<MediaRef>, val oldList: List<MediaRef>): DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldList[oldItemPosition].id == itemList[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldList[oldItemPosition] == itemList[newItemPosition]

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = itemList.size
    }
}
