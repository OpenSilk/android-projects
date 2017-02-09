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

import org.opensilk.common.dagger.ActivityScope;
import org.opensilk.common.dagger.ForApplication;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;

/**
 * Created by drew on 4/14/16.
 */
@ActivityScope
public class MovieSearchLoader extends SearchLoader {

    @Inject
    public MovieSearchLoader(@ForApplication Context context, VideosProviderClient mClient) {
        super(context, mClient);
    }

    protected Observable<MediaBrowser.MediaItem> makeObservable() {
        return Observable.<MediaBrowser.MediaItem, Cursor>using(() -> {
            return mContentResolver.query(mClient.uris().movieSearch(),
                    new String[]{"rowid"}, "title MATCH ?", new String[]{mQuery}, null);
        }, c -> {
            if (c == null || !c.moveToFirst()) {
                return Observable.empty();
            }
            final StringBuilder sb = new StringBuilder(c.getCount()*2);
            sb.append("movie_id IN (").append(c.getString(0));
            while (c.moveToNext()) {
                sb.append(",").append(c.getString(0));
            }
            sb.append(")");
            return makeMovieObservable(sb.toString());
        }, c -> {
            VideosProviderClient.closeCursor(c);
        });
    }

    Observable<MediaBrowser.MediaItem> makeMovieObservable(String selection) {
        return Observable.<MediaBrowser.MediaItem, Cursor>using(() -> {
            return mContentResolver.query(mClient.uris().media(),
                    VideosProviderClient.MEDIA_PROJ, "media_type=? AND " + selection,
                    new String[]{String.valueOf(MediaMetaExtras.MEDIA_TYPE.MOVIE)}, "_title");
        }, c2 -> {
            if (c2 == null || !c2.moveToFirst()) {
                return Observable.empty();
            }
            List<MediaBrowser.MediaItem> movies = new ArrayList<>(c2.getCount());
            do {
                movies.add(mClient.buildMedia(c2));
            } while (c2.moveToNext());
            return Observable.from(movies);
        }, c2 -> {
            VideosProviderClient.closeCursor(c2);
        });
    }
}
