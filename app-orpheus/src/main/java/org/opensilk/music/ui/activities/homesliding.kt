package org.opensilk.music.ui.activities

import android.content.Intent
import android.databinding.DataBindingUtil
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.dagger2.getDaggerComponent
import org.opensilk.common.lifecycle.Lifecycle
import org.opensilk.common.lifecycle.LifecycleService
import org.opensilk.common.lifecycle.bindToLifeCycle
import org.opensilk.common.lifecycle.terminateOnDestroy
import org.opensilk.common.recycler.ItemClickSupport
import org.opensilk.common.rx.RxListLoader

import org.opensilk.music.R
import org.opensilk.music.RootComponent
import org.opensilk.music.data.DataService
import org.opensilk.music.data._getMediaMeta
import org.opensilk.music.databinding.RecyclerMediaListArtworkBinding
import org.opensilk.music.getRootComponent
import org.opensilk.music.ui.recycler.*
import rx.Observable
import rx.Subscription
import rx.functions.Action1
import rx.lang.kotlin.subscribeWith
import rx.lang.kotlin.subscriber
import rx.observers.Subscribers
import timber.log.Timber
import javax.inject.Inject

@ActivityScope
@dagger.Component(
        dependencies = arrayOf(
                RootComponent::class
        ),
        modules = arrayOf(
                HomeModule::class
        )
)
interface HomeComponent: BaseComponent {
    fun inject(activity: HomeSlidingActivity)
}

@dagger.Module(
        includes = arrayOf(
                BaseModule::class
        )
)
class HomeModule {

}

fun HomeSlidingActivity.buildComponent(): Lazy<HomeComponent> {
    return lazy {
        DaggerHomeComponent.builder().rootComponent(this.getRootComponent()).build()
    }
}

@ActivityScope
class HomePresenter
@Inject
constructor(

): BasePresenter() {

    override fun onLoad(savedInstanceState: Bundle?) {
        super.onLoad(savedInstanceState)

    }

}

@ActivityScope
class HomeLoader
@Inject
constructor(
        val dataService: DataService
): RxListLoader<MediaTile> {
    override val listObservable: Observable<List<MediaTile>>
        get() {
            return dataService.subscribeChanges()
                    .startWith(true)
                    //push change notifications onto background thread for db access
                    .observeOn(DataService.sSubscribeScheduler)
                    .concatMap { dataService.getRoots().toObservable() }
                    .map { list ->
                        list.map { item ->
                            MediaTile(item).apply {
                                tileLayout = R.layout.recycler_media_list_artwork_oneline
                            }
                        }
                    }
                    .observeOn(DataService.sMainScheduler)
        }

}

class HomeSlidingActivity : BaseSlidingActivity(), ItemClickSupport.OnItemClickListener {

    companion object {
        internal val REQUEST_OPEN_FOLDER = 1001
    }

    override val selfNavActionId: Int = R.id.nav_folders_root

    override val activityComponent: HomeComponent by buildComponent()

    override fun injectSelf() {
        activityComponent.inject(this)
    }

    @Inject override lateinit var mPresenter: HomePresenter
    @Inject internal lateinit var mLoader: HomeLoader
    @Inject internal lateinit var mAdapter: MediaItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //init loader
        mLoader.listObservable.terminateOnDestroy(this).subscribe(
                { list ->
                    if (list.isEmpty()) {
                        Snackbar.make(mBinding.coordinator, "No roots selected", Snackbar.LENGTH_INDEFINITE)
                                .setAction("Dismiss", {})
                                .show()
                    } else {
                        mAdapter.items = list
                        if (mBinding.recycler.adapter !== mAdapter) {
                            mBinding.recycler.layoutManager = LinearLayoutManager(this)
                            mBinding.recycler.adapter = mAdapter
                        }
                    }
                },
                {
                    TODO()
                },
                {
                    Timber.d("onComplete()")
                }
        )
        //init click listeners
        ItemClickSupport.addTo(mBinding.recycler)
                .setOnItemClickListener(this)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.add_root, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            R.id.menu_add_root -> {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_OPEN_FOLDER)
                true //ret
            }
            else -> super.onOptionsItemSelected(item) //ret
        }
    }

    override fun onItemClicked(recyclerView: RecyclerView?, position: Int, v: View?) {
        val tile = mAdapter.items[position]
        val meta = tile.item._getMediaMeta()
        if (meta.isDirectory) {
            startActivity(Intent(this, FolderSlidingActivity::class.java).putExtra(EXTRA_MEDIA_ITEM, tile.item))
        } else {
            TODO()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.d("onActivityResult: requestCode %d resultCode %d data %s", requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_OPEN_FOLDER -> {
                when (resultCode) {
                    RESULT_OK -> {
                        data?.data?.let {
                            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            contentResolver.takePersistableUriPermission(it, flags)
                            mLoader.dataService.insertRoot(it).subscribe { added ->
                                if (!added) {
                                    Snackbar.make(mPresenter.binding.coordinator, "Failed to add root", Snackbar.LENGTH_LONG)
                                }
                            }
                        } ?: Timber.e("The returned data was null")
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

}
