package org.opensilk.music

import org.opensilk.common.app.BaseApp
import org.opensilk.common.dagger2.AppContextModule
import org.opensilk.common.dagger2.DaggerAppContextComponent

/**
 * Created by drew on 6/27/16.
 */
class TestApp : BaseApp() {
    override val rootComponent: Any by lazy {
        DaggerAppContextComponent.builder().appContextModule(AppContextModule(this)).build()
    }
}
