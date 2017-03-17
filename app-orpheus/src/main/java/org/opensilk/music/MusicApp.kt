package org.opensilk.music

import android.content.ComponentName
import android.content.Context
import dagger.Component
import dagger.Module
import dagger.Provides
import org.opensilk.common.app.BaseApp
import org.opensilk.common.dagger.AppContextComponent
import org.opensilk.common.dagger.AppContextModule
import org.opensilk.common.dagger.ForApplication
import org.opensilk.common.dagger2.getDaggerComponent
import org.opensilk.music.data.DataService
import org.opensilk.music.data.MusicAuthorityModule
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers
import javax.inject.Named
import javax.inject.Singleton

/**
 * Created by drew on 6/28/16.
 */
@Singleton
@Component(
        modules = arrayOf(
                AppContextModule::class,
                MusicAuthorityModule::class,
                RootModule::class
        )
)
interface RootComponent: AppContextComponent {
    fun getDataService(): DataService
    @Named("MainThread") fun getMainThreadObserveOn(): Scheduler
}

/**
 *
 */
@Module
class RootModule {
    @Provides @Named("MusicService")
    fun getMusicServiceComponent(@ForApplication context: Context): ComponentName {
        return ComponentName(context, "foo")
    }
    @Provides @Singleton @Named("MainThread")
    fun getObserveOnScheduler(): rx.Scheduler {
        return AndroidSchedulers.mainThread()
    }
}

/**
 *
 */
fun Context.getRootComponent(): RootComponent {
    return getDaggerComponent(this.applicationContext)
}

/**
 *
 */
class MusicApp: BaseApp() {
    override val rootComponent: Any by lazy {
        DaggerRootComponent.builder().appContextModule(AppContextModule(this)).build()
    }

    override fun onCreate() {
        super.onCreate()
        setupTimber(true)
    }
}