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

import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.net.Uri;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.dagger.ActivityScope;
import org.opensilk.common.core.rx.RxLoader2;
import org.opensilk.video.playback.VLCInstance;
import org.videolan.libvlc.Media;

import javax.inject.Inject;

import rx.Observable;
import timber.log.Timber;

/**
 * Created by drew on 4/10/16.
 */
@ActivityScope
public class UpnpDevicesLoader implements RxLoader2<MediaBrowser.MediaItem> {

    final VLCInstance mVlcInstance;

    @Inject
    public UpnpDevicesLoader(VLCInstance mVlcInstance) {
        this.mVlcInstance = mVlcInstance;
    }

    @Override
    public Observable<MediaBrowser.MediaItem> getObservable() {
        return Observable.<Media, org.videolan.libvlc.util.MediaBrowser>using(() -> {
            return new org.videolan.libvlc.util.MediaBrowser(mVlcInstance.get(), null);
        }, browser -> {
            return Observable.create(subscriber -> {
                browser.changeEventListener(new VLCBrowserEventListener() {
                    @Override
                    public void onMediaAdded(int i, Media media) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(media);
                        }
                    }

                    @Override
                    public void onBrowseEnd() {
                        Timber.d("onBrowseEnd()");
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onCompleted();
                        }
                    }
                });
                browser.discoverNetworkShares("upnp");
            });
        }, browser -> {
            browser.release();
        }).map(media -> {
            final String title = media.getMeta(Media.Meta.Title);
            final String artworkUrl = media.getMeta(Media.Meta.ArtworkURL);
            final MediaMetaExtras metaExtras = MediaMetaExtras.directory();
            metaExtras.setServerId(media.getMeta(Media.Meta.Description));
            metaExtras.setMediaTitle(title);
            final MediaDescription.Builder builder = new MediaDescription.Builder()
                    .setTitle(title)
                    .setMediaId("directory:"+media.getUri())
                    ;
            MediaDescriptionUtil.setMediaUri(builder, metaExtras, media.getUri());
            builder.setExtras(metaExtras.getBundle());
            if (!StringUtils.isEmpty(artworkUrl)) {
                builder.setIconUri(Uri.parse(artworkUrl));
            }
            return new MediaBrowser.MediaItem(builder.build(), MediaBrowser.MediaItem.FLAG_BROWSABLE);
        });
    }

}
