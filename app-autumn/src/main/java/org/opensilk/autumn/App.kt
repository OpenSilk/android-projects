package org.opensilk.autumn

import android.app.Application
import android.arch.lifecycle.*
import android.arch.persistence.room.Room
import android.content.Context
import android.content.SharedPreferences
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.opensilk.dagger2.ForApp
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
        DreamViewModelModule::class
))
interface AppComponent {
    fun inject(app: App)
    @Component.Builder
    abstract class Builder {
        @BindsInstance
        abstract fun context(@ForApp context: Context): Builder
        abstract fun build(): AppComponent
    }
}

@Module
object AppModule {
    @Provides @Singleton @JvmStatic
    fun provideDatabase(@ForApp context: Context): PlaylistDatabase =
            Room.databaseBuilder(context, PlaylistDatabase::class.java, "autumn-database")
                    .allowMainThreadQueries().build() //allow main thread because bug in roomDatabase

    @Provides @Singleton @JvmStatic
    fun providePlaylistDao(database: PlaylistDatabase): PlaylistDao =
            database.playlistDoa()

    @Provides @Singleton @JvmStatic
    fun provideOkhttp(): OkHttpClient =
            OkHttpClient.Builder().build()

    @Provides @Named("api_root") @JvmStatic
    fun proviedApiRoot(): String =
            "http://a1.phobos.apple.com/us/r1000/000/Features/atv/AutumnResources/videos/"

    @Provides @Singleton @JvmStatic
    fun provideNetworkApi(okHttpClient: OkHttpClient, @Named("api_root") apiRoot: String): NetworkApi =
            Retrofit.Builder()
                    .baseUrl(HttpUrl.parse(apiRoot))
                    .client(okHttpClient)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build().create(NetworkApi::class.java)

    @Provides @Singleton @JvmStatic
    fun providePreferences(@ForApp context: Context) : SharedPreferences =
            context.getSharedPreferences("autumn-prefs", Context.MODE_PRIVATE)

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

    override fun <T : ViewModel> create(modelClass: Class<T>): T =
            mViewModelFactory.create(modelClass)

    private val component: AppComponent by lazy {
        DaggerAppComponent.builder().context(this).build()
    }
}

fun <T: ViewModel> LifecycleActivity.fetchViewModel(clazz: KClass<T>): T {
    val vm = ViewModelProviders.of(this, (application as ViewModelProvider.Factory)).get(clazz.java)
    if (vm is LifecycleObserver) {
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