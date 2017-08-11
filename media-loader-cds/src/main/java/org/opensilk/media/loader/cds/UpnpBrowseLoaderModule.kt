package org.opensilk.media.loader.cds

import dagger.Binds
import dagger.Module

/**
 * Provides the UpnpBrowseLoader, allows mocking for tests
 */
@Module
abstract class UpnpBrowseLoaderModule {
    @Binds
    abstract fun upnpBrowserLoader(impl: UpnpBrowseLoaderImpl): UpnpBrowseLoader
}