package org.opensilk.media.database

import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by drew on 8/11/17.
 */
@Module
abstract class MediaProviderModule {
    @ContributesAndroidInjector
    abstract fun injector(): MediaProvider
}