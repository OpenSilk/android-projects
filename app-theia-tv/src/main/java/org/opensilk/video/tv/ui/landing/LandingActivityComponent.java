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

package org.opensilk.video.tv.ui.landing;

import org.opensilk.common.dagger.ActivityScope;
import org.opensilk.video.VideoAppComponent;

import dagger.Component;
import rx.functions.Func2;

/**
 * Created by drew on 4/8/16.
 */
@ActivityScope
@Component(
        dependencies = VideoAppComponent.class,
        modules = LandingActivityModule.class
)
public interface LandingActivityComponent {
    Func2<VideoAppComponent, LandingActivityModule, LandingActivityComponent> FACTORY= new Func2<VideoAppComponent, LandingActivityModule, LandingActivityComponent>() {
        @Override
        public LandingActivityComponent call(VideoAppComponent videoAppComponent, LandingActivityModule landingActivityModule) {
            return DaggerLandingActivityComponent.builder()
                    .videoAppComponent(videoAppComponent)
                    .landingActivityModule(landingActivityModule)
                    .build();
        }
    };
    LandingScreenComponent newLandingScreenComponent(LandingScreenModule module);
}