package org.opensilk.video.phone

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
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import dagger.android.support.DaggerApplication
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.opensilk.dagger2.InjectionManager
import org.opensilk.dagger2.ForApp
import org.opensilk.logging.installLogging
import org.opensilk.media.database.MediaProviderModule
import org.opensilk.media.loader.cds.UpnpBrowseLoaderModule
import org.opensilk.media.loader.doc.DocumentLoaderModule
import org.opensilk.media.loader.storage.StorageLoaderModule
import org.opensilk.video.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(
        RootModule::class,
        ObserverHolderServiceModule::class,
        AppJobServiceModule::class,
        MediaProviderModule::class,
        VideoAppProviderModule::class,
        LookupConfigModule::class,
        UpnpBrowseLoaderModule::class,
        DocumentLoaderModule::class,
        StorageLoaderModule::class,
        ViewModelModule::class,
        DrawerActivityViewModelModule::class,
        VideoGlideLibraryModule::class,
        HomeScreenModule::class,
        FolderScreenModule::class,
        DetailScreenModule::class,
        AndroidSupportInjectionModule::class
))
interface RootComponent: AndroidInjector<VideoApp> {
    @Component.Builder
    abstract class Builder {
        @BindsInstance
        abstract fun context(@ForApp context: Context): Builder
        abstract fun build(): RootComponent
    }
}

@Module
object RootModule {

    @Provides @Named("VideoDatabaseAuthority") @JvmStatic
    fun databaseAuthority(@ForApp context: Context): String = context.getString(R.string.videos_authority)

    @Provides @Named("MediaDatabaseAuthority") @JvmStatic
    fun mediaDatabaseAuthority(@ForApp context: Context): String = context.getString(R.string.media_authority)

    @Provides @Singleton @JvmStatic
    fun provideOkHttpClient(@ForApp context: Context): OkHttpClient =
            OkHttpClient.Builder()
                    .cache(Cache(context.suitableCacheDir("okhttp3"), (50 * 1024 * 1024).toLong()))
                    .build()

    @Provides @JvmStatic
    fun provideContentResolver(@ForApp context: Context): ContentResolver = context.contentResolver

}

/**
 * Created by drew on 8/6/17.
 */
open class VideoApp: DaggerApplication(),
        InjectionManager, ViewModelProvider.Factory {

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> =
            DaggerRootComponent.builder().context(this).build()

    override fun onCreate() {
        super.onCreate()
        installLogging(true)
        startUpnpService()
    }

    open fun startUpnpService() {
        //Start upnp service
        startService(Intent(this, ObserverHolderService::class.java))
    }

    @Inject lateinit var mCommonGlideBuilder: VideoGlideLibraryComponent.Builder

    override fun injectFoo(foo: Any) {
        when (foo) {
            is VideoGlideLibrary -> mCommonGlideBuilder.create(foo).inject(foo)
            else -> TODO("No builder for ${foo.javaClass.name}")
        }
    }

    @Inject lateinit var mViewModelFactory: ViewModelFactoryFactory

    override fun <T : ViewModel> create(modelClass: Class<T>): T =
            mViewModelFactory.create(modelClass)

}

@GlideModule
@Excludes(OkHttpLibraryGlideModule::class)
class GlideConfig: AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val cacheDir = context.suitableCacheDir("glide4")
        builder.setDiskCache(DiskLruCacheFactory({ cacheDir }, 512 * 1024 * 1024))
    }

    override fun isManifestParsingEnabled(): Boolean = false
}
