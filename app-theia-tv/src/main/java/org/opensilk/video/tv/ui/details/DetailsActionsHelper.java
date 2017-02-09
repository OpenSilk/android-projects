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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.support.v17.leanback.widget.Action;

import org.opensilk.common.dagger.ForActivity;
import org.opensilk.common.dagger.ScreenScope;
import org.opensilk.video.tv.ui.playback.PlaybackActivity;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * Created by drew on 4/1/16.
 */
@ScreenScope
public class DetailsActionsHelper {

    private final MediaBrowser.MediaItem mediaItem;
    private final Context activityContext;

    @Inject
    public DetailsActionsHelper(
            MediaBrowser.MediaItem mediaItem,
            @ForActivity Context activityContext
    ) {
        this.mediaItem = mediaItem;
        this.activityContext = activityContext;
    }

    public boolean handle(Action action) {
        Timber.d("Handle(action=%s) id=%d", action, action.getId());
        switch ((int) action.getId()) {
            case DetailsActions.PLAY:
            case DetailsActions.RESUME:
            case DetailsActions.START_OVER: {
                Intent intent = new Intent(activityContext, PlaybackActivity.class);
                intent.setAction(getAction(action.getId()));
                intent.putExtra(DetailsActivity.MEDIA_ITEM, mediaItem);
                ((Activity) activityContext).startActivityForResult(intent, 203);
                return true;
            }
            default:
                return false;
        }
    }

    private static String getAction(long id) {
        if (id == DetailsActions.RESUME) {
            return PlaybackActivity.ACTION_RESUME;
        } else {
            return PlaybackActivity.ACTION_PLAY;
        }
    }

}
