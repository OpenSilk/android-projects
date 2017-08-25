package org.opensilk.media.loader.storage

import dagger.Binds
import dagger.Module

/**
 * Created by drew on 8/20/17.
 */
@Module
abstract class StorageLoaderModule {
    @Binds
    abstract fun storageDeviceLoader(impl: StorageDeviceLoaderImpl): StorageDeviceLoader
    @Binds
    abstract fun storageLoader(impl: StorageLoaderImpl): StorageLoader
}