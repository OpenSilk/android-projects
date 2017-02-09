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

import android.content.Context;
import android.database.Cursor;
import android.media.browse.MediaBrowser;

import org.opensilk.common.core.dagger2.ActivityScope;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.rx.RxCursorListLoader;

import javax.inject.Inject;

/**
 * Created by drew on 4/8/16.
 */
@ActivityScope
public class TvSeriesLoader extends RxCursorListLoader<MediaBrowser.MediaItem> {

    final VideosProviderClient mClient;

    @Inject
    public TvSeriesLoader(
            @ForApplication Context context,
            VideosProviderClient mClient) {
        super(context);
        this.mClient = mClient;
        setUri(mClient.uris().tvSeries());
        setProjection(mClient.tvdb().TV_SERIES_PROJ);
        setSortOrder("_display_name");
    }

    @Override
    protected MediaBrowser.MediaItem makeFromCursor(Cursor c) throws Exception {
        return mClient.tvdb().buildTvSeries(c);
    }
}
