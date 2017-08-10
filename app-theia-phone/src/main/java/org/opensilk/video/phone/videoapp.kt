package org.opensilk.video.phone

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
import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.opensilk.common.dagger.*
import org.opensilk.video.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(
        RootModule::class,
        AppContextModule::class,
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
        PlaybackScreenModule::class
))
interface RootComponent: AppContextComponent, Injector<VideoApp>

@Module
object RootModule {

    @Provides @Named("DatabaseAuthority") @JvmStatic
    fun databaseAuthority(@ForApplication context: Context): String {
        return context.getString(R.string.videos_authority)
    }

    @Provides @Singleton @JvmStatic
    fun provideOkHttpClient(@ForApplication context: Context): OkHttpClient {
        return OkHttpClient.Builder()
                .cache(Cache(context.suitableCacheDir("okhttp3"), (50 * 1024 * 1024).toLong()))
                .build()
    }

    @Provides @JvmStatic
    fun provideContentResolver(@ForApplication context: Context): ContentResolver {
        return context.contentResolver
    }

}

/**
 * Created by drew on 8/6/17.
 */
open class VideoApp: Application(), InjectionManager, ViewModelProvider.Factory {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(DebugTreeWithThreadName())
        enableStrictMode()

        startUpnpService()
    }

    open fun startUpnpService() {
        //Start upnp service
        startService(Intent(this, UpnpHolderService::class.java))
    }

    protected fun enableStrictMode() {
        val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().penaltyFlashScreen()
        StrictMode.setThreadPolicy(threadPolicyBuilder.build())

        val vmPolicyBuilder = StrictMode.VmPolicy.Builder().detectAll().penaltyLog()
        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }

    val injectOnce = Once()
    val rootComponent: RootComponent by lazy {
        DaggerRootComponent.builder().appContextModule(AppContextModule(this)).build()
    }

    @Inject lateinit var mHomeBuilder: HomeScreenComponent.Builder
    @Inject lateinit var mFolderBuilder: FolderScreenComponent.Builder
    @Inject lateinit var mDetailBuilder: DetailScreenComponent.Builder
    @Inject lateinit var mUpnpHolderBuilder: UpnpHolderServiceComponent.Builder
    @Inject lateinit var mDatabaseProviderBuilder: DatabaseProviderComponent.Builder
    @Inject lateinit var mAppJobServiceBuilder: AppJobServiceComponent.Builder
    @Inject lateinit var mCommonGlideBuilder: VideoGlideLibraryComponent.Builder

    override fun injectFoo(foo: Any): Any {
        injectOnce.Do { rootComponent.inject(this) }
        return if (foo is HomeActivity) {
            mHomeBuilder.create(foo).inject(foo)
        } else if (foo is FolderActivity) {
            mFolderBuilder.create(foo).inject(foo)
        } else if (foo is DetailActivity) {
            mDetailBuilder.create(foo).inject(foo)
        } else if (foo is UpnpHolderService) {
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
    }

    @Inject lateinit var mViewModelFactory: ViewModelFactoryFactory

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        injectOnce.Do { rootComponent.inject(this) }
        return mViewModelFactory.create(modelClass)
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