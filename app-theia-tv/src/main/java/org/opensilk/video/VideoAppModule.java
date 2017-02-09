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

import org.opensilk.common.dagger.ForApplication;
import org.opensilk.video.util.Utils;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

/**
 * Created by drew on 3/16/16.
 */
@Module(
        includes = ConfigModule.class
)
public class VideoAppModule {

    final VideoApp app;

    public VideoAppModule(VideoApp app) {
        this.app = app;
    }

    @Provides @Singleton
    public OkHttpClient provideOkHttpClient(@ForApplication Context context) {
        return new OkHttpClient.Builder()
                .cache(new Cache(Utils.getCacheDir(context, "okhttp3"), (50 * 1024 * 1024)))
                .build();
    }

}
