package org.opensilk.video.telly

import android.content.Context
import android.net.Uri
import dagger.Component
import dagger.Module
import dagger.Provides
import org.opensilk.common.dagger.AppContextModule
import org.opensilk.common.dagger.ForApplication
import org.opensilk.video.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
        modules = arrayOf(
                MockRootModule::class,
                AppContextModule::class,
                UpnpHolderServiceModule::class,
                MockUpnpLoadersModule::class,
                HomeModule::class,
                MockHomeModule::class,
                FolderModule::class,
                MockFolderModule::class,
                DetailModule::class,
                MockDetailModule::class,
                PlaybackModule::class,
                MockPlaybackModule::class,
                PlaybackServiceModule::class,
                MockMediaProviderClientModule::class,
                DatabaseProviderModule::class,
                LookupModule::class
                )
)
interface MockRootComponent: RootComponent {
    fun injectMockApp(app: MockVideoApp)
}

/**
 *
 */
@Module
object MockRootModule {
    @Provides @Named("DatabaseAuthority") @JvmStatic
    fun databaseAuthority(@ForApplication context: Context) = context.getString(R.string.videos_authority)
}

/**
 *
 */
class MockVideoApp: VideoApp() {
    override val rootComponent: MockRootComponent by lazy {
        DaggerMockRootComponent.builder().appContextModule(AppContextModule(this)).build()
    }
    private val injectOnce = Once()

    @Inject lateinit var mMockHomeBuilder: MockHomeComponent.Builder
    @Inject lateinit var mMockFolderBuilder: MockFolderComponent.Builder
    @Inject lateinit var mMockDetailBuilder: MockDetailComponent.Builder
    @Inject lateinit var mMockPlaybackBuilder: MockPlaybackComponent.Builder

    override fun injectFoo(foo: Any) {
        injectOnce.Do {
            rootComponent.injectMockApp(this)
        }
        if (foo is HomeFragment) {
            (foo.activity as HomeActivity).daggerComponent(mMockHomeBuilder, foo).inject(foo)
        } else if (foo is FolderFragment) {
            (foo.activity as FolderActivity).daggerComponent(mMockFolderBuilder, foo).inject(foo)
        } else if (foo is DetailFragment) {
            (foo.activity as DetailActivity).daggerComponent(mMockDetailBuilder, foo).inject(foo)
        } else if (foo is PlaybackActivity) {
            foo.daggerComponent(mMockPlaybackBuilder, foo).inject(foo)
        } else {
            super.injectFoo(foo)
        }
    }

    override fun startUpnpService() {
        //stub
    }
}