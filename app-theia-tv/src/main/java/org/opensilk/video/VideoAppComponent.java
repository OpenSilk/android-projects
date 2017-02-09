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

import org.opensilk.common.dagger.AppContextComponent;
import org.opensilk.common.dagger.AppContextModule;
import org.opensilk.video.data.DataService;
import org.opensilk.video.data.ScannerServiceComponent;
import org.opensilk.video.data.VideosProviderComponent;
import org.opensilk.video.data.VideosProviderModule;
import org.opensilk.video.playback.VLCInstance;

import javax.inject.Singleton;

import dagger.Component;
import okhttp3.OkHttpClient;
import rx.functions.Func1;

/**
 * Created by drew on 3/16/16.
 */
@Singleton
@Component(
        modules = {
                VideoAppModule.class,
                AppContextModule.class
        }
)
public interface VideoAppComponent extends AppContextComponent {
    Func1<VideoApp, VideoAppComponent> FACTORY = new Func1<VideoApp, VideoAppComponent>() {
        @Override
        public VideoAppComponent call(VideoApp videoApp) {
            return DaggerVideoAppComponent.builder()
                    .videoAppModule(new VideoAppModule(videoApp))
                    .appContextModule(new AppContextModule(videoApp))
                    .build();
        }
    };
    VideoAppPreferences preferences();
    VLCInstance vlcInstance();
    DataService dataService();
    OkHttpClient okClient();
    VideosProviderComponent newVideosProviderComponent(VideosProviderModule module);
    ScannerServiceComponent newScannerServiceComponent();
}
