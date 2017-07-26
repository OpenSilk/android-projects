package org.opensilk.tvdb.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Created by drew on 7/25/17.
 */
class LanguageInterceptor(val language: String = "en"): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val mangledRequest = chain.request().newBuilder()
                .addHeader("Accept-Language", language)
                .build()
        return chain.proceed(mangledRequest)
    }
}