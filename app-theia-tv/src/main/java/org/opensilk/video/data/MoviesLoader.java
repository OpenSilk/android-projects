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
import android.support.annotation.Nullable;

import org.opensilk.common.core.dagger2.ActivityScope;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.rx.RxCursorListLoader;

import javax.inject.Inject;

/**
 * Created by drew on 4/10/16.
 */
@ActivityScope
public class MoviesLoader extends RxCursorListLoader<MediaBrowser.MediaItem> {

    final VideosProviderClient mClient;

    @Inject
    public MoviesLoader(
            @ForApplication Context mContext,
            VideosProviderClient mClient
    ) {
        super(mContext);
        this.mClient = mClient;
        setUri(mClient.uris().media());
        setProjection(VideosProviderClient.MEDIA_PROJ);
        setSelection("media_category=? AND movie_id IS NOT NULL AND movie_id > 0");
        setSelectionArgs(new String[]{
                String.valueOf(MediaMetaExtras.MEDIA_TYPE.MOVIE),
        });
        setSortOrder("_display_name");
    }

    @Nullable
    @Override
    protected MediaBrowser.MediaItem makeFromCursor(Cursor c) throws Exception {
        return mClient.buildMedia(c);
    }
}
