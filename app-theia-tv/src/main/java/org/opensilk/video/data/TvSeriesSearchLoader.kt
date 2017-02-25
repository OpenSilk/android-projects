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

package org.opensilk.video.data

import android.content.Context
import android.media.browse.MediaBrowser

import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.dagger.ForApplication

import javax.inject.Inject

import rx.Observable

/**
 * Created by drew on 4/14/16.
 */
@ActivityScope
class TvSeriesSearchLoader
@Inject
constructor(
        @ForApplication context: Context,
        mClient: VideosProviderClient
) : SearchLoader(context, mClient) {

    override fun makeObservable(): Observable<MediaBrowser.MediaItem> {
        return Observable.create<String> { s ->
             mContentResolver.query(mClient.uris().tvSeriesSearch(),
                     arrayOf("rowid"), "title MATCH ?", arrayOf(mQuery), null)?.use { c ->
                 if (c.moveToFirst()) {
                     val sb = StringBuilder((c.count * 2) + 10)
                     sb.append("_id IN (").append(c.getString(0))
                     while (c.moveToNext()) {
                         sb.append(",").append(c.getString(0))
                     }
                     sb.append(")")
                     s.onNext(sb.toString())
                 }
                 s.onCompleted()
             } ?: s.onError(NullPointerException("Cursor was null"))
        }.flatMap { q -> makeTvSeriesObservable(q) }
    }

    internal fun makeTvSeriesObservable(selection: String): Observable<MediaBrowser.MediaItem> {
        return Observable.create { s ->
            mContentResolver.query(mClient.uris().tvSeries(),
                    mClient.tvdb().TV_SERIES_PROJ, selection, null, "_display_name")?.use { c2 ->
                if (c2.moveToFirst()) {
                    do {
                        s.onNext(mClient.tvdb().buildTvSeries(c2))
                    } while (c2.moveToNext())
                }
                s.onCompleted()
            } ?: s.onError(NullPointerException("Cursor was null"))
        }
    }

}