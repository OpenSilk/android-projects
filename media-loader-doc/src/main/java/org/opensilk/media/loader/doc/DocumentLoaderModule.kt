package org.opensilk.media.loader.doc

import dagger.Binds
import dagger.Module

/**
 * Provides the document loader
 *
 * Created by drew on 8/10/17.
 */
@Module
abstract class DocumentLoaderModule {
    @Binds
    abstract fun documentLoader(impl: DocumentLoaderImpl): DocumentLoader
}