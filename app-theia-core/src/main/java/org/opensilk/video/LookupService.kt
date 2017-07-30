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
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.apache.commons.lang3.concurrent.TimedSemaphore
import org.opensilk.common.dagger.ForApplication
import org.opensilk.media.MediaMeta
import org.opensilk.tmdb.api.ApiKeyInterceptor
import org.opensilk.tmdb.api.TMDb
import org.opensilk.tvdb.api.LanguageInterceptor
import org.opensilk.tvdb.api.TVDb
import org.opensilk.tvdb.api.model.Auth
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import rx.Observable
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

const val LOOKUP_NAME = "lookup_name"
const val LOOKUP_YEAR = "lookup_year"
const val LOOKUP_SEASON_NUM = "lookup_season_num"
const val LOOKUP_EPISODE_NUM = "lookup_episode_num"

private const val WAIT_TIME: Long = 1000
private const val WAIT_USERS = 1

@Module
object LookupModule {
    @Provides @Singleton @JvmStatic
    fun provideOkHttpClient(@ForApplication context: Context): OkHttpClient {
        return OkHttpClient.Builder()
                .cache(Cache(context.suitableCacheDir("okhttp3"), (50 * 1024 * 1024).toLong()))
                .build()
    }
    @Provides @Singleton @JvmStatic
    fun provideTVDBAuth(): Auth {
        return Auth(BuildConfig.TVDB_API_KEY)
    }
    @Provides @Singleton @Named("tvdb_api_root") @JvmStatic
    fun provideTVDBApiRoot(): String {
        return "https://api.thetvdb.com/"
    }
    @Provides @Singleton @Named("tvdb_banner_root") @JvmStatic
    fun provideTVDBBannerRoot(): String {
        return "https://thetvdb.com/banners/"
    }
    @Provides @Singleton @JvmStatic
    fun provideTVDBApi(@Named("tvdb_api_root") apiRoot: String, okHttpClient: OkHttpClient): TVDb {
        return Retrofit.Builder()
                .baseUrl(HttpUrl.parse(apiRoot))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .validateEagerly(true)
                .client(okHttpClient.newBuilder().addInterceptor(LanguageInterceptor()).build())
                .build()
                .create(TVDb::class.java)
    }
    @Provides @Singleton @Named("moviedb_api_key") @JvmStatic
    fun provideMovieDbApiKey(): String {
        return BuildConfig.TMDB_API_KEY;
    }
    @Provides @Singleton @Named("moviedb_api_root") @JvmStatic
    fun provideMovieDBApiRoot(): String {
        return "https://api.themoviedb.org/3/"
    }
    @Provides @Singleton @JvmStatic
    fun provideMovieDB(@Named("moviedb_api_root") apiRoot: String,
                       @Named("moviedb_api_key") apiKey: String,
                       okHttpClient: OkHttpClient): TMDb {
        return Retrofit.Builder()
                .baseUrl(HttpUrl.parse(apiRoot))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .client(okHttpClient.newBuilder().addInterceptor(ApiKeyInterceptor(apiKey)).build())
                .build()
                .create(TMDb::class.java)
    }
}

interface LookupHandler {
    fun lookupObservable(meta: MediaMeta): Observable<MediaMeta>
}

class LookupException: Exception()
class IllegalMediaKindException: Exception()

/**
 * Created by drew on 4/11/16.
 */
@Singleton
class LookupService
@Inject constructor(
        private val mMovieDb: LookupMovieDb,
        private val mTVDb: LookupTVDb
): LookupHandler {

    companion object {
        private var sSemaphore = TimedSemaphore(WAIT_TIME, TimeUnit.MILLISECONDS, WAIT_USERS)

        //TODO actually handle 429 or whatever the try later code is
        //for now we just limit all our network calls to one per second
        @Synchronized fun waitTurn() {
            try {
                if (sSemaphore.isShutdown) {
                    sSemaphore = TimedSemaphore(WAIT_TIME, TimeUnit.MILLISECONDS, WAIT_USERS)
                }
                sSemaphore.acquire()
            } catch (e: InterruptedException) {
                Timber.w("Interrupted while waiting on semaphore")
            }
        }
    }

    override fun lookupObservable(meta: MediaMeta): Observable<MediaMeta> {
        val title = meta.displayName
        if (meta.extras.containsKey(LOOKUP_NAME)) {
            return if (meta.extras.containsKey(LOOKUP_SEASON_NUM)) {
                mTVDb.lookupObservable(meta)
            } else {
                mMovieDb.lookupObservable(meta)
            }
        } else if (matchesTvEpisode(title)) {
            val name = extractSeriesName(title)
            val seasonNum = extractSeasonNumber(title)
            val episodeNum = extractEpisodeNumber(title)
            if (name.isNullOrBlank() || seasonNum <= 0 || episodeNum <= 0) {
                return Observable.error(LookupException())
            }
            meta.extras.putString(LOOKUP_NAME, name)
            meta.extras.putInt(LOOKUP_SEASON_NUM, seasonNum)
            meta.extras.putInt(LOOKUP_EPISODE_NUM, episodeNum)
            return mTVDb.lookupObservable(meta)
        } else if (matchesMovie(title)) {
            val name = extractMovieName(title)
            val year = extractMovieYear(title)
            if (name.isNullOrBlank()) {
                return Observable.error(LookupException())
            }
            meta.extras.putString(LOOKUP_NAME, name)
            meta.extras.putString(LOOKUP_YEAR, year)
            return mMovieDb.lookupObservable(meta)
        } else {
            return Observable.error(LookupException())
        }
    }
}
