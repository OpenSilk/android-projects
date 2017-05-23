package org.opensilk.upnp.cds.browser

import dagger.Component
import dagger.Module
import org.opensilk.common.dagger.AppContextComponent
import org.opensilk.common.dagger.ProviderScope

/**
 * Created by drew on 5/19/17.
 */
@ProviderScope
@Component(
        dependencies = arrayOf(AppContextComponent::class),
        modules = arrayOf(CDSDocProviderModule::class)
)
interface CDSDocProviderComponent {
    fun inject(provider: CDSDocProvider)
}

@Module
class CDSDocProviderModule {

}