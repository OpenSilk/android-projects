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

import android.media.browse.MediaBrowser;
import android.support.v17.leanback.widget.VerticalGridPresenter;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.dagger.ScreenScope;
import org.opensilk.video.data.DataService;
import org.opensilk.video.data.MediaMetaExtras;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import rx.Observable;
import rx.functions.Func1;

/**
 * Created by drew on 3/22/16.
 */
@Module
public class FoldersScreenModule {

    @Provides @ScreenScope @Named("title")
    public String provideTitle(MediaBrowser.MediaItem mediaItem) {
        CharSequence title = mediaItem.getDescription().getTitle();
        if (StringUtils.isEmpty(title)) {
            if (mediaItem.isBrowsable()) {
                title = "Directory";
            } else {
                title = "Media";
            }
        }
        return title.toString();
    }

    @Provides @ScreenScope
    public VerticalGridPresenter provideGridPresenter() {
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(1);
        return gridPresenter;
    }

    @Provides @ScreenScope @Named("isIndexed")
    public Observable<Boolean> provideIsIndexedObservable(
            MediaBrowser.MediaItem mediaItem,
            DataService dataService
    ) {
        return dataService.getMediaItem(mediaItem)
                .map(new Func1<MediaBrowser.MediaItem, Boolean>() {
                    @Override
                    public Boolean call(MediaBrowser.MediaItem item) {
                        MediaMetaExtras metaExtras = MediaMetaExtras.from(item.getDescription());
                        return metaExtras.isIndexed();
                    }
                });
    }

}
