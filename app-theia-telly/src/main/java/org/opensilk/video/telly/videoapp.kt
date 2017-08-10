package org.opensilk.video.telly

import android.app.Application
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.Excludes
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpLibraryGlideModule
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.module.LibraryGlideModule
import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.opensilk.common.dagger.*
import org.opensilk.logging.installLogging
import org.opensilk.video.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Created by drew on 5/28/17.
 */
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
        VideoGlideLibraryModule::class,
        HomeModule::class,
        FolderModule::class,
        DetailModule::class,
        PlaybackModule::class
))
interface RootComponent: AppContextComponent, Injector<VideoApp>

/**
 *
 */
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
 * This class is overridden in the mock build variant, changes here will not be seen by espresso tests!
 */
open class VideoApp: Application(), InjectionManager, ViewModelProvider.Factory {

    open internal val rootComponent: RootComponent by lazy {
        DaggerRootComponent.builder().appContextModule(AppContextModule(this)).build()
    }
    private val injectOnce = Once()

    override fun onCreate() {
        super.onCreate()
        installLogging(true)

        startUpnpService()
    }

    open fun startUpnpService() {
        //Start upnp service
        startService(Intent(this, UpnpHolderService::class.java))
    }

    //For now we hold references to all our injectors here
    //this will go away when we switch over to dagger-android and AndroidInjection
    //but, for now, i cannot get the @IntoMap thing to fucking work on our Injector.Factory
    //i think the problem is kotlin and how it handles generics, but not really sure
    //if you are looking for something to waste time on, HomeActivityModule declares an
    //enable @IntoMap method that is supposed to provide an Injector.Factory but doesn't
    //@Inject lateinit var mInjectors: Map<Class<*>, Injector.Factory<*>>
    @Inject lateinit var mHomeBuilder: HomeComponent.Builder
    @Inject lateinit var mFolderBuilder: FolderComponent.Builder
    @Inject lateinit var mDetailBuilder: DetailComponent.Builder
    @Inject lateinit var mPlaybackBuilder: PlaybackComponent.Builder
    @Inject lateinit var mUpnpHolderBuilder: UpnpHolderServiceComponent.Builder
    @Inject lateinit var mDatabaseProviderBuilder: DatabaseProviderComponent.Builder
    @Inject lateinit var mAppJobServiceBuilder: AppJobServiceComponent.Builder
    @Inject lateinit var mCommonGlideBuilder: VideoGlideLibraryComponent.Builder

    /**
     * Anything that is injectable needs to be injected here.
     * They should not inject themselves, that's not how dependency injection
     * is supposed to work.
     */
    override fun injectFoo(foo: Any) {
        injectOnce.Do {
            rootComponent.inject(this)
        }
        if (foo is HomeFragment) {
            (foo.activity as HomeActivity).daggerComponent(mHomeBuilder, foo).inject(foo)
        } else if (foo is FolderFragment) {
            (foo.activity as FolderActivity).daggerComponent(mFolderBuilder, foo).inject(foo)
        } else if (foo is DetailFragment) {
            (foo.activity as DetailActivity).daggerComponent(mDetailBuilder, foo).inject(foo)
        } else if (foo is PlaybackActivity) {
            foo.daggerComponent(mPlaybackBuilder, foo).inject(foo)
        } else if (foo is UpnpHolderService) {
            mUpnpHolderBuilder.build().inject(foo)
        } else if (foo is DatabaseProvider) {
            mDatabaseProviderBuilder.build().inject(foo)
        } else if (foo is AppJobService) {
            mAppJobServiceBuilder.create(foo).inject(foo)
        } else if (foo is VideoGlideLibrary) {
            mCommonGlideBuilder.create(foo).inject(foo)
        } else {
            TODO("Don't have an injector for ${foo.javaClass}")
        }
    }

    @Inject lateinit var mViewModelFactory: ViewModelFactoryFactory

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return mViewModelFactory.create(modelClass)
    }
}

/**
 *
 */
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
