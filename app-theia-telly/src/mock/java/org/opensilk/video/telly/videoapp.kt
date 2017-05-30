package org.opensilk.video.telly

import dagger.Component
import dagger.Module
import org.opensilk.common.app.BaseApp
import org.opensilk.common.dagger.AppContextComponent
import org.opensilk.common.dagger.AppContextModule
import org.opensilk.upnp.cds.browser.CDSUpnpService
import javax.inject.Singleton

@Singleton
@Component(
        modules = arrayOf(RootModule::class, AppContextModule::class)
)
interface MockRootComponent: RootComponent

/**
 *
 */
@Module
class MockRootModule{

}

/**
 *
 */
class MockVideoApp: BaseApp() {
    override val rootComponent: Any by lazy {
        return@lazy ""
    }

    override fun onCreate() {
        super.onCreate()
        setupTimber(true, {})
    }
}