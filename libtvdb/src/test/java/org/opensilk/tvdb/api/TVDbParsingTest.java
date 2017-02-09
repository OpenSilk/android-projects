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

package org.opensilk.tvdb.api;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensilk.tvdb.api.model.ActorList;
import org.opensilk.tvdb.api.model.AllZipped;
import org.opensilk.tvdb.api.model.SeriesInfo;
import org.opensilk.tvdb.api.model.SeriesList;
import org.opensilk.tvdb.BuildConfig;
import org.opensilk.tvdb.api.model.Updates;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.simpleframework.xml.core.Persister;

import java.io.InputStream;

import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import rx.Observable;

/**
 * Created by drew on 3/19/16.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class TVDbParsingTest {

    MockWebServer mServer;
    TVDb mApi;

    @Before
    public void setup() throws Exception {
        mServer = new MockWebServer();
        mServer.start();

        mApi = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(AllZippedConverter.Factory.instance())
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .validateEagerly(true)
                .build()
                .create(TVDb.class);
    }

    @After
    public void teardown() throws Exception {
        mServer.shutdown();
    }

    @Test
    public void testGetSeriesParses() throws Exception {
        enqueueResponse("getseries-archer.xml");

        Observable<SeriesList> lst = mApi.getSeriesObservable("archer", "en");
        lst.subscribe(l -> {
            Assertions.assertThat(l.getSeries().size()).isEqualTo(3);
        });
    }

    @Test
    public void testSeriesParses() throws Exception {
        enqueueResponse("series-archer.xml");

        Observable<SeriesInfo> s = mApi.seriesInfoObservable("foo", 111, "en");
        s.subscribe(si -> {
            Assertions.assertThat(si.getSeries().getId()).isEqualTo(110381);
            Assertions.assertThat(si.getEpisodes()).isNull();
        });
    }

    @Test
    public void testAllZippedParses() throws Exception {
        enqueueResponse("series-archer.zip");

        Observable<AllZipped> s = mApi.allZippedObservable("foo", 111, "en");
        s.subscribe(az -> {
            Assertions.assertThat(az.getSeries().getId()).isEqualTo(110381);
            Assertions.assertThat(az.getEpisodes().size()).isEqualTo(84);
            Assertions.assertThat(az.getBanners().size()).isEqualTo(80);
            Assertions.assertThat(az.getActors().size()).isEqualTo(9);
        });
    }

    @Test
    public void testUpdatesNoneParses() throws Exception {
        enqueueResponse("updates-none.xml");

        Observable<Updates> s = mApi.updatesObservable("none");
        s.subscribe(u -> {
            Assertions.assertThat(u.getTime()).isEqualTo(1461009686);
            Assertions.assertThat(u.getSeries()).isNull();
            Assertions.assertThat(u.getEpisodes()).isNull();
        });
    }

    @Test
    public void testUpdatesAllParses() throws Exception {
        enqueueResponse("updates-all.xml");

        Observable<Updates> s = mApi.updatesObservable("all", 11111);
        s.subscribe(u -> {
            Assertions.assertThat(u.getTime()).isEqualTo(1461008948);
            Assertions.assertThat(u.getSeries().size()).isEqualTo(10);
            Assertions.assertThat(u.getEpisodes().size()).isEqualTo(21);
        });
    }

    @Test
    public void test_parseBrokenActorsList() throws Exception {
        InputStream is = openResource("actors-broken.xml");
        try {
            Persister p = new Persister();
            ActorList al = p.read(ActorList.class, is);
            Assertions.assertThat(al.getActors().size()).isEqualTo(23);
        } finally {
            is.close();
        }
    }

    private void enqueueResponse(String filename) throws Exception {
        MockResponse mr = new MockResponse();
        InputStream is = null;
        try {
            is = openResource(filename);
            Buffer b = new Buffer();
            b.readFrom(is);
            mr.setBody(b);
            mServer.enqueue(mr);
        } finally {
            is.close();
        }
    }

    private InputStream openResource(String filename) throws Exception {
        return getClass().getClassLoader().getResourceAsStream(filename);
    }
}
