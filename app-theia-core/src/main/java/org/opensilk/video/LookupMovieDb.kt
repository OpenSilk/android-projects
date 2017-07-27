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

import org.opensilk.media.MediaMeta
import org.opensilk.media.UPNP_VIDEO
import org.opensilk.media.newMediaRef
import org.opensilk.tmdb.api.TMDb
import org.opensilk.tmdb.api.model.ImageList
import org.opensilk.tmdb.api.model.Movie
import org.opensilk.tmdb.api.model.TMDbConfig
import rx.Observable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by drew on 4/11/16.
 */
@Singleton
class LookupMovieDb
@Inject
constructor(
        internal val mClient: DatabaseClient,
        internal val mApi: TMDb
) : LookupHandler {

    class MovieWithImages(val movie: Movie, val images: ImageList)

    internal val mConfigObservable: Observable<TMDbConfig> by lazy {
        return@lazy mApi.configurationObservable()
                .doOnNext { config ->
                    Timber.d("Updating TMDB config")
                    mClient.setMovieImageBaseUrl(config.images.baseUrl)
                }.replay(1).autoConnect()
    }

    override fun lookupObservable(meta: MediaMeta): Observable<MediaMeta> {
        val mediaRef = newMediaRef(meta.mediaId)
        if (mediaRef.kind != UPNP_VIDEO) {
            return Observable.error(IllegalMediaKindException())
        }
        val name = meta.extras.getString(LOOKUP_NAME, "")
        if (name.isBlank()) {
            return Observable.error(IllegalArgumentException())
        }
        val year = meta.extras.getString(LOOKUP_YEAR, "")

        val cacheObservable = mClient.getMovieAssociation(name, year)
                .map { id -> mClient.uris.movie(id) }.toObservable()

        val networkObservable = Observable.defer {
            //do search
            LookupService.waitTurn()
            return@defer if (year.isBlank())
                mApi.searchMovieObservable(name, "en")
            else {
                mApi.searchMovieObservable(name, year, "en")
            }
        }.flatMap { list ->
            //stream movie list
            return@flatMap if (list.results == null) {
                Observable.empty()
            } else {
                Observable.from(list.results).take(5)
            }
        }.flatMap { movie ->
            //fetch movie and images
            return@flatMap Observable.defer {
                LookupService.waitTurn()
                return@defer Observable.zip(
                        mApi.movieObservable(movie.id, "en"),
                        mApi.movieImagesObservable(movie.id, "en"),
                        { m, i -> MovieWithImages(m, i) }
                ).map {
                    val uri = mClient.addMovie(it.movie)
                    mClient.addMovieImages(it.images)
                    return@map uri
                }
            }
        }

        return mConfigObservable.flatMap {
            cacheObservable.onExceptionResumeNext(networkObservable)
        //}.doOnNext { uri ->
        //    mClient.moviedb.setMovieAssociation(name, year, uri.lastPathSegment.toLong())
        }.flatMap { uri ->
            mClient.getMovie(uri.lastPathSegment.toLong()).toObservable()
        }
    }

}
