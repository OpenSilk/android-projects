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

import android.media.browse.MediaBrowser;

import org.apache.commons.lang3.concurrent.TimedSemaphore;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Created by drew on 4/11/16.
 */
public abstract class LookupService {

    private static final long WAIT_TIME = 1000;
    private static final int WAIT_USERS = 1;

    private static TimedSemaphore sSemaphore = new TimedSemaphore(WAIT_TIME, TimeUnit.MILLISECONDS, WAIT_USERS);

    //TODO actually handle 429 or whatever the try later code is
    //for now we just limit all our network calls to one per second
    protected static synchronized void waitTurn() {
        try {
            if (sSemaphore.isShutdown()) {
                sSemaphore = new TimedSemaphore(WAIT_TIME, TimeUnit.MILLISECONDS, WAIT_USERS);
            }
            sSemaphore.acquire();
        } catch (InterruptedException e) {
            Timber.w("Interrupted while waiting on semaphore");
        }
    }

    public abstract rx.Observable<MediaBrowser.MediaItem> lookup(MediaBrowser.MediaItem mediaItem);

}
