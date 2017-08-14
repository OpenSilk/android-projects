package org.opensilk.video.telly

import android.content.ContentResolver
import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import dagger.android.support.DaggerApplication
import okhttp3.OkHttpClient
import org.opensilk.dagger2.ForApp
import org.opensilk.media.database.MediaProviderModule
import org.opensilk.video.*
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
        modules = arrayOf(
                MockRootModule::class,
                UpnpHolderServiceModule::class,
                AppJobServiceModule::class,
                MediaProviderModule::class,
                VideoAppProviderModule::class,
                LookupConfigModule::class,
                ViewModelModule::class,
                VideoGlideLibraryModule::class,
                HomeScreenModule::class,
                FolderScreenModule::class,
                DetailScreenModule::class,
                AndroidSupportInjectionModule::class,
                MocksModule::class
        )
)
interface MockRootComponent: RootComponent {
    @Component.Builder
    abstract class Builder {
        @BindsInstance
        abstract fun context(@ForApp context: Context): Builder
        abstract fun build(): MockRootComponent
    }
}

/**
 *
 */
@Module
object MockRootModule {

    @Provides @Named("VideoDatabaseAuthority") @JvmStatic
    fun databaseAuthority(@ForApp context: Context): String = context.getString(R.string.videos_authority)

    @Provides @Named("MediaDatabaseAuthority") @JvmStatic
    fun mediaDatabaseAuthority(@ForApp context: Context): String {
        return context.getString(R.string.media_authority)
    }

    @Provides @Singleton @JvmStatic
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    @Provides @JvmStatic
    fun provideContentResolver(@ForApp context: Context): ContentResolver {
        return context.contentResolver
    }

}

/**
 *
 */
class MockVideoApp: VideoApp() {

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerMockRootComponent.builder().context(this).build()
    }

    override fun startUpnpService() {
        //stub
    }

}