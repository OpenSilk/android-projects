package org.opensilk.video

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.LibraryGlideModule
import dagger.Module
import dagger.Subcomponent
import okhttp3.OkHttpClient
import org.opensilk.dagger2.Injector
import org.opensilk.dagger2.injectMe
import java.io.InputStream
import javax.inject.Inject

/**
 * Created by drew on 8/10/17.
 */
@Subcomponent
interface VideoGlideLibraryComponent: Injector<VideoGlideLibrary> {
    @Subcomponent.Builder
    abstract class Builder: Injector.Builder<VideoGlideLibrary>()
}

@Module(subcomponents = arrayOf(VideoGlideLibraryComponent::class))
abstract class VideoGlideLibraryModule

@GlideModule
class VideoGlideLibrary: LibraryGlideModule() {

    @Inject lateinit var mOkHttpClient: OkHttpClient

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        injectMe(context)
        //disable the cache, glide will handle
        val okClient = mOkHttpClient.newBuilder().cache(null).build()
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(okClient))
    }
}

