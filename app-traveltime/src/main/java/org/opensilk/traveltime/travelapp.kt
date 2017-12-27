package org.opensilk.traveltime

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import dagger.android.support.DaggerApplication
import org.opensilk.dagger2.ForApp
import javax.inject.Singleton

/**
 * Created by drew on 12/24/17.
 */
@Component(
        modules = arrayOf(
                AndroidSupportInjectionModule::class,
                AppModule::class
        )
)
@Singleton
interface AppComponent: AndroidInjector<App> {
    @Component.Builder
    abstract class Builder {
        @BindsInstance abstract fun appContext(@ForApp context: Context): Builder
        abstract fun build(): AppComponent
    }
}

@Module
object AppModule {

}

class App: DaggerApplication() {
    override fun applicationInjector(): AndroidInjector<out DaggerApplication> =
            DaggerAppComponent.builder().appContext(this).build()
}