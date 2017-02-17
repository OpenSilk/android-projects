package org.opensilk.music

import org.opensilk.common.app.BaseApp
import org.opensilk.common.dagger2.AppContextModule

/**
 * Created by drew on 6/28/16.
 */
class MusicApp(): BaseApp() {
    override val rootComponent: Any by lazy {
        DaggerRootComponent.builder().appContextModule(AppContextModule(this)).build()
    }

    override fun onCreate() {
        super.onCreate()
        setupTimber(true)
    }
}