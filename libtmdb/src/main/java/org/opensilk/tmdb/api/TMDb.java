/*
 * Copyright (c) 2016 OpenSilk Productions LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.tmdb.api;

import org.opensilk.tmdb.api.model.ImageList;
import org.opensilk.tmdb.api.model.Movie;
import org.opensilk.tmdb.api.model.MovieList;
import org.opensilk.tmdb.api.model.TMDbConfig;
import org.opensilk.tmdb.api.model.TvEpisode;
import org.opensilk.tmdb.api.model.TvSeason;
import org.opensilk.tmdb.api.model.TvSeries;
import org.opensilk.tmdb.api.model.TvSeriesList;

import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by drew on 3/20/16.
 */
public interface TMDb {
    @GET("configuration")
    rx.Single<TMDbConfig> configuration();

    @GET("search/movie")
    rx.Single<MovieList> searchMovie(@Query("query") String query, @Query("language") String language);

    @GET("search/movie")
    rx.Single<MovieList> searchMovie(@Query("query") String query, @Query("year") String year, @Query("language") String language);

    @GET("movie/{id}")
    rx.Single<Movie> movie(@Path("id") long id, @Query("language") String language);

    @GET("movie/{id}/images")
    rx.Single<ImageList> movieImages(@Path("id") long id, @Query("language") String language);

    @GET("search/tv")
    rx.Single<TvSeriesList> searchTv(@Query("query") String query, @Query("language") String language);

    @GET("search/tv")
    rx.Single<TvSeriesList> searchTv(@Query("query") String query, @Query("first_air_date_year") String year, @Query("language") String language);

    @GET("tv/{id}")
    rx.Single<TvSeries> tvSeries(@Path("id") long id, @Query("language") String language);

    @GET("tv/{id}/season/{season_number}")
    rx.Single<TvSeason> tvSeason(@Path("id") long id, @Path("season_number") int season_number, @Query("language") String language);

    @GET("tv/{id}/season/{season_number}/episode/{episode_number}")
    rx.Single<TvEpisode> tvEpisode(@Path("id") long id, @Path("season_number") int season_number, @Path("episode_number") int episode_number, @Query("language") String language);

    @GET("configuration")
    rx.Observable<TMDbConfig> configurationObservable();

    @GET("search/movie")
    rx.Observable<MovieList> searchMovieObservable(@Query("query") String query, @Query("language") String language);

    @GET("search/movie")
    rx.Observable<MovieList> searchMovieObservable(@Query("query") String query, @Query("year") String year, @Query("language") String language);

    @GET("movie/{id}")
    rx.Observable<Movie> movieObservable(@Path("id") long id, @Query("language") String language);

    @GET("movie/{id}/images")
    rx.Observable<ImageList> movieImagesObservable(@Path("id") long id, @Query("language") String language);

    @GET("search/tv")
    rx.Observable<TvSeriesList> searchTvObservable(@Query("query") String query, @Query("language") String language);

    @GET("search/tv")
    rx.Observable<TvSeriesList> searchTvObservable(@Query("query") String query, @Query("first_air_date_year") String year, @Query("language") String language);

    @GET("tv/{id}")
    rx.Observable<TvSeries> tvSeriesObservable(@Path("id") long id, @Query("language") String language);

    @GET("tv/{id}/season/{season_number}")
    rx.Observable<TvSeason> tvSeasonObservable(@Path("id") long id, @Path("season_number") int season_number, @Query("language") String language);

    @GET("tv/{id}/season/{season_number}/episode/{episode_number}")
    rx.Observable<TvEpisode> tvEpisodeObservable(@Path("id") long id, @Path("season_number") int season_number, @Path("episode_number") int episode_number, @Query("language") String language);

}
