package org.opensilk.video.telly

import android.content.Context
import android.support.v4.view.ViewCompat
import dagger.Component
import dagger.Module
import org.opensilk.common.app.BaseApp
import org.opensilk.common.dagger.AppContextComponent
import org.opensilk.common.dagger.AppContextModule
import org.opensilk.common.dagger.getDaggerComponent
import org.opensilk.upnp.cds.browser.CDSUpnpService
import javax.inject.Singleton

/**
 * Created by drew on 5/28/17.
 */
@Singleton
@Component(
        modules = arrayOf(RootModule::class, AppContextModule::class)
)
interface RootComponent: AppContextComponent {
    fun upnpService(): CDSUpnpService
}

/**
 *
 */
@Module
class RootModule

/**
 *
 */
fun Context.rootComponent(): RootComponent {
    return applicationContext.getDaggerComponent<RootComponent>()
}

/**
 *
 */
class VideoApp: BaseApp() {
    override val rootComponent: RootComponent by lazy {
        DaggerRootComponent.builder().appContextModule(AppContextModule(this)).build()
    }

    override fun onCreate() {
        super.onCreate()
        setupTimber(true, {})
    }
}
