package org.opensilk.music

import android.app.Application
import android.content.Context
import dagger.Component
import dagger.Module
import dagger.Provides
import org.opensilk.common.dagger.AppContextComponent
import org.opensilk.common.dagger.AppContextModule
import org.opensilk.common.dagger.ForApplication
import timber.log.Timber
import javax.inject.Named
import javax.inject.Singleton

/**
 * Created by drew on 6/28/16.
 */
@Singleton
@Component(modules = arrayOf(
        AppContextModule::class,
        RootModule::class
))
interface RootComponent: AppContextComponent

/**
 *
 */
@Module
class RootModule {
    @Provides @Named("music_authority")
    fun provideMusicAuthority(@ForApplication context: Context): String {
        return context.getString(R.string.music_provider)
    }
}

/**
 *
 */
open class MusicApp: Application() {

    val rootComponent: RootComponent by lazy {
        DaggerRootComponent.builder().appContextModule(AppContextModule(this)).build()
    }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(DebugTreeWithThreadName())
    }

}

open class DebugTreeWithThreadName : Timber.DebugTree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, appendThreadName(message), t)
    }

    internal fun appendThreadName(msg: String): String {
        val threadName = Thread.currentThread().name
        if ("main" == threadName) {
            return msg
        }
        return "$msg [$threadName]"
    }
}