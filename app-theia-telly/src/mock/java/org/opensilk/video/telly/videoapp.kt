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
abstract class MockRootModule

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
            (foo.activity as HomeActivity).daggerComponent(mMockHomeBuilder, foo).inject(foo)
        } else if (foo is FolderFragment) {
            (foo.activity as FolderActivity).daggerComponent(mMockFolderBuilder, foo).inject(foo)
        } else if (foo is DetailFragment) {
            (foo.activity as DetailActivity).daggerComponent(mMockDetailBuilder, foo).inject(foo)
        } else {
            super.injectFoo(foo)
        }
    }
}