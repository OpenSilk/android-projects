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

package org.opensilk.video.tv.ui.folders;

import org.opensilk.common.dagger.ActivityScope;
import org.opensilk.video.VideoAppComponent;

import dagger.Component;
import rx.functions.Func2;

/**
 * Created by drew on 3/22/16.
 */
@ActivityScope
@Component(
        dependencies = VideoAppComponent.class,
        modules = FoldersActivityModule.class
)
public interface FoldersActivityComponent {
    Func2<VideoAppComponent, FoldersActivityModule, FoldersActivityComponent> FACTORY = new Func2<VideoAppComponent, FoldersActivityModule, FoldersActivityComponent>() {
        @Override
        public FoldersActivityComponent call(VideoAppComponent videoAppComponent, FoldersActivityModule foldersActivityModule) {
            return DaggerFoldersActivityComponent.builder()
                    .videoAppComponent(videoAppComponent)
                    .foldersActivityModule(foldersActivityModule)
                    .build();
        }
    };
    FoldersScreenComponent newScreenComponent();
}
