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

import android.media.browse.MediaBrowser;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.video.data.DataService;
import org.opensilk.video.data.MediaMetaExtras;
import org.opensilk.video.util.Utils;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by drew on 4/14/16.
 */
@ScreenScope
class MovieExtender extends VideoExtender {

    @Inject
    public MovieExtender(
            DataService dataService,
            DetailsFileInfoRow fileInfoRow,
            DetailsActionsAdapter movieActionsAdapter,
            @Named("mediaItem") Observable<MediaBrowser.MediaItem> mediaItemObservable
    ) {
        super(dataService, fileInfoRow, movieActionsAdapter, mediaItemObservable);
    }

}
