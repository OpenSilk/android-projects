package org.opensilk.autumn

import android.app.Application
import android.arch.lifecycle.*
import android.arch.persistence.room.Room
import android.content.Context
import android.content.SharedPreferences
import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.opensilk.common.dagger.AppContextComponent
import org.opensilk.common.dagger.AppContextModule
import org.opensilk.common.dagger.ForApplication
import org.opensilk.common.dagger.Injector
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
@Component(modules = arrayOf(
        AppModule::class,
        AppContextModule::class,
        DreamViewModelModule::class
))
interface AppComponent: AppContextComponent, Injector<App>

@Module
object AppModule {
    @Provides @Singleton @JvmStatic
    fun provideDatabase(@ForApplication context: Context): PlaylistDatabase {
        return Room.databaseBuilder(context, PlaylistDatabase::class.java, "autumn-database")
                .allowMainThreadQueries().build() //allow main thread because bug in roomDatabase
    }
    @Provides @Singleton @JvmStatic
    fun providePlaylistDao(database: PlaylistDatabase): PlaylistDao {
        return database.playlistDoa()
    }
    @Provides @Singleton @JvmStatic
    fun provideOkhttp(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }
    @Provides @Named("api_root") @JvmStatic
    fun proviedApiRoot(): String {
        return "http://a1.phobos.apple.com/us/r1000/000/Features/atv/AutumnResources/videos/"
    }
    @Provides @Singleton @JvmStatic
    fun provideNetworkApi(okHttpClient: OkHttpClient, @Named("api_root") apiRoot: String): NetworkApi {
        return Retrofit.Builder()
                .baseUrl(HttpUrl.parse(apiRoot))
                .client(okHttpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .build().create(NetworkApi::class.java)
    }
    @Provides @Singleton @JvmStatic
    fun providePreferences(@ForApplication context: Context) : SharedPreferences {
        return context.getSharedPreferences("autumn-prefs", Context.MODE_PRIVATE)
    }
}

/**
 * Created by drew on 8/4/17.
 */
class App: Application(), ViewModelProvider.Factory {

    @Inject lateinit var mViewModelFactory: AppViewModelFactory

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        component.inject(this)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return mViewModelFactory.create(modelClass)
    }

    private val component: AppComponent by lazy {
        DaggerAppComponent.builder().appContextModule(AppContextModule(this)).build()
    }
}

fun <T: ViewModel> LifecycleActivity.fetchViewModel(clazz: KClass<T>): T {
    val vm = ViewModelProviders.of(this, (application as ViewModelProvider.Factory)).get(clazz.java)
    if (this is LifecycleRegistryOwner && vm is LifecycleObserver) {
        this.lifecycle.addObserver(vm)
    }
    return vm
}

@Singleton
class AppViewModelFactory
@Inject constructor(
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