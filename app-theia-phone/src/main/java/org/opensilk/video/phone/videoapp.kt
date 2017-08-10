package org.opensilk.video.phone

import android.app.Activity
import android.app.Application
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.StrictMode
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.Excludes
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpLibraryGlideModule
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.module.AppGlideModule
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.AndroidSupportInjectionModule
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.opensilk.common.dagger.*
import org.opensilk.dagger2.ForApp
import org.opensilk.logging.installLogging
import org.opensilk.video.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(
        RootModule::class,
        UpnpHolderServiceModule::class,
        AppJobServiceModule::class,
        MediaProviderModule::class,
        DatabaseProviderModule::class,
        LookupConfigModule::class,
        UpnpBrowseLoaderModule::class,
        ViewModelModule::class,
        DrawerActivityViewModelModule::class,
        VideoGlideLibraryModule::class,
        HomeScreenModule::class,
        FolderScreenModule::class,
        DetailScreenModule::class,
        AndroidSupportInjectionModule::class
))
interface RootComponent {
    fun inject(app: VideoApp)
    @Component.Builder
    abstract class Builder {
        @BindsInstance
        abstract fun context(@ForApp context: Context): Builder
        abstract fun build(): RootComponent
    }
}

@Module
object RootModule {

    @Provides @Named("DatabaseAuthority") @JvmStatic
    fun databaseAuthority(@ForApp context: Context): String {
        return context.getString(R.string.videos_authority)
    }

    @Provides @Singleton @JvmStatic
    fun provideOkHttpClient(@ForApp context: Context): OkHttpClient {
        return OkHttpClient.Builder()
                .cache(Cache(context.suitableCacheDir("okhttp3"), (50 * 1024 * 1024).toLong()))
                .build()
    }

    @Provides @JvmStatic
    fun provideContentResolver(@ForApp context: Context): ContentResolver {
        return context.contentResolver
    }

}

/**
 * Created by drew on 8/6/17.
 */
open class VideoApp: Application(), InjectionManager,
        ViewModelProvider.Factory, HasActivityInjector {

    val injectOnce = Once()
    val rootComponent: RootComponent by lazy {
        DaggerRootComponent.builder().context(this).build()
    }

    override fun onCreate() {
        injectOnce.Do { rootComponent.inject(this) }
        super.onCreate()
        installLogging(true)
        startUpnpService()
    }

    open fun startUpnpService() {
        //Start upnp service
        startService(Intent(this, UpnpHolderService::class.java))
    }

    @Inject lateinit var mUpnpHolderBuilder: UpnpHolderServiceComponent.Builder
    @Inject lateinit var mDatabaseProviderBuilder: DatabaseProviderComponent.Builder
    @Inject lateinit var mAppJobServiceBuilder: AppJobServiceComponent.Builder
    @Inject lateinit var mCommonGlideBuilder: VideoGlideLibraryComponent.Builder

    override fun injectFoo(foo: Any): Any {
        injectOnce.Do { rootComponent.inject(this) }
        if (foo is UpnpHolderService) {
            mUpnpHolderBuilder.create(foo).inject(foo)
        } else if (foo is DatabaseProvider) {
            mDatabaseProviderBuilder.create(foo).inject(foo)
        } else if (foo is AppJobService) {
            mAppJobServiceBuilder.create(foo).inject(foo)
        } else if (foo is VideoGlideLibrary) {
            mCommonGlideBuilder.create(foo).inject(foo)
        } else {
            TODO("No builder for ${foo::class}")
        }
        return Any()
    }

    @Inject lateinit var mViewModelFactory: ViewModelFactoryFactory

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return mViewModelFactory.create(modelClass)
    }

    @Inject lateinit var mActivityInjector: DispatchingAndroidInjector<Activity>

    override fun activityInjector(): AndroidInjector<Activity> {
        return mActivityInjector
    }

}

@GlideModule
@Excludes(OkHttpLibraryGlideModule::class)
class GlideConfig: AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val cacheDir = context.suitableCacheDir("glide4")
        builder.setDiskCache(DiskLruCacheFactory({ cacheDir }, 512 * 1024 * 1024))
    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}
