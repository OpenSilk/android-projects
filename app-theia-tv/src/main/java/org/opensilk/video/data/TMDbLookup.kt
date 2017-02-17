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

package org.opensilk.video.data

import android.media.MediaDescription
import android.media.browse.MediaBrowser

import org.opensilk.common.dagger.ServiceScope
import org.opensilk.tmdb.api.TMDb
import org.opensilk.tmdb.api.model.ImageList
import org.opensilk.tmdb.api.model.Movie
import org.opensilk.tmdb.api.model.MovieList
import org.opensilk.tmdb.api.model.TMDbConfig
import org.opensilk.video.util.Utils

import javax.inject.Inject

import rx.Observable
import rx.functions.Func1
import rx.functions.Func2
import timber.log.Timber
import java.util.*

/**
 * Created by drew on 4/11/16.
 */
@ServiceScope
class TMDbLookup
@Inject
constructor(
        internal val mClient: VideosProviderClient,
        internal val mApi: TMDb
) : LookupService() {

    internal val mSeenMovies: MutableMap<Long, Movie> = LinkedHashMap()
    internal val mSeenAssociations: MutableMap<String, Long> = LinkedHashMap()

    internal val mConfigObservable: Observable<TMDbConfig> by lazy {
        return@lazy mApi.configurationObservable()
                .doOnNext { config ->
                    Timber.d("Updating TMDB config")
                    mClient.moviedb().updateConfig(config)
                }.replay(1).autoConnect()
    }

    override fun lookup(mediaItem: MediaBrowser.MediaItem?): rx.Observable<MediaBrowser.MediaItem> {
        if (mediaItem == null) {
            return rx.Observable.error<MediaBrowser.MediaItem>(NullPointerException("Sent null mediaItem"))
        }
        val description = mediaItem.description
        val metaExtras = MediaMetaExtras.from(description)
        val title = MediaDescriptionUtil.getMediaTitle(description)
        val name = Utils.extractMovieName(title)
        if (name.isNullOrBlank()) {
            return rx.Observable.error<MediaBrowser.MediaItem>(Exception("Failed to extract movie name from " + title))
        }
        val year = Utils.extractMovieYear(title)

        val cacheObservable = mConfigObservable.flatMap<MediaBrowser.MediaItem>(Func1 { config ->
            rx.Observable.create<MediaBrowser.MediaItem> { subscriber ->
                //Timber.d("lookup(%s)", title);
                val movieId = mSeenAssociations[name] ?: mClient.moviedb().getMovieAssociation(name)
                if (movieId > 0) {
                    mSeenAssociations[name] = movieId
                    val movie: Movie? = mSeenMovies[movieId] ?: mClient.moviedb().getMovie(movieId)
                    if (movie != null) {
                        mSeenMovies.put(movieId, movie)
                        if (!subscriber.isUnsubscribed) {
                            subscriber.onNext(buildMediaItem(mediaItem, movie, config))
                            subscriber.onCompleted()
                        }
                        return@create
                    }
                }
                if (!subscriber.isUnsubscribed) {
                    subscriber.onError(Exception("Missed cache q=" + name))
                }
            }
        })

        val networkObservable = mConfigObservable.flatMap({ config ->
            val movieSingle: Observable<MovieList>
            if (year.isNullOrBlank()) {
                movieSingle = mApi.searchMovieObservable(name, "en")
            } else {
                movieSingle = mApi.searchMovieObservable(name, year, "en")
            }
            LookupService.waitTurn()
            Timber.i("lookup(%s)->[movie=%s, year=%s]", title, name, year)
            /*return*/ movieSingle.flatMap<Pair<Movie, ImageList>>(Func1 { movieList ->
                if (movieList.results != null && movieList.results.size > 0) {
                    val movie = movieList.results[0]
                    LookupService.waitTurn()
                    /*return*/ Observable.zip(
                            mApi.movieObservable(movie.id!!, "en"),
                            mApi.movieImagesObservable(movie.id!!, "en"),
                            { movie, movieImage -> Pair(movie, movieImage) })
                }
                /*return*/ Observable.error(Exception("No results"))
            }).doOnNext { pair ->
                val movie = pair.first
                val imageList = pair.second
                mClient.moviedb().insertMovie(movie, config)
                mClient.moviedb().insertImages(imageList, config)
                mClient.moviedb().setMovieAssociation(name, movie.id!!)
                //getContentResolver().notifyChange(mClient.uris().movies(), null);
                //getContentResolver().notifyChange(mClient.uris().mediaMovie(), null);
            }.map({ pair ->
                buildMediaItem(mediaItem, pair.first, config)
                //            }).doOnCompleted(() -> {
                //                Timber.d("lookup(%s)->[onCompleted]", title);
            })
        })

        if (!metaExtras.isIndexed) {
            return cacheObservable.onExceptionResumeNext(networkObservable)
        } else {
            return Observable.empty<MediaBrowser.MediaItem>()
        }
    }

    //TODO use images
    internal fun buildMediaItem(mediaItem: MediaBrowser.MediaItem, movie: Movie, config: TMDbConfig): MediaBrowser.MediaItem {
        val description = mediaItem.description
        val metaExtras = MediaMetaExtras.from(description)
        val builder = MediaDescriptionUtil.newBuilder(description)

        metaExtras.movieId = movie.id!!
        metaExtras.isIndexed = true
        builder.setTitle(movie.title)
        builder.setSubtitle(movie.releaseDate)
        if (movie.posterPath.isNullOrBlank()) {
            builder.setIconUri(mClient.moviedb().makePosterUri(
                    config.images.secureBaseUrl, movie.posterPath))
        }
        if (movie.backdropPath.isNullOrBlank()) {
            metaExtras.backdropUri = mClient.moviedb().makeBackdropUri(
                    config.images.secureBaseUrl, movie.backdropPath)
        }
        builder.setExtras(metaExtras.bundle)
        val newMediaItem = MediaBrowser.MediaItem(builder.build(), mediaItem.flags)
        //        mClient.insertMedia(newMediaItem);
        return newMediaItem
        //        mDataService.notifyChange(newMediaItem);
    }
}
