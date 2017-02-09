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

package org.opensilk.video;

import android.content.Context;
import android.util.Log;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.module.GlideModule;
import com.bumptech.glide.util.ContentLengthInputStream;

import org.opensilk.common.core.dagger2.DaggerService;
import org.opensilk.video.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;

/**
 * Created by drew on 4/10/16.
 */
public class GlideConfig implements GlideModule {

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        final File cacheDir = Utils.getCacheDir(context, "glide4");
        builder.setDiskCache(new DiskLruCacheFactory(() -> cacheDir, (512 * 1024 * 1024)));
    }

    @Override
    public void registerComponents(Context context, Registry registry) {
        Timber.d("registerComponents()");
        VideoAppComponent appComponent = DaggerService.getDaggerComponent(context);
        OkHttpClient okClient = appComponent.okClient().newBuilder()
                .addNetworkInterceptor(chain -> {
                    //let glide store responses
                    return chain.proceed(chain.request().newBuilder()
                        .cacheControl(new CacheControl.Builder().noStore().noCache().build())
                        .build());
                }).build();
        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(okClient));
    }

    /*
        Until updated for glide 4
     */

    public static class OkHttpUrlLoader implements ModelLoader<GlideUrl, InputStream> {

        private final Call.Factory client;

        public OkHttpUrlLoader(Call.Factory client) {
            this.client = client;
        }

        @Override
        public boolean handles(GlideUrl url) {
            return true;
        }

        @Override
        public LoadData<InputStream> buildLoadData(GlideUrl model, int width, int height, Options options) {
            return new LoadData<>(model, new OkHttpStreamFetcher(client, model));
        }

        /**
         * The default factory for {@link OkHttpUrlLoader}s.
         */
        public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {
            private static volatile Call.Factory internalClient;
            private Call.Factory client;

            private static Call.Factory getInternalClient() {
                if (internalClient == null) {
                    synchronized (Factory.class) {
                        if (internalClient == null) {
                            internalClient = new OkHttpClient();
                        }
                    }
                }
                return internalClient;
            }

            /**
             * Constructor for a new Factory that runs requests using a static singleton client.
             */
            public Factory() {
                this(getInternalClient());
            }

            /**
             * Constructor for a new Factory that runs requests using given client.
             *
             * @param client this is typically an instance of {@code OkHttpClient}.
             */
            public Factory(Call.Factory client) {
                this.client = client;
            }

            @Override
            public ModelLoader<GlideUrl, InputStream> build(MultiModelLoaderFactory multiFactory) {
                return new OkHttpUrlLoader(client);
            }

            @Override
            public void teardown() {
                // Do nothing, this instance doesn't own the client.
            }
        }
    }

    public static class OkHttpStreamFetcher implements DataFetcher<InputStream> {
        private static final String TAG = "OkHttpFetcher";
        private final Call.Factory client;
        private final GlideUrl url;
        private InputStream stream;
        private ResponseBody responseBody;
        private volatile Call call;

        public OkHttpStreamFetcher(Call.Factory client, GlideUrl url) {
            this.client = client;
            this.url = url;
        }

        @Override
        public void loadData(Priority priority, final DataCallback<? super InputStream> callback) {
            Request.Builder requestBuilder = new Request.Builder().url(url.toStringUrl());
            for (Map.Entry<String, String> headerEntry : url.getHeaders().entrySet()) {
                String key = headerEntry.getKey();
                requestBuilder.addHeader(key, headerEntry.getValue());
            }
            Request request = requestBuilder.build();

            call = client.newCall(request);
            call.enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "OkHttp failed to obtain result", e);
                    }
                    callback.onLoadFailed(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        long contentLength = response.body().contentLength();
                        responseBody = response.body();
                        stream = ContentLengthInputStream.obtain(responseBody.byteStream(), contentLength);
                    } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "OkHttp got error response: " + response.code() + ", " + response.message());
                    }
                    callback.onDataReady(stream);
                }
            });
        }

        @Override
        public void cleanup() {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                // Ignored
            }
            if (responseBody != null) {
                responseBody.close();
            }
        }

        @Override
        public void cancel() {
            Call local = call;
            if (local != null) {
                local.cancel();
            }
        }

        @Override
        public Class<InputStream> getDataClass() {
            return InputStream.class;
        }

        @Override
        public DataSource getDataSource() {
            return DataSource.REMOTE;
        }
    }

}
