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

import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import org.opensilk.media.MediaRef
import org.opensilk.media.MovieId
import org.opensilk.media.MovieRef
import org.opensilk.tmdb.api.TMDb
import org.opensilk.tmdb.api.model.ImageList
import org.opensilk.tmdb.api.model.Movie
import org.opensilk.tmdb.api.model.TMDbConfig
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
        return@lazy mApi.configurationObservable().retry(2)
                .doOnNext { config ->
                    Timber.d("Updating TMDB config")
                    mClient.setMovieImageBaseUrl(config.images.baseUrl)
                }.replay(1).autoConnect()
    }

    override fun lookupObservable(lookup: LookupRequest): Observable<MovieRef> {
        val name = lookup.lookupName
        if (name.isBlank()) {
            return Observable.error(IllegalArgumentException())
        }
        val year = lookup.releaseYear

        val networkObservable = Observable.defer {
            //do search
            Timber.d("Searching name=$name name=$year")
            return@defer if (year.isBlank())
                mApi.searchMovieObservable(name, "en")
            else {
                mApi.searchMovieObservable(name, year, "en")
            }
        }.flatMapMaybe { list ->
            //stream movie list
            val result = list.results?.get(0)
            return@flatMapMaybe if (result == null) {
                Maybe.empty()
            } else {
                Timber.d("Found movie ${result.title}")
                mClient.getMovie(MovieId(result.id))
                        .switchIfEmpty(fetchCompleteMovieInfo(result))
            }
        }

        return mConfigObservable.flatMap({ networkObservable })
                .switchIfEmpty(Observable.error(LookupException("Empty movie data")))
    }

    private fun fetchCompleteMovieInfo(movie: Movie): Maybe<MovieRef> {
        //fetch movie and images
        return Maybe.defer {
            LookupService.waitTurn()
            return@defer Maybe.zip<Movie, ImageList, MovieWithImages>(
                    mApi.movieObservable(movie.id, "en").firstElement(),
                    mApi.movieImagesObservable(movie.id, "en").firstElement(),
                    BiFunction { m, i -> MovieWithImages(m, i) }
            ).map { mwi ->
                val ref = mwi.movie.toMovieRef()
                val posters = mwi.images.posters.map { it.toMovieImageRef(mwi.movie.id, "poster") }
                val backdrops = mwi.images.backdrops.map { it.toMovieImageRef(mwi.movie.id, "backdrop") }
                mClient.addMovie(ref)
                mClient.addMovieImages(posters)
                mClient.addMovieImages(backdrops)
                return@map ref.id
            }.flatMap { id ->
                mClient.getMovie(id)
            }
        }
    }

}
