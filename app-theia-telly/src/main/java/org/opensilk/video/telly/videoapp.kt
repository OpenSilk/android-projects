package org.opensilk.video.telly

import android.app.Application
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.module.AppGlideModule
import dagger.*
import org.opensilk.common.dagger.*
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
@Component(
        modules = arrayOf(RootModule::class,
                AppContextModule::class,
                UpnpHolderServiceModule::class,
                UpnpLoadersModule::class,
                HomeModule::class,
                FolderModule::class,
                DetailModule::class,
                PlaybackModule::class,
                PlaybackServiceModule::class,
                MediaProviderModule::class,
                DatabaseProviderModule::class,
                LookupModule::class
        )
)
interface RootComponent: AppContextComponent, Injector<VideoApp>

/**
 *
 */
@Module
object RootModule {
    @Provides @Named("DatabaseAuthority") @JvmStatic
    fun databaseAuthority(@ForApplication context: Context) = context.getString(R.string.videos_authority)
}

/**
 * This class is overridden in the mock build variant, changes here will not be seen by espresso tests!
 */
open class VideoApp: Application(), InjectionManager, ViewModelProvider.Factory {

    open val rootComponent: RootComponent by lazy {
        DaggerRootComponent.builder().appContextModule(AppContextModule(this)).build()
    }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(DebugTreeWithThreadName())

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
    @Inject lateinit var mPlaybackServiceBuilder: PlaybackServiceComponent.Builder
    @Inject lateinit var mDatabaseProviderBuilder: DatabaseProviderComponent.Builder

    /**
     * Anything that is injectable needs to be injected here.
     * They should not inject themselves, that's not how dependency injection
     * is supposed to work.
     */
    override fun injectFoo(foo: Any) {
        rootComponent.inject(this)
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
        } else if (foo is PlaybackService) {
            mPlaybackServiceBuilder.build().inject(foo)
        } else if (foo is DatabaseProvider) {
            mDatabaseProviderBuilder.build().inject(foo)
        } else {
            TODO("Don't have an injector for ${foo.javaClass}")
        }
    }

    @Inject lateinit var mViewModelFactory: AppViewModelFactory

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return mViewModelFactory.create(modelClass)
    }
}

/**
 *
 */
@GlideModule
class GlideConfig: AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val cacheDir = context.suitableCacheDir("glide4")
        builder.setDiskCache(DiskLruCacheFactory({ cacheDir }, 512 * 1024 * 1024))
    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}

/**
 *
 */
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

@Singleton
class AppViewModelFactory
@Inject
constructor(
        val providersMap: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
): ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (providersMap.containsKey(modelClass)) {
            return providersMap[modelClass]!!.get() as T
        }
        throw IllegalArgumentException("No factory for $modelClass")
    }
}
