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

import org.opensilk.common.dagger.ServiceScope;
import org.opensilk.tmdb.api.ApiKeyInterceptor;
import org.opensilk.tmdb.api.TMDb;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * Created by drew on 4/11/16.
 */
@Module
public class TMDbModule {

    @Provides @ServiceScope
    public TMDb provideTMDB(OkHttpClient okHttpClient,
                            @Named("tmdb_root") String baseUrl,
                            @Named("tmdb_api_key") String apiKey) {

        OkHttpClient client = okHttpClient.newBuilder()
                .addInterceptor(ApiKeyInterceptor.create(apiKey))
                .build();

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .client(client)
                .build()
                .create(TMDb.class);
    }

}
