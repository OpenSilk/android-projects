package org.opensilk.music.ui.activities

import android.content.Intent
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import dagger.Provides
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.lifecycle.terminateOnDestroy
import org.opensilk.common.recycler.ItemClickSupport
import org.opensilk.common.loader.RxListLoader
import org.opensilk.music.R
import org.opensilk.music.RootComponent
import org.opensilk.music.data.*
import org.opensilk.music.data.ref.DocumentRef
import org.opensilk.music.getRootComponent
import org.opensilk.music.ui.recycler.MediaItemAdapter
import org.opensilk.music.ui.recycler.MediaTile
import rx.Observable
import rx.exceptions.Exceptions
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * Created by drew on 8/1/16.
 */

@ActivityScope
@dagger.Component(
        dependencies = arrayOf(
                RootComponent::class
        ),
        modules = arrayOf(
                FolderModule::class
        )
)
interface FolderComponent: BaseComponent {
    fun inject(activity: FolderSlidingActivity)
}

@dagger.Module(
        includes = arrayOf(
                BaseModule::class
        )
)
class FolderModule (
        val mediaItem: MediaBrowser.MediaItem
){
    @Provides @ActivityScope
    fun provideMediaItem() : MediaBrowser.MediaItem {
        return mediaItem
    }
}

fun FolderSlidingActivity.buildComponent(): Lazy<FolderComponent> {
    return lazy {
        val mediaItem: MediaBrowser.MediaItem = intent.getParcelableExtra(EXTRA_MEDIA_ITEM)
        return@lazy DaggerFolderComponent.builder()
                .rootComponent(this.getRootComponent())
                .folderModule(FolderModule(mediaItem))
                .build()
    }
}

@ActivityScope
class FolderPresenter
@Inject
constructor(): BasePresenter() {

}

@ActivityScope
class FolderLoader
@Inject
constructor(
        val mediaItem: MediaBrowser.MediaItem,
        val dataService: DataService
) : RxListLoader<MediaTile> {

    var comparator: Comparator<MediaBrowser.MediaItem> = AscendingCompare

    override val listObservable: Observable<List<MediaTile>>
        get() = dataService.subscribeChanges(mediaItem)
                .observeOn(DataService.sSubscribeScheduler)
                .concatMap {
                    val docRef = mediaItem._getMediaRef()
                    return@concatMap if (docRef is DocumentRef) {
                        dataService.getDocChildren(docRef).toObservable()
                    } else {
                        Observable.error<List<MediaBrowser.MediaItem>>(
                                IllegalArgumentException("Unsupported mediaRef")
                        )
                    }
                }.map { list ->
                    list.sortedWith(comparator).map {
                        MediaTile(it).apply {
                            tileLayout = R.layout.recycler_media_list_artwork
                        }
                    }
                }.observeOn(DataService.sMainScheduler)

    fun requestNext() {
        dataService.notifyItemChanged(mediaItem)
    }
}

const val EXTRA_MEDIA_ITEM = "media_item"

class FolderSlidingActivity(): BaseSlidingActivity(), ItemClickSupport.OnItemClickListener {

    override val selfNavActionId: Int = 0

    override val activityComponent: FolderComponent by buildComponent()

    override fun injectSelf() {
        activityComponent.inject(this)
    }

    @Inject internal lateinit var mMediaItem: MediaBrowser.MediaItem
    @Inject override lateinit var mPresenter: FolderPresenter
    @Inject internal lateinit var mLoader: FolderLoader
    @Inject internal lateinit var mAdapter: MediaItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //init loader
        mLoader.listObservable.terminateOnDestroy(this).subscribe(
                { list ->
                    if (list.isEmpty()) {
                        TODO()
                    } else {
                        mAdapter.items = list
                        if (mBinding.recycler.adapter !== mAdapter) {
                            mBinding.recycler.layoutManager = LinearLayoutManager(this)
                            mBinding.recycler.adapter = mAdapter
                        }
                    }
                }
        //TODO onError
        )
        //init click listeners
        ItemClickSupport.addTo(mBinding.recycler)
                .setOnItemClickListener(this)
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu!!
        menuInflater.inflate(R.menu.play_all, menu)
        menuInflater.inflate(R.menu.add_to_queue, menu)
        menuInflater.inflate(R.menu.folder_sort_by, menu)
//        menuInflater.inflate(R.menu.view_as, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item!!
        return when (item.itemId) {
            R.id.menu_play_all -> {
                TODO()
            }
            R.id.menu_add_to_queue -> {
                TODO()
            }
            R.id.menu_sort_by_az -> {
                mLoader.comparator = AscendingCompare
                mLoader.requestNext()
                true //ret
            }
            R.id.menu_sort_by_za -> {
                mLoader.comparator = DecendingCompare
                mLoader.requestNext()
                true //ret
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}

