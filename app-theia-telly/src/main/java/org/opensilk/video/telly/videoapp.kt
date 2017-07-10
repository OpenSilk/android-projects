package org.opensilk.video.telly

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.module.AppGlideModule
import dagger.Component
import dagger.Module
import org.opensilk.common.app.BaseApp
import org.opensilk.common.dagger.*
import org.opensilk.video.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by drew on 5/28/17.
 */
@Singleton
@Component(
        modules = arrayOf(RootModule::class,
                AppContextModule::class,
                UpnpHolderServiceModule::class,
                HomeModule::class,
                FolderModule::class,
                DetailModule::class,
                PlaybackModule::class,
                PlaybackServiceModule::class,
                ProviderModule::class
        )
)
interface RootComponent: AppContextComponent, Injector<VideoApp>

/**
 *
 */
@Module
abstract class RootModule

/**
 * This class is overridden in the mock build variant, changes here will not be seen by espresso tests!
 */
open class VideoApp: BaseApp(), InjectionManager {

    override val rootComponent: RootComponent by lazy {
        DaggerRootComponent.builder().appContextModule(AppContextModule(this)).build()
    }

    override fun onCreate() {
        super.onCreate()
        rootComponent.inject(this)
        setupTimber(true, {})
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

    /**
     * Anything that is injectable needs to be injected here.
     * They should not inject themselves, that's not how dependency injection
     * is supposed to work.
     */
    override fun injectFoo(foo: Any) {
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
        } else {
            TODO("Don't have an injector for ${foo.javaClass}")
        }
    }

}

@Suppress("UNCHECKED_CAST")
fun <T> BaseVideoActivity.daggerComponent(bob: Injector.Factory<T>, foo: T): Injector<T> {
    val ref = scope.getService<DaggerServiceReference>(DAGGER_SERVICE)
    if (ref.cmp == null) {
        ref.cmp = bob.create(foo)
    }
    return ref.cmp as Injector<T>
}

/**
 * holds the component in activies dagger service
 * set the activityComponent to this in all activities
 */
class DaggerServiceReference {
    var cmp: Injector<*>? = null
}

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