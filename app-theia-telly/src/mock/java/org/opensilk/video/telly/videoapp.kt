package org.opensilk.video.telly

import android.media.browse.MediaBrowser
import dagger.Component
import dagger.Module
import dagger.Provides
import org.opensilk.common.app.BaseApp
import org.opensilk.common.dagger.AppContextComponent
import org.opensilk.common.dagger.AppContextModule
import org.opensilk.common.dagger.InjectionManager
import org.opensilk.common.dagger.Injector
import org.opensilk.common.loader.RxLoader
import org.opensilk.upnp.cds.browser.CDSUpnpService
import rx.Observable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
        modules = arrayOf(MockRootModule::class,
                AppContextModule::class,
                UpnpHolderServiceModule::class,
                HomeModule::class,
                MockHomeModule::class,
                FolderModule::class,
                MockFolderModule::class,
                DetailModule::class,
                MockDetailModule::class
                )
)
interface MockRootComponent: RootComponent {
    fun injectMockApp(app: MockVideoApp)
}

/**
 *
 */
@Module
class MockRootModule

/**
 *
 */
class MockVideoApp: VideoApp() {
    override val rootComponent: Any by lazy {
        DaggerMockRootComponent.builder().appContextModule(AppContextModule(this)).build()
    }

    override fun onCreate() {
        super.onCreate()
        (rootComponent as MockRootComponent).injectMockApp(this)
    }

    @Inject lateinit var mMockHomeBuilder: MockHomeComponent.Builder
    @Inject lateinit var mMockFolderBuilder: MockFolderComponent.Builder
    @Inject lateinit var mMockDetailBuilder: MockDetailComponent.Builder

    override fun injectFoo(foo: Any) {
        if (foo is HomeFragment) {
            val act = foo.activity as HomeActivity
            val comp: Injector<HomeFragment> = if (act.hasDaggerComponent()) {
                act.daggerComponent()
            } else {
                act.setDaggerComponent(mMockHomeBuilder.build())
            }
            comp.inject(foo)
        } else if (foo is FolderFragment) {
            val act = foo.activity as FolderActivity
            val comp: Injector<FolderFragment> = if (act.hasDaggerComponent()) {
                act.daggerComponent()
            } else {
                val mediaItem: MediaBrowser.MediaItem = act.intent.getParcelableExtra(EXTRA_MEDIAITEM)
                act.setDaggerComponent(mMockFolderBuilder.mediaItem(mediaItem).build())
            }
            comp.inject(foo)
        } else if (foo is DetailFragment) {
            val act = foo.activity as DetailActivity
            val comp: Injector<DetailFragment> = if (act.hasDaggerComponent()) {
                act.daggerComponent()
            } else {
                val mediaItem: MediaBrowser.MediaItem = act.intent.getParcelableExtra(EXTRA_MEDIAITEM)
                act.setDaggerComponent(mMockDetailBuilder.mediaItem(mediaItem).build())
            }
            comp.inject(foo)
        } else {
            super.injectFoo(foo)
        }
    }
}