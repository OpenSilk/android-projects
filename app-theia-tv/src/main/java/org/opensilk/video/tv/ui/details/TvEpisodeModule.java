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

package org.opensilk.video.tv.ui.details;

import android.content.Context;
import android.media.browse.MediaBrowser;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.widget.Toast;

import org.opensilk.common.dagger.ForActivity;
import org.opensilk.common.dagger.ScreenScope;
import org.opensilk.video.data.VideoFileInfo;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 3/21/16.
 */
@Module
public class TvEpisodeModule {

    @Provides @ScreenScope
    public OnActionClickedListener getActionsClickListener(
            final DetailsActionsHelper helper,
            @ForActivity final Context activityContext
    ) {
        return new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                if (!helper.handle(action)) {
                    Toast.makeText(activityContext, "UNIMPLEMENTED", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    @Provides @ScreenScope @Named("additionalRowsAdapter")
    public ObjectAdapter getRowsAdapter(
            DetailsFileInfoRowPresenter detailsFileInfoRowPresenter,
            DetailsFileInfoRow detailsFileInfoRow) {
        ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
        ArrayObjectAdapter arrayObjectAdapter = new ArrayObjectAdapter(presenterSelector);

        presenterSelector.addClassPresenter(DetailsFileInfoRow.class, detailsFileInfoRowPresenter);
        arrayObjectAdapter.add(detailsFileInfoRow);

        return arrayObjectAdapter;
    }

    @Provides @ScreenScope
    public DetailsFileInfoRow provideFileInfoRow(MediaBrowser.MediaItem mediaItem) {
        //init with default info
        DetailsFileInfoRow row = new DetailsFileInfoRow(DetailsRowIds.FILE_INFO,
                new HeaderItem("File Info"), VideoFileInfo.from(mediaItem));
        return row;
    }

    @Provides @ScreenScope
    public DetailsScreenExtender provideExtender(TvEpisodeExtender extender){
        return extender;
    }

}
