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

import android.content.ContentProvider;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.net.Uri;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensilk.tmdb.api.model.ImageList;
import org.opensilk.tmdb.api.model.Movie;
import org.opensilk.tmdb.api.model.MovieList;
import org.opensilk.tmdb.api.model.TMDbConfig;
import org.opensilk.tvdb.api.AllZippedConverter;
import org.opensilk.tvdb.api.model.AllZipped;
import org.opensilk.tvdb.api.model.Banner;
import org.opensilk.tvdb.api.model.Episode;
import org.opensilk.video.BuildConfig;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowLog;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.converter.moshi.MoshiConverterFactory;
import timber.log.Timber;

/**
 * Created by drew on 4/3/16.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class VideosProviderClientTest {

    VideosProvider videosProvider;
    VideosProviderClient client;

    @Before
    public void setup() throws Exception {
        Timber.uprootAll();
        Timber.plant(new Timber.DebugTree());
        ShadowLog.stream = System.err;
        videosProvider = new VideosProvider();
//        videosProvider.attachInfo(RuntimeEnvironment.application, null);
        Method attachInfo = ContentProvider.class.getDeclaredMethod("attachInfo",
                Context.class, ProviderInfo.class, boolean.class);
        attachInfo.setAccessible(true);
        attachInfo.invoke(videosProvider, RuntimeEnvironment.application, null, true);

        String authority = RuntimeEnvironment.application.getPackageName()+".provider.videos";
        client = new VideosProviderClient(RuntimeEnvironment.application,
                new VideosUris(authority), "http://test.com/");
        ShadowContentResolver.registerProvider(authority, videosProvider);

        InputStream is = getClass().getClassLoader().getResourceAsStream("series-archer.zip");
        try {
            ResponseBody body = ResponseBody.create(MediaType.parse("application/octetstream"),
                    new Buffer().readFrom(is).readByteArray());
            AllZipped al = AllZippedConverter.instance().convert(body);
            client.tvdb().insertAllZipped(al);
        } finally {
            IOUtils.closeQuietly(is);
        }
        InputStream is2 = getClass().getClassLoader().getResourceAsStream("tmdb-movie-hungergames.json");
        try {
            ResponseBody body = ResponseBody.create(MediaType.parse("application/json"),
                    new Buffer().readFrom(is2).readByteArray());
            Movie movie = (Movie) MoshiConverterFactory.create().responseBodyConverter(Movie.class, null, null)
                    .convert(body);
            Assertions.assertThat(movie).isNotNull();
            TMDbConfig config = new TMDbConfig(new TMDbConfig.Images(null, "http://ex.com/",
                    null, null, null, null, null));
            client.moviedb().insertMovie(movie, config);
        } finally {
            IOUtils.closeQuietly(is2);
        }
        InputStream is3 = getClass().getClassLoader().getResourceAsStream("tmdb-movieimages-hungergames.json");
        try {
            ResponseBody body = ResponseBody.create(MediaType.parse("application/json"),
                    new Buffer().readFrom(is3).readByteArray());
            ImageList list = (ImageList) MoshiConverterFactory.create().responseBodyConverter(
                    ImageList.class, null, null).convert(body);
            Assertions.assertThat(list).isNotNull();
            TMDbConfig config = new TMDbConfig(new TMDbConfig.Images(null, "http://ex.com/",
                    null, null, null, null, null));
            client.moviedb().insertImages(list, config);
        } finally {
            IOUtils.closeQuietly(is3);
        }

    }

    @After
    public void teardown() {
        ShadowContentResolver.reset();
    }


    @Test
    public void testGetMedia() {
        insertTestMedia();
        insertTestMedia2();

        MediaBrowser.MediaItem item = client.getMedia(TEST_MEDIA_URI_1);
        Assertions.assertThat(item).isNotNull();
        MediaMetaExtras metaExtras1 = MediaMetaExtras.from(item.getDescription());
//        Assertions.assertThat(metaExtras1.isIndexed()).isTrue();
        Assertions.assertThat(item.getDescription().getTitle()).isEqualTo("Mole Hunt");
//        Assertions.assertThat(item.getDescription().getSubtitle()).isEqualTo("Archer (2009) - S01E01");
//        Assertions.assertThat(metaExtras1.getBackdropUri()).isEqualTo(Uri.parse("http://test.com/banners/fanart/original/110381-23.jpg"));
//        Assertions.assertThat(item.getDescription().getIconUri()).isEqualTo(Uri.parse("http://test.com/banners/posters/110381-9.jpg"));
//        Assertions.assertThat(metaExtras1.getPosterUri()).isEqualTo(Uri.parse("http://test.com/banners/posters/110381-9.jpg"));


        item = client.getMedia(TEST_MEDIA_URI_2);
        Assertions.assertThat(item).isNotNull();
        metaExtras1 = MediaMetaExtras.from(item.getDescription());
//        Assertions.assertThat(metaExtras1.isIndexed()).isTrue();
        Assertions.assertThat(item.getDescription().getTitle()).isEqualTo("The Hunger Games");
//        Assertions.assertThat(item.getDescription().getSubtitle()).isEqualTo("2012-03-12");
//        Assertions.assertThat(item.getDescription().getIconUri()).isEqualTo(
//                client.moviedb().makePosterUri("http://ex.com/", "/iLJdwmzrHFjFwI5lvYAT1gcpRuA.jpg"));
//        Assertions.assertThat(item2.getDescription().getSubtitle()).isEqualTo("Archer (2009)");
    }

    @Test
    public void testGetBanners() {
        List<Banner> banners = client.tvdb().getBanners(110381);
        Assertions.assertThat(banners.size()).isEqualTo(80);
    }

    @Test
    public void testGetBanners2() {
        List<Banner> banners = client.tvdb().getBanners(110381, 2);
        Assertions.assertThat(banners.size()).isEqualTo(2);
    }

    @Test
    public void testgetMovie() {
        insertTestMedia2();
        Movie m = client.moviedb().getMovie(70160);
        Assertions.assertThat(m).isNotNull();
    }

    @Test
    public void testGetChildren() {
        insertTestMedia();
        insertTestMedia2();

        MediaDescription.Builder bob = new MediaDescription.Builder()
                .setMediaId("directory:"+TEST_MEDIA_URI_PARENT_1);
        MediaMetaExtras metaExtras = MediaMetaExtras.directory();
        MediaDescriptionUtil.setMediaUri(bob, metaExtras, TEST_MEDIA_URI_PARENT_1);
        bob.setExtras(metaExtras.getBundle());
        List<MediaBrowser.MediaItem> lst = client.getChildren(new MediaBrowser.MediaItem(bob.build(), MediaBrowser.MediaItem.FLAG_BROWSABLE));
        Assertions.assertThat(lst.size()).isEqualTo(2);
    }

    @Test
    public void testGetEpisode() {
        insertTestMedia();

        Episode e = client.tvdb().getEpisode(1555111);
        Assertions.assertThat(e).isNotNull();
        Assertions.assertThat(e.getId()).isEqualTo(1555111);
    }

    static final Uri TEST_MEDIA_URI_PARENT_1 = Uri.parse("upnp://foo_server/dir");
    static final Uri TEST_MEDIA_URI_1 = Uri.parse("http://foo.com/111.mp4");
    void insertTestMedia() {
        MediaDescription.Builder bob = new MediaDescription.Builder()
                .setTitle("Mole Hunt")
                .setMediaId("media:"+TEST_MEDIA_URI_1);
        MediaMetaExtras metaExtras = MediaMetaExtras.tvEpisode();
        metaExtras.setParentUri(TEST_MEDIA_URI_PARENT_1);
        metaExtras.setServerId("foo_server");
        metaExtras.setEpisodeId(1555111);
        metaExtras.setSeriesId(110381);
        metaExtras.setMediaTitle("archer.2009.s01e01");
        MediaDescriptionUtil.setMediaUri(bob, metaExtras, TEST_MEDIA_URI_1);
        bob.setExtras(metaExtras.getBundle());
        MediaBrowser.MediaItem mediaItem = new MediaBrowser.MediaItem(bob.build(), MediaBrowser.MediaItem.FLAG_PLAYABLE);

        client.insertMedia(mediaItem);
    }
    static final Uri TEST_MEDIA_URI_2 = Uri.parse("http://foo.com/222.mp4");
    void insertTestMedia2() {
        MediaDescription.Builder bob = new MediaDescription.Builder()
                .setTitle("hunger.games.2012")
                .setMediaId("media:"+TEST_MEDIA_URI_2);
        MediaMetaExtras metaExtras = MediaMetaExtras.movie();
        metaExtras.setParentUri(TEST_MEDIA_URI_PARENT_1);
        metaExtras.setServerId("foo_server");
        metaExtras.setMovieId(70160);
        metaExtras.setMediaTitle("hunger.games.2012");
        MediaDescriptionUtil.setMediaUri(bob, metaExtras, TEST_MEDIA_URI_2);
        bob.setExtras(metaExtras.getBundle());
        MediaBrowser.MediaItem mediaItem = new MediaBrowser.MediaItem(bob.build(), MediaBrowser.MediaItem.FLAG_PLAYABLE);

        client.insertMedia(mediaItem);
    }

}
