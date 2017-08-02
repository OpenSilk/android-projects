package org.opensilk.video

import android.content.Context
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.LibraryGlideModule
import timber.log.Timber

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