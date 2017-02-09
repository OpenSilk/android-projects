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

import android.net.Uri;
import android.support.annotation.NonNull;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.util.MediaBrowser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by drew on 4/16/16.
 */
public class VLCBrowserBrowseFuture implements Future<List<Media>>, MediaBrowser.EventListener {

    private boolean mDone = false;
    private final List<Media> mMedias = new ArrayList<>();

    private VLCBrowserBrowseFuture(){
    }

    public static VLCBrowserBrowseFuture from(MediaBrowser browser, Uri mediaUri) {
        VLCBrowserBrowseFuture f = new VLCBrowserBrowseFuture();
        browser.changeEventListener(f);
        browser.browse(mediaUri, false);
        return f;
    }

    @Override
    public synchronized void onMediaAdded(int i, Media media) {
        mMedias.add(i, media);
    }

    @Override
    public synchronized void onMediaRemoved(int i, Media media) {
        mMedias.remove(i);
    }

    @Override
    public synchronized void onBrowseEnd() {
        mDone = true;
        notifyAll();
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        notifyAll();
        return false;
    }

    @Override
    public synchronized boolean isCancelled() {
        return false;
    }

    @Override
    public synchronized boolean isDone() {
        return mDone;
    }

    @Override
    public synchronized List<Media> get() throws InterruptedException, ExecutionException {
        try {
            return get(45, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public synchronized List<Media> get(long timeout, @NonNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        wait(unit.toMillis(timeout));
        if (Thread.interrupted()) {
            throw new InterruptedException();
        } else if (!mDone) {
            throw new TimeoutException("Timed out before finished");
        }
        return mMedias;
    }
}
