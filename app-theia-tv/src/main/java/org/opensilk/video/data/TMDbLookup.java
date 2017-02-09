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

package org.opensilk.video.data;

import android.media.MediaDescription;
import android.media.browse.MediaBrowser;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.opensilk.common.dagger.ServiceScope;
import org.opensilk.tmdb.api.TMDb;
import org.opensilk.tmdb.api.model.ImageList;
import org.opensilk.tmdb.api.model.Movie;
import org.opensilk.tmdb.api.model.MovieList;
import org.opensilk.tmdb.api.model.TMDbConfig;
import org.opensilk.video.util.Utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import rx.Observable;
import timber.log.Timber;

/**
 * Created by drew on 4/11/16.
 */
@ServiceScope
public class TMDbLookup extends LookupService {

    final VideosProviderClient mClient;
    final TMDb mApi;

    final Map<Long, Movie> mSeenMovies = Collections.synchronizedMap(new HashMap<>());
    final Map<String, Long> mSeenAssociations = Collections.synchronizedMap(new HashMap<>());

    Observable<TMDbConfig> mConfigObservable;

    @Inject
    public TMDbLookup(VideosProviderClient mClient, TMDb mApi) {
        this.mClient = mClient;
        this.mApi = mApi;
    }

    @Override
    public rx.Observable<MediaBrowser.MediaItem> lookup(final MediaBrowser.MediaItem mediaItem) {
        if (mediaItem == null) {
            return rx.Observable.error(new NullPointerException("Sent null mediaItem"));
        }
        final MediaDescription description = mediaItem.getDescription();
        final MediaMetaExtras metaExtras = MediaMetaExtras.from(description);
        final String title = MediaDescriptionUtil.getMediaTitle(description);
        final String name = Utils.extractMovieName(title);
        if (StringUtils.isEmpty(name)) {
            return rx.Observable.error(new Exception("Failed to extract movie name from " + title));
        }
        final String year = Utils.extractMovieYear(title);

        Observable<MediaBrowser.MediaItem> cacheObservable = getConfig().flatMap(config -> {
            return rx.Observable.<MediaBrowser.MediaItem>create(subscriber -> {
//                Timber.d("lookup(%s)", title);
                Long movieId = mSeenAssociations.get(name);
                if (movieId == null) {
                    movieId = mClient.moviedb().getMovieAssociation(name);
                    if (movieId > 0) {
                        mSeenAssociations.put(name, movieId);
                    }
                }
                if (movieId > 0) {
                    Movie movie = mSeenMovies.get(movieId);
                    if (movie == null) {
                        movie = mClient.moviedb().getMovie(movieId);
                        mSeenMovies.put(movieId, movie);
                    }
                    if (movie != null) {
                        if (subscriber.isUnsubscribed()) {
                            return;
                        }
                        subscriber.onNext(buildMediaItem(mediaItem, movie, config));
                        subscriber.onCompleted();
                        return;
                    }
                }
                if (subscriber.isUnsubscribed()) {
                    return;
                }
                subscriber.onError(new Exception("Missed cache q=" + name));
            });
        });

        Observable<MediaBrowser.MediaItem> networkObservable = getConfig().flatMap(config -> {
            Observable<MovieList> movieSingle;
            if (StringUtils.isEmpty(year)) {
                movieSingle = mApi.searchMovieObservable(name, "en");
            } else {
                movieSingle = mApi.searchMovieObservable(name, year, "en");
            }
            waitTurn();
            Timber.i("lookup(%s)->[movie=%s, year=%s]", title, name, year);
            return movieSingle.flatMap(movieList -> {
                if (movieList.getResults() != null && movieList.getResults().size() > 0) {
                    Movie movie = movieList.getResults().get(0);
                    waitTurn();
                    return Observable.zip(
                            mApi.movieObservable(movie.getId(), "en"),
                            mApi.movieImagesObservable(movie.getId(), "en"),
                            Pair::of);
                }
                return Observable.error(new Exception("No results"));
            }).doOnNext(pair -> {
                Movie movie = pair.getLeft();
                ImageList imageList = pair.getRight();
                mClient.moviedb().insertMovie(movie, config);
                mClient.moviedb().insertImages(imageList, config);
                mClient.moviedb().setMovieAssociation(name, movie.getId());
//                        getContentResolver().notifyChange(mClient.uris().movies(), null);
//                        getContentResolver().notifyChange(mClient.uris().mediaMovie(), null);
            }).map(pair -> {
                return buildMediaItem(mediaItem, pair.getLeft(), config);
//            }).doOnCompleted(() -> {
//                Timber.d("lookup(%s)->[onCompleted]", title);
            });
        });

        if (!metaExtras.isIndexed()) {
            return cacheObservable.onExceptionResumeNext(networkObservable);
        } else {
            return Observable.empty();
        }
    }

    synchronized rx.Observable<TMDbConfig> getConfig() {
        if (mConfigObservable == null) {
            mConfigObservable = mApi.configurationObservable()
                    .doOnNext(config -> {
                        Timber.d("Updating TMDB config");
                        mClient.moviedb().updateConfig(config);
                    }).replay(1).autoConnect();
        }
        return mConfigObservable;
    }

    //TODO use images
    MediaBrowser.MediaItem buildMediaItem(MediaBrowser.MediaItem mediaItem, Movie movie, TMDbConfig config) {
        MediaDescription description = mediaItem.getDescription();
        MediaMetaExtras metaExtras = MediaMetaExtras.from(description);
        MediaDescription.Builder builder = MediaDescriptionUtil.newBuilder(description);

        metaExtras.setMovieId(movie.getId());
        metaExtras.setIndexed(true);
        builder.setTitle(movie.getTitle());
        builder.setSubtitle(movie.getReleaseDate());
        if (!StringUtils.isEmpty(movie.getPosterPath())) {
            builder.setIconUri(mClient.moviedb().makePosterUri(
                    config.getImages().getSecureBaseUrl(), movie.getPosterPath()));
        }
        if (!StringUtils.isEmpty(movie.getBackdropPath())) {
            metaExtras.setBackdropUri(mClient.moviedb().makeBackdropUri(
                    config.getImages().getSecureBaseUrl(), movie.getBackdropPath()));
        }
        builder.setExtras(metaExtras.getBundle());
        MediaBrowser.MediaItem newMediaItem = new MediaBrowser.MediaItem(builder.build(), mediaItem.getFlags());
//        mClient.insertMedia(newMediaItem);
        return newMediaItem;
//        mDataService.notifyChange(newMediaItem);
    }
}
