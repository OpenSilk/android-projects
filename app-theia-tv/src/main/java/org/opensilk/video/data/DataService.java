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
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.tmdb.api.model.Movie;
import org.opensilk.tvdb.api.model.Episode;
import org.opensilk.video.playback.VLCInstance;
import org.opensilk.video.util.Utils;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.util.Extensions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Scheduler;
import rx.Single;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.Exceptions;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import timber.log.Timber;

/**
 * Created by drew on 4/1/16.
 */
@Singleton
public class DataService {

    private final Scheduler sSubscribeOn = Schedulers.from(Executors.newFixedThreadPool(16));
    private static final Scheduler sObserveOn = AndroidSchedulers.mainThread();

    private final VLCInstance mVlcInstance;
    private final VideosProviderClient mDbClient;
    private final Context mAppContext;
    private final OkHttpClient mOkClient;

    @Inject
    public DataService(
            VLCInstance mVlcInstance,
            VideosProviderClient mDbClient,
            @ForApplication Context mAppContext,
            OkHttpClient mOkClient
    ) {
        this.mVlcInstance = mVlcInstance;
        this.mDbClient = mDbClient;
        this.mAppContext = mAppContext;
        this.mOkClient = mOkClient;
    }


    public Observable<MediaBrowser.MediaItem> getMediaItem(final MediaBrowser.MediaItem mediaItem) {
        return getMediaItem(mediaItem.getDescription());
    }

    /**
     * This observable never completes
     */
    public Observable<MediaBrowser.MediaItem> getMediaItem(final MediaDescription description) {
        final Uri mediaUri = MediaDescriptionUtil.getMediaUri(description);
        final MediaMetaExtras metaExtras = MediaMetaExtras.from(description);
        Observable<MediaBrowser.MediaItem> observable;
        if (mediaUri == null) {
            if (metaExtras.isTvSeries()) {
                observable = getTvSeriesInternal(description.getMediaId());
            } else {
                observable = Observable.error(new Exception("Unimplemented mediaType=" + metaExtras.getMediaType()));
            }
        } else {
            //TODO the lookup causes a notify which results in a second (redundant) emission
            observable = getMediaInternal(mediaUri).flatMap(item -> {
                MediaMetaExtras extas = MediaMetaExtras.from(item);
                if (!item.isPlayable() || extas.isIndexed()) {
                    return Observable.just(item);
                }
                return Observable.<MediaBrowser.MediaItem, ScannerService.Connection>using(() -> {
                    try {
                        return ScannerService.bindService(mAppContext);
                    } catch (InterruptedException e) {
                        throw Exceptions.propagate(e);
                    }
                }, connection -> {
                    return connection.getClient().scan(item);
                }, connection -> {
                    connection.close();
                });
            });
        }
        return observable.subscribeOn(sSubscribeOn).observeOn(sObserveOn);
    }

    /**
     * This Observable does not complete, items will be emitted on change notifications.
     * Used to update the list views.
     */
    public rx.Observable<MediaBrowser.MediaItem> getMediaItemOnChange(MediaBrowser.MediaItem mediaItem) {
        Uri mediaUri = MediaItemUtil.getMediaUri(mediaItem);
        if (mediaUri == null) {
            return rx.Observable.error(new NullPointerException("Null media uri title="
                    + MediaItemUtil.getMediaTitle(mediaItem)));
        }
        return getMediaItemOnChange(mediaUri);
    }

    /**
     * This Observable does not complete, items will be emitted on change notifications.
     * If lastUpdated is less than last recorded change an item will be emitted immediately
     */
    public rx.Observable<MediaBrowser.MediaItem> getMediaItemOnChange(MediaBrowser.MediaItem mediaItem, long lastUpdated) {
        Uri mediaUri = MediaItemUtil.getMediaUri(mediaItem);
        if (mediaUri == null) {
            return rx.Observable.error(new NullPointerException("Null media uri title="
                    + MediaItemUtil.getMediaTitle(mediaItem)));
        }
        Long changeTime = mLastChanged.get(mediaUri);
        //if media has changed since they were updated send an item immediately
        if (changeTime != null && lastUpdated > 0 && changeTime > lastUpdated) {
            return getMediaInternal(mediaUri).subscribeOn(sSubscribeOn).observeOn(sObserveOn);
        } else {
            return getMediaItemOnChange(mediaUri);
        }
    }

    public Observable<MediaBrowser.MediaItem> getMediaItemOnChange(final Uri mediaUri) {
        return getMediaUriChanges(mediaUri)
                .map(uri -> mDbClient.getMedia(mediaUri))
                .observeOn(sObserveOn);
    }

    public Single<MediaBrowser.MediaItem> getMediaItemSingle(final MediaDescription description) {
        final Uri mediaUri = MediaDescriptionUtil.getMediaUri(description);
        return Single.<MediaBrowser.MediaItem>create(subscriber -> {
            MediaBrowser.MediaItem item = mDbClient.getMedia(mediaUri);
            if (subscriber.isUnsubscribed()) {
                return;
            }
            if (item != null) {
                subscriber.onSuccess(item);
            } else {
                subscriber.onError(new Exception("Missed database for " + mediaUri));
            }
        }).subscribeOn(sSubscribeOn).observeOn(sObserveOn);
    }

    Observable<MediaBrowser.MediaItem> getMediaInternal(final Uri mediaUri) {
        return Observable.<MediaBrowser.MediaItem>create(subscriber -> {
            subscriber.add(registerMediaUriChanges(mediaUri, uri -> {
                MediaBrowser.MediaItem item1 = mDbClient.getMedia(mediaUri);
                if (subscriber.isUnsubscribed()) {
                    return;
                }
                if (item1 != null) {
                    subscriber.onNext(item1);
                } else {
                    Timber.w("getMediaItem() MediaItem should /really/ be in database by now");
                }
            }));
            MediaBrowser.MediaItem item = mDbClient.getMedia(mediaUri);
            if (subscriber.isUnsubscribed()) {
                return;
            }
            if (item != null) {
                subscriber.onNext(item);
            } else {
                subscriber.onError(new Exception("Missed database for " + mediaUri));
            }
        });
    }

    Observable<MediaBrowser.MediaItem> getTvSeriesInternal(final String mediaId) {
        return Observable.create(subscriber -> {
            final long id = Long.valueOf(mediaId.substring(mediaId.indexOf(':') + 1));
            subscriber.add(registerMediaUriChanges(mDbClient.uris().tvSeries(id), uri -> {
                MediaBrowser.MediaItem item1 = mDbClient.tvdb().getTvSeries(id);
                if (subscriber.isUnsubscribed()) {
                    return;
                }
                if (item1 != null) {
                    subscriber.onNext(item1);
                }
            }));
            MediaBrowser.MediaItem item = mDbClient.tvdb().getTvSeries(mediaId);
            if (subscriber.isUnsubscribed()) {
                return;
            }
            if (item != null) {
                subscriber.onNext(item);
            } else {
                subscriber.onError(new Exception("Missed database for " + mediaId));
            }
        });
    }

    /**
     * This observable completes ofter one item
     */
    public Observable<VideoFileInfo> getVideoFileInfo(final MediaBrowser.MediaItem mediaItem) {
        final Uri mediaUri = MediaDescriptionUtil.getMediaUri(mediaItem.getDescription());
        final MediaDescription description = mediaItem.getDescription();
        return Observable.<VideoFileInfo.Builder, Media>using(() -> {
            return new Media(mVlcInstance.get(), mediaUri);
        }, media -> {
            return Observable.create(subscriber -> {
                final Subscription timeout = Observable.timer(30, TimeUnit.SECONDS)
                        .subscribe(l -> {
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onError(new TimeoutException("Took too long to parse"));
                            }
                        });
                subscriber.add(timeout);
                media.setEventListener(event -> {
                    timeout.unsubscribe(); //cancel error task
                    switch (event.type) {
                        case Media.Event.ParsedChanged: {
                            VideoFileInfo.Builder bob = VideoFileInfo.builder(mediaUri)
                                    .setTitle(MediaDescriptionUtil.getMediaTitle(description))
                                    .setDuration(media.getDuration())
                                    ;
                            for (int ii=0; ii<media.getTrackCount(); ii++) {
                                Media.Track track = media.getTrack(ii);
                                if (track == null) {
                                    continue;
                                }
                                switch (track.type) {
                                    case Media.Track.Type.Audio: {
                                        Media.AudioTrack audioTrack = (Media.AudioTrack) track;
                                        bob.addAudioTrack(audioTrack.codec, audioTrack.bitrate,
                                                audioTrack.rate, audioTrack.channels);
                                        break;
                                    }
                                    case Media.Track.Type.Video: {
                                        Media.VideoTrack videoTrack = (Media.VideoTrack) track;
                                        bob.addVideoTrack(videoTrack.codec, videoTrack.width,
                                                videoTrack.height, videoTrack.bitrate,
                                                videoTrack.frameRateNum, videoTrack.frameRateDen);
                                        break;
                                    }
                                    case Media.Track.Type.Text: {
                                        //TODO
                                        break;
                                    }
                                }
                            }
                            if (subscriber.isUnsubscribed()) {
                                return;
                            }
                            subscriber.onNext(bob);
                            subscriber.onCompleted();
                            break;
                        }
                        default: {
                            if (subscriber.isUnsubscribed()) {
                                return;
                            }
                            subscriber.onError(new Exception("unexpected event"));
                            break;
                        }
                    }
                });
                //TODO parse() doesn't synchronously parse
                media.parseAsync(Media.Parse.ParseNetwork);
            });
        }, media -> {
                media.setEventListener(null);
                media.release();
        }).zipWith(getFileSize(mediaItem).toObservable(), (builder, size) -> {
            return builder.setSize(size).build();
        }); //no observeon as media callback is posted to main thread.
    }

    public Single<Long> getFileSize(MediaBrowser.MediaItem mediaItem) {
        final Uri mediaUri = MediaItemUtil.getMediaUri(mediaItem);
        return Single.<Long>create(subscriber -> {
            long value = 0;
            if (mediaItem != null && StringUtils.startsWith(mediaUri.toString(), "http")) {
                Request request = new Request.Builder()
                        .head()
                        .url(mediaUri.toString())
                        .build();
                Call call = mOkClient.newCall(request);
                try {
                    Response response = call.execute();
                    String len = response.header("Content-Length");
                    value = Long.valueOf(len);
                } catch (IOException|NumberFormatException|NullPointerException e) {
                    Timber.w(e, "getFileSize");
                    value = 0;
                }
            } else {
                Timber.w("Invalid mediaUri=%=", mediaUri);
            }
            //always success, TODO if server doest support HEAD use GET Range: bytes=0-1
            if (!subscriber.isUnsubscribed()) {
                subscriber.onSuccess(value);
            }
        }).doOnSuccess(value -> {
            if (value > 0) {
                mDbClient.updateMediaFileSize(mediaUri, value);
            }
        }).subscribeOn(sSubscribeOn).observeOn(sObserveOn);
    }

    public Observable<VideoDescInfo> getVideoDescription(final MediaBrowser.MediaItem mediaItem) {
        return Observable.create(subscriber -> {
            VideoDescInfo.Builder builder = VideoDescInfo.builder()
                    .setTitle(mediaItem.getDescription().getTitle())
                    .setSubtitle(mediaItem.getDescription().getSubtitle())
                    ;
            MediaMetaExtras metaExtras = MediaMetaExtras.from(mediaItem);
            if (metaExtras.isTvEpisode()) {
                Episode e = mDbClient.tvdb().getEpisode(metaExtras.getEpisodeId());
                if (e != null && e.getOverview() != null) {
                    builder.setOverview(e.getOverview());
                }
            } else if (metaExtras.isMovie()) {
                Movie m = mDbClient.moviedb().getMovie(metaExtras.getMovieId());
                if (m != null && m.getOverview() != null) {
                    builder.setOverview(m.getOverview());
                }
            }
            subscriber.onNext(builder.build());
            subscriber.onCompleted();
        }); //TODO use subscribeOn?? were /are/ hitting the db
    }

    public Observable<List<MediaBrowser.MediaItem>> getChildren(final MediaBrowser.MediaItem parentItem) {
        MediaMetaExtras metaExtras = MediaMetaExtras.from(parentItem.getDescription());
        if (metaExtras.isTvSeries()) {
            return getTvSeriesChildren(parentItem);
        } else if (metaExtras.isDirectory()) {
            return getDirectoryChildren(parentItem);
        }
        Timber.d("Unimplemented mediatype=%d", metaExtras.getMediaType());
        return Observable.empty();
    }

    /**
     * This observable never completes
     */
    public Observable<List<MediaBrowser.MediaItem>> getTvSeriesChildren(final MediaBrowser.MediaItem parentItem) {
        Timber.d("getTvSeriesChildren(%s)", MediaItemUtil.getMediaTitle(parentItem));
        return Observable.<List<MediaBrowser.MediaItem>>create(subscriber -> {
            subscriber.onNext(mDbClient.getTvEpisodes(parentItem.getMediaId()));
            //todo watch changes
        }).subscribeOn(sSubscribeOn).observeOn(sObserveOn);
    }

    /**
     * This observable never completes
     */
    public Observable<List<MediaBrowser.MediaItem>> getDirectoryChildren(final MediaBrowser.MediaItem parentItem) {
        Timber.d("getDirectoryChildren(%s)", MediaItemUtil.getMediaTitle(parentItem));
        Uri parentMediaUri = MediaItemUtil.getMediaUri(parentItem);
        return getMediaUriChanges(parentMediaUri)
                .startWith(parentMediaUri)
                //ignore uri and use passed mediaitem
                .flatMap(uri -> getDirectoryChildrenList(parentItem))
                .subscribeOn(sSubscribeOn).observeOn(sObserveOn);
    }

    /**
     * This observable completes after one emission.
     * This observable does not utilize subscribeOn() or observeOn().
     */
    public rx.Observable<List<MediaBrowser.MediaItem>> getDirectoryChildrenList(
            final MediaBrowser.MediaItem parentItem) {
        return Observable.<List<Media>, org.videolan.libvlc.util.MediaBrowser>using(() -> {
            return new org.videolan.libvlc.util.MediaBrowser(mVlcInstance.get(), null);
        }, browser -> {
            Uri parentUri = MediaItemUtil.getMediaUri(parentItem);
            Future<List<Media>> future = VLCBrowserBrowseFuture.from(browser, parentUri);
            return Observable.from(future);
        }, browser -> {
            browser.release();
        }).map(medias -> {
            List<MediaBrowser.MediaItem> indexedItems = mDbClient.getChildren(parentItem);
            Timber.d("getDirectoryChildrenList.Map(%s) mediassize=%d indexedsize=%d",
                    MediaItemUtil.getMediaTitle(parentItem), medias.size(), indexedItems.size());
            List<MediaBrowser.MediaItem> mediaItems = new ArrayList<>(medias.size());
            for (Media media : medias) {
                MediaBrowser.MediaItem childItem = null;
                for (MediaBrowser.MediaItem mediaItem : indexedItems) {
                    Uri uri = MediaItemUtil.getMediaUri(mediaItem);
                    if (media.getUri().equals(uri)) {
                        childItem = reconcileMedia(media, mediaItem);
                        break;
                    }
                }
                if (childItem == null) {
                    childItem = mediaToMediaItem(media, parentItem);
                }
                mediaItems.add(childItem);
            }
            return mediaItems;
        }).doOnNext(mediaItems -> {
            for (MediaBrowser.MediaItem mediaItem : mediaItems) {
                MediaMetaExtras extras = MediaMetaExtras.from(mediaItem);
                //If new or on change do a lookup
                if (!extras.isIndexed() || extras.isDirty()) {
                    mDbClient.insertMedia(mediaItem);
                    if (extras.isVideoFile()) {
                        ScannerService.scan(mAppContext, mediaItem);
                    }
                }
            }
            mDbClient.removeOrphans(parentItem, mediaItems);
        });
    }

    //mostly for directories minidlna may reuse an id and we get stuck with the old one
    private static MediaBrowser.MediaItem reconcileMedia(Media media, MediaBrowser.MediaItem mediaItem) {
        MediaMetaExtras metaExtras = MediaMetaExtras.from(mediaItem);
        String mediaTitle = media.getMeta(Media.Meta.Title);
        String itemMediaTitle = metaExtras.getMediaTitle();
        if (!StringUtils.isEmpty(mediaTitle) && !StringUtils.equals(mediaTitle, itemMediaTitle)) {
            metaExtras.setDirty(true);
            metaExtras.setMediaTitle(mediaTitle);
            MediaDescription.Builder bob = MediaDescriptionUtil.newBuilder(mediaItem.getDescription());
            if (metaExtras.isDirectory()) {
                bob.setTitle(mediaTitle);
            }
            bob.setExtras(metaExtras.getBundle());
            MediaBrowser.MediaItem newItem = new MediaBrowser.MediaItem(bob.build(), mediaItem.getFlags());
            Timber.i("Updating mediaTitle %s -> %s", itemMediaTitle, mediaTitle);
            return newItem;
        } else {
            return mediaItem;
        }
    }

    public static MediaBrowser.MediaItem mediaToMediaItem(
            @NonNull final Media media,
            @Nullable final MediaBrowser.MediaItem parentMediaItem
    ) {
        final String title = media.getMeta(Media.Meta.Title);
        final MediaMetaExtras metaExtras = guessMediaType(media);
        if (parentMediaItem != null) {
            metaExtras.setParentId(parentMediaItem.getMediaId());
            metaExtras.setParentUri(MediaItemUtil.getMediaUri(parentMediaItem));
            final MediaMetaExtras parentExtras = MediaMetaExtras.from(parentMediaItem);
            metaExtras.setServerId(parentExtras.getServerId());
            metaExtras.setMediaTitle(title);
        }
        final MediaDescription.Builder builder = new MediaDescription.Builder()
                .setTitle(title)
                .setMediaId("media:" + media.getUri())
                ;
        MediaDescriptionUtil.setMediaUri(builder, metaExtras, media.getUri());
        builder.setExtras(metaExtras.getBundle());
        int flags = 0;
        if (metaExtras.isDirectory()) {
            flags = MediaBrowser.MediaItem.FLAG_BROWSABLE;
        } else if (!metaExtras.isUnknown()) {
            flags = MediaBrowser.MediaItem.FLAG_PLAYABLE;
        }
        return new MediaBrowser.MediaItem(builder.build(),flags);
    }

    private static MediaMetaExtras guessMediaType(Media media) {
        MediaMetaExtras extras = defineMediaType(media);
        if (extras.isVideo()) {
            String title = media.getMeta(Media.Meta.Title);
            if (Utils.matchesTvEpisode(title)) {
                return MediaMetaExtras.tvEpisode();
            } else if (Utils.matchesMovie(title)) {
                return MediaMetaExtras.movie();
            }
        }
        return extras;
    }

    private static MediaMetaExtras defineMediaType(Media media) {
        final int type = media.getType();

        if (type == Media.Type.Directory) {
            return MediaMetaExtras.directory();
        }

        final String title = media.getMeta(Media.Meta.Title);
        final Uri uri = media.getUri();

        //Try title first
        int dotIndex = title != null ? title.lastIndexOf(".") : -1;
        if (dotIndex != -1) {
            final String fileExt = title.substring(dotIndex).toLowerCase(Locale.ENGLISH);
            if (Extensions.VIDEO.contains(fileExt)) {
                return MediaMetaExtras.video();
            }
        }

        //try uri if no match on title
        final int index = uri.toString().indexOf('?');
        String location;
        if (index == -1) {
            location = uri.toString();
        } else {
            location = uri.toString().substring(0, index);
        }
        dotIndex = location.lastIndexOf(".");
        if (dotIndex != -1) {
            final String fileExt = location.substring(dotIndex).toLowerCase(Locale.ENGLISH);
            if (Extensions.VIDEO.contains(fileExt)) {
                return MediaMetaExtras.video();
            }
        }

        //not a video file
        return MediaMetaExtras.unknown();
    }

    final Scheduler.Worker mNotifyWorker = sSubscribeOn.createWorker();
    final PublishSubject<Uri> mChangeBus = PublishSubject.create();
    //i don't know if this is all that smart, the idea here is to provide
    //a mechanism for the lists to update the progress on items. We only watch
    //for changes on items currently bound to a view. this provides a reference
    //so we can push a change to a newly bound view if the item has been updated
    //since the presenter was initially created.
    final Map<Uri, Long> mLastChanged = Collections.synchronizedMap(new HashMap<>());
    final Observable<Uri> mChangeObservable = mChangeBus.doOnNext(uri -> {
        mLastChanged.put(uri, System.currentTimeMillis());
    });

    public Observable<Uri> getMediaUriChanges(final Uri mediaUri) {
        return mChangeObservable.filter(uri -> {
            return uri != null && uri.equals(mediaUri);
        }).doOnNext(uri -> {
            Timber.d("onChange(%s)", uri);
        });
    }

    public Subscription registerMediaUriChanges(final Uri mediaUri, final Action1<Uri> onNext) {
        return getMediaUriChanges(mediaUri).subscribe(uri -> {
            onNext.call(mediaUri);
        });
    }

    public void notifyChange(MediaBrowser.MediaItem mediaItem) {
        notifyChange(MediaItemUtil.getMediaUri(mediaItem));
    }

    public void notifyChange(final Uri mediaUri) {
        mNotifyWorker.schedule(() -> {
            Timber.d("notifyChange(%s)", mediaUri);
            mChangeBus.onNext(mediaUri);
        });
    }

}
