/*
 * Copyright (c) 2016 OpenSilk Productions LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.video

import android.content.Context
import android.util.Log

import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Priority
import com.bumptech.glide.Registry
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.module.GlideModule
import com.bumptech.glide.util.ContentLengthInputStream

import org.opensilk.video.util.Utils

import java.io.File
import java.io.IOException
import java.io.InputStream

import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.opensilk.common.dagger2.getDaggerComponent
import timber.log.Timber

/**
 * Created by drew on 4/10/16.
 */
class GlideConfig : GlideModule {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val cacheDir = Utils.getCacheDir(context, "glide4")
        builder.setDiskCache(DiskLruCacheFactory({ cacheDir }, 512 * 1024 * 1024))
    }

    override fun registerComponents(context: Context, registry: Registry) {
        Timber.d("registerComponents()")
        val appComponent = getDaggerComponent<VideoAppComponent>(context)
        val okClient = appComponent.okClient().newBuilder()
                .addNetworkInterceptor({ chain ->
                    //let glide store responses
                    chain.proceed(chain.request().newBuilder()
                            .cacheControl(CacheControl.Builder().noStore().noCache().build())
                            .build())
                }).build()
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(okClient))
    }

    /*
        Until updated for glide 4
     */

    class OkHttpUrlLoader(private val client: Call.Factory) : ModelLoader<GlideUrl, InputStream> {

        override fun handles(url: GlideUrl): Boolean {
            return true
        }

        override fun buildLoadData(model: GlideUrl, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
            return ModelLoader.LoadData(model, OkHttpStreamFetcher(client, model))
        }

        /**
         * The default factory for [OkHttpUrlLoader]s.
         */
        class Factory
        /**
         * Constructor for a new Factory that runs requests using given client.

         * @param client this is typically an instance of `OkHttpClient`.
         */
        @JvmOverloads constructor(
                private val client: Call.Factory = GlideConfig.OkHttpUrlLoader.Factory.internalClient
        ) : ModelLoaderFactory<GlideUrl, InputStream> {

            override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GlideUrl, InputStream> {
                return OkHttpUrlLoader(client)
            }

            override fun teardown() {
                // Do nothing, this instance doesn't own the client.
            }

            companion object {
                private val internalClient: Call.Factory by lazy {
                    return@lazy OkHttpClient()
                }
            }
        }
        /**
         * Constructor for a new Factory that runs requests using a static singleton client.
         */
    }

    class OkHttpStreamFetcher(private val client: Call.Factory, private val url: GlideUrl) : DataFetcher<InputStream> {
        private var stream: InputStream? = null
        private var responseBody: ResponseBody? = null
        @Volatile private var call: Call? = null

        override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
            val requestBuilder = Request.Builder().url(url.toStringUrl())
            for ((key, value) in url.headers) {
                requestBuilder.addHeader(key, value)
            }
            val request = requestBuilder.build()

            call = client.newCall(request)
            call!!.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "OkHttp failed to obtain result", e)
                    }
                    callback.onLoadFailed(e)
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val contentLength = response.body().contentLength()
                        responseBody = response.body()
                        stream = ContentLengthInputStream.obtain(responseBody!!.byteStream(), contentLength)
                    } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "OkHttp got error response: " + response.code() + ", " + response.message())
                    }
                    callback.onDataReady(stream)
                }
            })
        }

        override fun cleanup() {
            try {
                if (stream != null) {
                    stream!!.close()
                }
            } catch (e: IOException) {
                // Ignored
            }

            if (responseBody != null) {
                responseBody!!.close()
            }
        }

        override fun cancel() {
            val local = call
            local?.cancel()
        }

        override fun getDataClass(): Class<InputStream> {
            return InputStream::class.java
        }

        override fun getDataSource(): DataSource {
            return DataSource.REMOTE
        }

        companion object {
            private val TAG = "OkHttpFetcher"
        }
    }

}
