package org.opensilk.video

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

/**
 * Created by drew on 8/6/17.
 */
@Module
abstract class ViewModelModule {
    @Binds @IntoMap @ViewModelKey(DetailViewModel::class)
    abstract fun detailViewModel(vm: DetailViewModel): ViewModel
    @Binds @IntoMap @ViewModelKey(FolderViewModel::class)
    abstract fun folderViewModel(vm: FolderViewModel): ViewModel
    @Binds @IntoMap @ViewModelKey(HomeViewModel::class)
    abstract fun homeViewModel(vm: HomeViewModel): ViewModel
    @Binds @IntoMap @ViewModelKey(PlaybackViewModel::class)
    abstract fun bindPlaybackViewModel(viewModel: PlaybackViewModel): ViewModel
}