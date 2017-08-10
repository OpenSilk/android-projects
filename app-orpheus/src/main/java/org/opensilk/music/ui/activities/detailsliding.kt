package org.opensilk.music.ui.activities

import android.media.browse.MediaBrowser
import android.support.v7.widget.RecyclerView
import android.view.View
import dagger.Component
import dagger.Module
import dagger.Provides
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.lazyBindLayout
import org.opensilk.common.recycler.ItemClickSupport
import org.opensilk.music.R
import org.opensilk.music.RootComponent
import org.opensilk.music.databinding.ActivityDetailsBinding
import org.opensilk.music.databinding.SheetPlayingBinding
import javax.inject.Inject

/**
 * Created by drew on 5/21/17.
 */
@ActivityScope
@Component(
        dependencies = arrayOf(
                RootComponent::class
        ),
        modules = arrayOf(
                DetailModule::class
        )
)
interface DetailComponent: BaseComponent {
    fun inject(activity: DetailSlidingActivity)
}

/**
 *
 */
@Module(
        includes = arrayOf(
                BaseModule::class
        )
)
class DetailModule(val mediaItem: MediaBrowser.MediaItem) {
    @Provides @ActivityScope
    fun provideMediaItem(): MediaBrowser.MediaItem {
        return mediaItem
    }
}

@ActivityScope
class DetailLoader @Inject constructor(

): RxListLoader<DetailTile> {
    override val listObservable: Observable<List<DetailTile>>
        get() = TODO()
}

class DetailTile

/**
 *
 */
class DetailSlidingActivity : BaseSlidingActivity(), ItemClickSupport.OnItemClickListener {

    override val activityComponent: DetailComponent by lazy {
        val mediaItem: MediaBrowser.MediaItem = intent.getParcelableExtra(EXTRA_MEDIA_ITEM)
        return@lazy DaggerDetailComponent.builder().rootComponent(getRootComponent())
                .detailModule(DetailModule(mediaItem)).build()
    }
    private val mBinding: ActivityDetailsBinding by lazyBindLayout(R.layout.activity_details)
    override val mSheetBinding: SheetPlayingBinding
        get() = mBinding.playingSheet

    @Inject lateinit var mMediaItem: MediaBrowser.MediaItem

    override fun injectSelf() {
        activityComponent.inject(this)
    }

    override fun onItemClicked(recyclerView: RecyclerView?, position: Int, v: View?) {
        TODO("not implemented")
    }
}