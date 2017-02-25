package org.opensilk.music

import android.content.Context
import dagger.Component
import org.opensilk.common.dagger.AppContextComponent
import org.opensilk.common.dagger.AppContextModule
import org.opensilk.common.dagger2.getDaggerComponent
import org.opensilk.music.data.DataService
import org.opensilk.music.data.MusicAuthorityModule
import javax.inject.Singleton

/**
 * Created by drew on 6/28/16.
 */
@Singleton
@Component(
        modules = arrayOf(
                AppContextModule::class,
                MusicAuthorityModule::class
        )
)
interface RootComponent: AppContextComponent {
    fun getDataService(): DataService
}

fun Context.getRootComponent(): RootComponent {
    return getDaggerComponent(this.applicationContext)
}