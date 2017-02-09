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

package org.opensilk.video.playback;

import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.net.Uri;
import android.support.annotation.Nullable;

import org.opensilk.video.data.MediaDescriptionUtil;
import org.opensilk.video.data.MediaMetaExtras;
import org.opensilk.video.data.VideosProviderClient;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

/**
 * Created by drew on 4/9/16.
 */
public class PlaybackQueue {

    private final VideosProviderClient mProviderClient;

    private final AtomicLong mIdGenerator = new AtomicLong(1);
    private final AtomicReference<List<MediaSession.QueueItem>> mQueue = new AtomicReference<>();
    private final AtomicInteger mPosition = new AtomicInteger();
    private String mTitle;

    @Inject
    public PlaybackQueue(VideosProviderClient mProviderClient) {
        this.mProviderClient = mProviderClient;
    }

    public void loadFromUri(Uri mediaUri) {
        MediaBrowser.MediaItem mediaItem = mProviderClient.getMedia(mediaUri);
        if (mediaItem == null) {
            throw new IllegalArgumentException("Unable to find " + mediaUri + " in database");
        }
        MediaMetaExtras metaExtras = MediaMetaExtras.from(mediaItem.getDescription());
        List<MediaBrowser.MediaItem> siblings = mProviderClient.getChildren(metaExtras.getParentUri());
        ListIterator<MediaBrowser.MediaItem> siblingII = siblings.listIterator();
        while (siblingII.hasNext()) {
            Uri siblingUri = MediaDescriptionUtil.getMediaUri(siblingII.next().getDescription());
            if (mediaUri.equals(siblingUri)) {
                mPosition.set(siblingII.previousIndex());
                break;
            }
        }
        List<MediaSession.QueueItem> newQueue = new ArrayList<>(siblings.size());
        for (MediaBrowser.MediaItem sibling : siblings) {
            newQueue.add(new MediaSession.QueueItem(sibling.getDescription(), mIdGenerator.getAndIncrement()));
        }
        mQueue.set(newQueue);
        mTitle = "Up Next";
    }

    public void moveToItem(long id) {
        List<MediaSession.QueueItem> queue = mQueue.get();
        if (queue == null) {
            return;
        }
        ListIterator<MediaSession.QueueItem> qII = queue.listIterator();
        while (qII.hasNext()) {
            if (qII.next().getQueueId() == id) {
                mPosition.set(qII.previousIndex());
                break;
            }
        }
    }

    public @Nullable MediaSession.QueueItem getCurrent() {
        List<MediaSession.QueueItem> queue = mQueue.get();
        if (queue == null) {
            return null;
        }
        int pos = mPosition.get();
        if (pos >= queue.size() || pos < 0) {
            return null;
        }
        return queue.get(pos);
    }

    public @Nullable MediaSession.QueueItem getNext() {
        List<MediaSession.QueueItem> queue = mQueue.get();
        if (queue == null) {
            return null;
        }
        int nextPos = mPosition.incrementAndGet();
        if (nextPos >= queue.size() || nextPos < 0) {
            return null;
        }
        return queue.get(nextPos);
    }

    public @Nullable MediaSession.QueueItem getPrevious() {
        List<MediaSession.QueueItem> queue = mQueue.get();
        if (queue == null) {
            return null;
        }
        if (queue.isEmpty()) {
            return null;
        }
        int newPos = mPosition.decrementAndGet();
        if (newPos >= queue.size() || newPos < 0) {
            return null;
        }
        return queue.get(newPos);
    }

    public @Nullable List<MediaSession.QueueItem> getQueue() {
        List<MediaSession.QueueItem> queue = mQueue.get();
        if (queue == null) {
            return null;
        }
        int pos = mPosition.get();
        if (pos >= queue.size() || pos < 0) {
            return queue;
        } else {
            //truncate so current item is first in list
            return queue.subList(pos, queue.size());
        }
    }

    public @Nullable String getTitle() {
        return mTitle;
    }

}
