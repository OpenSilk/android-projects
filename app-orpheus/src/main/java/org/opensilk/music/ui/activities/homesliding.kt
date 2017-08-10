package org.opensilk.music.ui.activities

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.music.R
import org.opensilk.music.RootComponent
import org.opensilk.music.data.DataService
import org.opensilk.music.ui.recycler.MediaItemAdapter
import timber.log.Timber
import javax.inject.Inject

/**
 *
 */
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

/**
 *
 */
@dagger.Module(
        includes = arrayOf(
                BaseModule::class
        )
)
class HomeModule

/**
 *
 */
fun HomeSlidingActivity.buildComponent(): Lazy<HomeComponent> {
    return lazy {
        DaggerHomeComponent.builder().rootComponent(this.getRootComponent()).build()
    }
}

/**
 *
 */
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

/**
 *
 */
class HomeSlidingActivity : DrawerSlidingActivity() {

    override val selfNavActionId: Int = R.id.nav_folders_root

    override val activityComponent: HomeComponent by buildComponent()

    override fun injectSelf() {
        activityComponent.inject(this)
    }

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

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.add_root, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
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

}
