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

package org.opensilk.video.tv.ui.playback;

import org.opensilk.common.core.dagger2.ActivityScope;
import org.opensilk.video.VideoAppComponent;

import dagger.Component;
import rx.functions.Func2;

/**
 * Created by drew on 3/22/16.
 */
@ActivityScope
@Component(
        dependencies = VideoAppComponent.class,
        modules = PlaybackActivityModule.class
)
public interface PlaybackActivityComponent {
    Func2<VideoAppComponent, PlaybackActivityModule, PlaybackActivityComponent> FACTORY = new Func2<VideoAppComponent, PlaybackActivityModule, PlaybackActivityComponent>() {
        @Override
        public PlaybackActivityComponent call(VideoAppComponent videoAppComponent, PlaybackActivityModule playbackActivityModule) {
            return DaggerPlaybackActivityComponent.builder()
                    .videoAppComponent(videoAppComponent)
                    .playbackActivityModule(playbackActivityModule)
                    .build();
        }
    };
    void inject(PlaybackActivity activity);
    PlaybackControlsComponent newPlaybackControlsComponent(PlaybackControlsModule module);
}
