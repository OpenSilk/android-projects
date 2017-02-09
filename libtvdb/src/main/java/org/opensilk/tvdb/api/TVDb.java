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

import org.opensilk.tvdb.api.model.AllZipped;
import org.opensilk.tvdb.api.model.SeriesInfo;
import org.opensilk.tvdb.api.model.SeriesList;
import org.opensilk.tvdb.api.model.Updates;

import okhttp3.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by drew on 3/19/16.
 */
public interface TVDb {

    @GET("api/Updates.php")
    rx.Single<Updates> updatesSingle(@Query("type") String type);

    @GET("api/Updates.php")
    rx.Single<Updates> updatesSingle(@Query("type") String type,
                                     @Query("time") long time);

    @GET("api/GetSeries.php")
    rx.Single<SeriesList> getSeriesSingle(@Query("seriesname") String seriesName,
                                          @Query("language") String language);

    @GET("api/{apikey}/series/{seriesid}/{language}.xml")
    rx.Single<SeriesInfo> seriesInfoSingle(@Path("apikey") String apiKey,
                                           @Path("seriesid") long seriesId,
                                           @Path("language") String language);

    @GET("api/{apikey}/series/{seriesid}/all/{language}.zip")
    rx.Single<AllZipped> allZippedSingle(@Path("apikey") String apiKey,
                                         @Path("seriesid") long seriesId,
                                         @Path("language") String language);

    @GET("api/Updates.php")
    rx.Observable<Updates> updatesObservable(@Query("type") String type);

    @GET("api/Updates.php")
    rx.Observable<Updates> updatesObservable(@Query("type") String type,
                                             @Query("time") long time);

    @GET("api/GetSeries.php")
    rx.Observable<SeriesList> getSeriesObservable(@Query("seriesname") String seriesName,
                                                  @Query("language") String language);

    @GET("api/{apikey}/series/{seriesid}/{language}.xml")
    rx.Observable<SeriesInfo> seriesInfoObservable(@Path("apikey") String apiKey,
                                                   @Path("seriesid") long seriesId,
                                                   @Path("language") String language);

    @GET("api/{apikey}/series/{seriesid}/all/{language}.zip")
    rx.Observable<AllZipped> allZippedObservable(@Path("apikey") String apiKey,
                                                 @Path("seriesid") long seriesId,
                                                 @Path("language") String language);
}
