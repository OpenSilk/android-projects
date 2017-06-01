package org.opensilk.video.telly

import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.media.browse.MediaBrowser
import android.support.v4.view.ViewCompat
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.internal.ReferenceReleasingProviderManager
import dagger.multibindings.Multibinds
import mortar.MortarScope
import mortar.Scoped
import org.opensilk.common.app.BaseApp
import org.opensilk.common.dagger.*
import org.opensilk.common.loader.RxListLoader
import org.opensilk.common.loader.RxLoader
import org.opensilk.common.mortar.HasScope
import org.opensilk.upnp.cds.browser.CDSUpnpService
import java.lang.ref.SoftReference
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Created by drew on 5/28/17.
 */
@Singleton
@Component(
        modules = arrayOf(RootModule::class,
                AppContextModule::class,
                UpnpHolderServiceModule::class,
                HomeModule::class
        )
)
interface RootComponent: AppContextComponent, Injector<VideoApp>

/**
 *
 */
@Module
abstract class RootModule

/**
 *
 */
fun Context.rootComponent(): RootComponent {
    return applicationContext.getDaggerComponent<RootComponent>()
}

/**
 * This class is overridden in the mock build variant, changes here will not be seen by espresso tests!
 */
open class VideoApp: BaseApp(), InjectionManager {

    override val rootComponent: Any by lazy {
        DaggerRootComponent.builder().appContextModule(AppContextModule(this)).build()
    }

    override fun onCreate() {
        super.onCreate()
        rootComponent().inject(this)
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
    @Inject lateinit var mUpnpHolderBuilder: UpnpHolderServiceComponent.Builder

    /**
     * Anything that is injectable needs to be injected here.
     * They should not inject themselves, that's not how dependency injection
     * is supposed to work.
     */
    override fun injectFoo(foo: Any) {
        if (foo is HomeFragment) {
            val act = foo.activity as HomeActivity
            val comp: Injector<HomeFragment> = if (act.hasDaggerComponent()) {
                act.daggerComponent()
            } else {
                act.setDaggerComponent(mHomeBuilder.build())
            }
            comp.inject(foo)
        } else if (foo is FolderFragment) {
            val act = foo.activity as FolderActivity
            val comp: Injector<FolderFragment> = if (act.hasDaggerComponent()) {
                act.daggerComponent()
            } else {
                val mediaItem: MediaBrowser.MediaItem = act.intent.getParcelableExtra(EXTRA_MEDIAITEM)
                act.setDaggerComponent(mFolderBuilder.mediaItem(mediaItem).build())
            }
            comp.inject(foo)
        } else if (foo is UpnpHolderService) {
            mUpnpHolderBuilder.build().inject(foo)
        } else {
            TODO("Don't have an injector for ${foo.javaClass}")
        }
    }

}

fun BaseVideoActivity.hasDaggerComponent(): Boolean {
    val ref = scope.getService<DaggerServiceReference>(DAGGER_SERVICE)
    return ref.cmp != null
}

fun <T> BaseVideoActivity.setDaggerComponent(bob: Injector<T>): Injector<T> {
    val ref = scope.getService<DaggerServiceReference>(DAGGER_SERVICE)
    ref.cmp = bob
    return bob
}

@Suppress("UNCHECKED_CAST")
fun <T> BaseVideoActivity.daggerComponent(): Injector<T> {
    return scope.getService<DaggerServiceReference>(DAGGER_SERVICE).cmp as Injector<T>
}

/**
 * holds the component in activies dagger service
 * set the activityComponent to this in all activities
 */
class DaggerServiceReference {
    var cmp: Injector<*>? = null
}