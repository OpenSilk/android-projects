package org.opensilk.music.viewmodel

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

/**
 * Created by drew on 9/5/17.
 */
@Module
abstract class ViewModelModule {
    @Binds @IntoMap @ViewModelKey(FolderViewModel::class)
    abstract fun folder(impl: FolderViewModel): ViewModel
    @Binds @IntoMap @ViewModelKey(PlayingViewModel::class)
    abstract fun playing(impl: PlayingViewModel): ViewModel
}