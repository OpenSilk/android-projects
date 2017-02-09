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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensilk.tmdb.BuildConfig;
import org.opensilk.tmdb.api.model.ImageList;
import org.opensilk.tmdb.api.model.Movie;
import org.opensilk.tmdb.api.model.MovieList;
import org.opensilk.tmdb.api.model.TMDbConfig;
import org.opensilk.tmdb.api.model.TvEpisode;
import org.opensilk.tmdb.api.model.TvSeason;
import org.opensilk.tmdb.api.model.TvSeries;
import org.opensilk.tmdb.api.model.TvSeriesList;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.util.Arrays;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import rx.Single;

/**
 * Created by drew on 3/20/16.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class TMDbParsingTest {

    TMDb mApi;
    MockWebServer mServer;

    @Before
    public void setup() throws Exception {
        mServer = new MockWebServer();
        mServer.start();

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(ApiKeyInterceptor.create("fooooooo"))
                .build();

        mApi = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .client(client)
                .build()
                .create(TMDb.class);
    }

    @After
    public void teardown() throws Exception {
        mServer.shutdown();
    }

    @Test
    public void testConfigParses() throws Exception {
        enqueueResponse("tmdb-config.json");

        Single<TMDbConfig> s = mApi.configuration();
        s.subscribe(c -> {
            System.out.println(c.getImages().toString());
        });
    }

    @Test
    public void testMovieSearchParses() throws Exception {
        enqueueResponse("tmdb-moviesearch-hungergames.json");

        Single<MovieList> s = mApi.searchMovie("hunger games", "en");
        s.subscribe(c -> {
            System.out.println(Arrays.toString(c.getResults().toArray()));
        });
    }

    @Test
    public void testMovieSearchWithYearParses() throws Exception {
        enqueueResponse("tmdb-moviesearch-hungergames-2012.json");

        Single<MovieList> s = mApi.searchMovie("hunger games", "2012", "en");
        s.subscribe(c -> {
            System.out.println(Arrays.toString(c.getResults().toArray()));
        });
    }

    @Test
    public void testMovieParses() throws Exception {
        enqueueResponse("tmdb-movie-hungergames.json");

        Single<Movie> s = mApi.movie(1111, "en");
        s.subscribe(m -> {
            System.out.println(m.toString());
        });
    }

    @Test
    public void testMovieImagesParses() throws Exception {
        enqueueResponse("tmdb-movieimages-hungergames.json");

        Single<ImageList> s = mApi.movieImages(1111, "en");
        s.subscribe(m -> {
            //
        });
    }

    @Test
    public void testTvSearchParses() throws Exception {
        enqueueResponse("tmdb-search-archer.json");

        Single<TvSeriesList> s = mApi.searchTv("archer", "en");
        s.subscribe(m -> {

        });
    }

    @Test
    public void testTvSeriesParses() throws Exception {
        enqueueResponse("tmdb-tv-archer.json");

        Single<TvSeries> s = mApi.tvSeries(1111, "en");
        s.subscribe(m -> {
        });
    }

    @Test
    public void testTvSeasonParses() throws Exception {
        enqueueResponse("tmdb-tv-archer-s1.json");

        Single<TvSeason> s = mApi.tvSeason(111, 1, "en");
        s.subscribe(m -> {

        });
    }

    @Test
    public void testTvEpisodeParses() throws  Exception {
        enqueueResponse("tmdb-tv-archer-s1e1.json");

        Single<TvEpisode> s = mApi.tvEpisode(111, 1, 1, "en");
        s.subscribe(m -> {

        });
    }

    private void enqueueResponse(String filename) throws Exception {
        MockResponse mr = new MockResponse();
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(filename);
            Buffer b = new Buffer();
            b.readFrom(is);
            mr.setBody(b);
            mServer.enqueue(mr);
        } finally {
            is.close();
        }
    }

}
