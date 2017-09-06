package org.opensilk.music

import android.content.Context
import dagger.*
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.AndroidSupportInjectionModule
import dagger.android.support.DaggerApplication
import org.opensilk.dagger2.ForApp
import org.opensilk.logging.DebugTreeWithThreadName
import org.opensilk.music.ui.DetailModule
import org.opensilk.music.ui.FolderModule
import org.opensilk.music.ui.HomeModule
import org.opensilk.music.viewmodel.ViewModelModule
import timber.log.Timber
import javax.inject.Named
import javax.inject.Singleton

/**
 * Created by drew on 6/28/16.
 */
@Singleton
@Component(modules = arrayOf(
        RootModule::class,
        AndroidSupportInjectionModule::class,
        ViewModelModule::class,
        FolderModule::class,
        DetailModule::class,
        HomeModule::class
))
interface RootComponent: AndroidInjector<MusicApp> {
    @Component.Builder
    abstract class Builder {
        @BindsInstance
        abstract fun context(@ForApp context: Context): Builder
        abstract fun build(): RootComponent
    }
}

/**
 *
 */
@Module
class RootModule {
    @Provides @Named("music_authority")
    fun provideMusicAuthority(@ForApp context: Context): String =
            context.getString(R.string.music_provider)
}

/**
 *
 */
open class MusicApp: DaggerApplication() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(DebugTreeWithThreadName)
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> =
            DaggerRootComponent.builder().context(this).build()
}

