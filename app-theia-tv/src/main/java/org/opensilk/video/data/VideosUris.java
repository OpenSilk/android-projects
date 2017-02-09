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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.UriMatcher;
import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by drew on 4/1/16.
 */
public class VideosUris {

    final String mAuthority;
    final UriMatcher mMatcher;

    @Inject
    public VideosUris(@Named("videosAuthority") String mAuthority) {
        this.mAuthority = mAuthority;
        this.mMatcher = makeMatcher(mAuthority);
    }

    Uri.Builder base() {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(mAuthority).appendPath("videos");
    }

    public Uri tvSeries() {
        return base().appendPath("tv").appendPath("series").build();
    }

    public Uri tvSeries(long id) {
        return ContentUris.withAppendedId(tvSeries(), id);
    }

    public Uri tvSeriesSearch() {
        return base().appendPath("tv").appendPath("series").appendPath("search").build();
    }

    public Uri tvEpisodes() {
        return base().appendPath("tv").appendPath("episodes").build();
    }

    public Uri tvEpisode(long id) {
        return ContentUris.withAppendedId(tvEpisodes(), id);
    }

    public Uri tvBanners() {
        return base().appendPath("tv").appendPath("banners").build();
    }

    public Uri tvBanner(long id) {
        return ContentUris.withAppendedId(tvBanners(), id);
    }

    public Uri tvActors() {
        return base().appendPath("tv").appendPath("actors").build();
    }

    public Uri tvActor(long id) {
        return ContentUris.withAppendedId(tvActors(), id);
    }

    public Uri tvLookups() {
        return base().appendPath("tv").appendPath("lookups").build();
    }

    public Uri tvEpisodeDescriptions() {
        return base().appendPath("tv").appendPath("episodes").appendPath("descriptions").build();
    }

    public Uri tvEpisodeDescription(long id) {
        return ContentUris.withAppendedId(tvEpisodeDescriptions(), id);
    }

    public Uri media() {
        return base().appendPath("media").build();
    }

    public Uri media(long id) {
        return ContentUris.withAppendedId(media(), id);
    }

    public Uri movies() {
        return base().appendPath("movies").build();
    }

    public Uri movie(long id) {
        return ContentUris.withAppendedId(movies(), id);
    }

    public Uri movieImages() {
        return base().appendPath("movies").appendPath("images").build();
    }

    public Uri movieImage(long id) {
        return ContentUris.withAppendedId(movieImages(), id);
    }

    public Uri movieLookups() {
        return base().appendPath("movies").appendPath("lookups").build();
    }

    public Uri movieSearch() {
        return base().appendPath("movies").appendPath("search").build();
    }

    public Uri scanStart() {
        return base().appendPath("scan").appendPath("start").build();
    }

    public Uri scanComplete(){
        return base().appendPath("scan").appendPath("complete").build();
    }

    public Uri tvLookupStart() {
        return base().appendPath("lookup").appendPath("tv").appendPath("start").build();
    }

    public Uri tvLookupComplete() {
        return base().appendPath("lookup").appendPath("tv").appendPath("complete").build();
    }

    public Uri movieLookupStart() {
        return base().appendPath("lookup").appendPath("movie").appendPath("start").build();
    }

    public Uri movieLookupComplete() {
        return base().appendPath("lookup").appendPath("movie").appendPath("complete").build();
    }

    public interface M {
        int TV_SERIES = 1;
        int TV_SERIES_ONE = 2;
        int TV_EPISODES = 3;
        int TV_EPISODES_ONE = 4;
        int TV_BANNERS = 5;
        int TV_BANNERS_ONE = 6;
        int TV_ACTORS = 7;
        int TV_ACTORS_ONE = 8;
        int TV_LOOKUPS = 9;
        //
        //
        int TV_EPISODE_DESC = 12;
        int TV_EPISODE_DESC_ONE = 13;
        int TV_SERIES_SEARCH = 14;

        int MEDIA = 20;
        int MEDIA_ONE = 21;

        int MOVIES = 30;
        int MOVIES_ONE = 31;
        int MOVIE_IMAGES = 32;
        int MOVIE_IMAGES_ONE = 33;
        int MOVIE_LOOKUPS = 34;
        int MOVIE_SEARCH = 35;

    }

    public UriMatcher getMatcher() {
        return mMatcher;
    }

    static UriMatcher makeMatcher(String authority) {
        UriMatcher m = new UriMatcher(UriMatcher.NO_MATCH);
        m.addURI(authority, "videos/tv/series", M.TV_SERIES);
        m.addURI(authority, "videos/tv/series/#", M.TV_SERIES_ONE);
        m.addURI(authority, "videos/tv/series/search", M.TV_SERIES_SEARCH);
        m.addURI(authority, "videos/tv/episodes", M.TV_EPISODES);
        m.addURI(authority, "videos/tv/episodes/#", M.TV_EPISODES_ONE);
        m.addURI(authority, "videos/tv/banners", M.TV_BANNERS);
        m.addURI(authority, "videos/tv/banners/#", M.TV_BANNERS_ONE);
        m.addURI(authority, "videos/tv/actors", M.TV_ACTORS);
        m.addURI(authority, "videos/tv/actors/#", M.TV_ACTORS_ONE);
        m.addURI(authority, "videos/tv/lookups", M.TV_LOOKUPS);
        m.addURI(authority, "videos/tv/episodes/descriptions", M.TV_EPISODE_DESC);
        m.addURI(authority, "videos/tv/episodes/descriptions/#", M.TV_EPISODE_DESC_ONE);

        m.addURI(authority, "videos/media", M.MEDIA);
        m.addURI(authority, "videos/media/#", M.MEDIA_ONE);

        m.addURI(authority, "videos/movies", M.MOVIES);
        m.addURI(authority, "videos/movies/#", M.MOVIES_ONE);
        m.addURI(authority, "videos/movies/images", M.MOVIE_IMAGES);
        m.addURI(authority, "videos/movies/images/#", M.MOVIE_IMAGES_ONE);
        m.addURI(authority, "videos/movies/lookups", M.MOVIE_LOOKUPS);
        m.addURI(authority, "videos/movies/search", M.MOVIE_SEARCH);
        return m;
    }
}
