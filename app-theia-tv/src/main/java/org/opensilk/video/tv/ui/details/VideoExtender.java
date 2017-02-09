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
 * Created by drew on 4/10/16.
 */
@ScreenScope
class VideoExtender implements DetailsScreenExtender {

    final CompositeSubscription subscriptions = new CompositeSubscription();
    final DataService dataService;
    final DetailsFileInfoRow fileInfoRow;
    final DetailsActionsAdapter actionsAdapter;
    final Observable<MediaBrowser.MediaItem> mediaItemObservable;

    @Inject
    public VideoExtender(
            DataService dataService,
            DetailsFileInfoRow fileInfoRow,
            DetailsActionsAdapter actionsAdapter,
            @Named("mediaItem") Observable<MediaBrowser.MediaItem> mediaItemObservable
    ) {
        this.dataService = dataService;
        this.fileInfoRow = fileInfoRow;
        this.actionsAdapter = actionsAdapter;
        this.mediaItemObservable = mediaItemObservable;
    }

    @Override
    public void onCreate(DetailsScreenFragment fragment) {
        Subscription fileSub = mediaItemObservable.flatMap(dataService::getVideoFileInfo)
                .subscribe(fileInfoRow::setItem, e -> {
                    Timber.w(e, "videoFileInfoSubscription");
                });
        subscriptions.add(fileSub);
        Subscription actionSub = mediaItemObservable
                .subscribe(this::updateVideoActions, e -> {
                    Timber.w(e, "movieActionsSubscription");
                });
        subscriptions.add(actionSub);
    }

    @Override
    public void onDestroy(DetailsScreenFragment fragment) {
        subscriptions.clear();
    }

    void updateVideoActions(MediaBrowser.MediaItem mediaItem) {
        MediaMetaExtras metaExtras = MediaMetaExtras.from(mediaItem.getDescription());
        if (metaExtras.getLastPosition() > 0 && metaExtras.getLastPosition() < (metaExtras.getDuration() - 20000)) {
            actionsAdapter.set(DetailsActions.RESUME, new Action(DetailsActions.RESUME,
                    String.format(Locale.US, "Resume (%s)",
                            Utils.humanReadableDuration(metaExtras.getLastPosition()))));
            actionsAdapter.set(DetailsActions.START_OVER, new Action(DetailsActions.START_OVER,
                    "Start Over"));
            actionsAdapter.clear(DetailsActions.PLAY);
        } else {
            actionsAdapter.set(DetailsActions.PLAY, new Action(DetailsActions.PLAY, "Play"));
            actionsAdapter.clear(DetailsActions.RESUME);
            actionsAdapter.clear(DetailsActions.START_OVER);
        }
    }
}
