package org.opensilk.video

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.LibraryGlideModule
import okhttp3.CacheControl
import org.opensilk.common.dagger.getDaggerComponent
import timber.log.Timber
import java.io.InputStream

/**
 * Created by drew on 6/3/17.
 */
@GlideModule
class GlideConfig: LibraryGlideModule() {

    override fun registerComponents(context: Context, registry: Registry) {
        Timber.d("registerComponents()")
//        val appComponent: VideoAppComponent = context.getDaggerComponent()
//        val okClient = appComponent.okClient().newBuilder()
//                .addNetworkInterceptor({ chain ->
//                    //let glide store responses
//                    chain.proceed(chain.request().newBuilder()
//                            .cacheControl(CacheControl.Builder().noStore().noCache().build())
//                            .build())
//                }).build()
//        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(okClient))
    }
}