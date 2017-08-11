package org.opensilk.video.telly

import android.content.ContentResolver
import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import org.opensilk.dagger2.ForApp
import org.opensilk.video.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
        modules = arrayOf(
                MockRootModule::class,
                UpnpHolderServiceModule::class,
                AppJobServiceModule::class,
                MediaProviderModule::class,
                DatabaseProviderModule::class,
                LookupConfigModule::class,
                ViewModelModule::class,
                VideoGlideLibraryModule::class,
                HomeModule::class,
                FolderModule::class,
                DetailModule::class,
                PlaybackModule::class,
                MocksModule::class
        )
)
interface MockRootComponent: RootComponent {
    fun injectMockApp(app: MockVideoApp)
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
    @Provides @Named("DatabaseAuthority") @JvmStatic
    fun databaseAuthority(@ForApp context: Context): String = context.getString(R.string.videos_authority)

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
    override val rootComponent: MockRootComponent by lazy {
        DaggerMockRootComponent.builder().context(this).build()
    }
    private val injectOnce = Once()

    @Inject lateinit var mDatabaseClient: DatabaseClient

    override fun onCreate() {
        super.onCreate()
        injectOnce.Do {
            rootComponent.injectMockApp(this)
        }
    }

    override fun startUpnpService() {
        //stub
    }

}