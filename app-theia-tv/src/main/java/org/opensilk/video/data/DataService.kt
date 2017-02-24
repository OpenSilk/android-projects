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
import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.media.browse.MediaBrowser.MediaItem
import android.media.tv.TvContract
import android.net.Uri
import android.provider.DocumentsContract

import org.apache.commons.lang3.StringUtils
import org.opensilk.common.dagger.ForApplication
import org.opensilk.tmdb.api.model.Movie
import org.opensilk.tvdb.api.model.Episode
import org.opensilk.video.playback.VLCInstance
import org.opensilk.video.util.Utils
import org.videolan.libvlc.Media
import org.videolan.libvlc.util.Extensions

import java.io.IOException
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import javax.inject.Inject
import javax.inject.Singleton

import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.opensilk.media.*
import rx.Observable
import rx.Scheduler
import rx.Single
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.exceptions.Exceptions
import rx.functions.Action1
import rx.functions.Func1
import rx.functions.Func2
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import timber.log.Timber

/**
 * Created by drew on 4/1/16.
 */
@Singleton
class DataService
@Inject
constructor(
        private val mVlcInstance: VLCInstance,
        private val mDbClient: VideosProviderClient,
        @ForApplication private val mAppContext: Context,
        private val mOkClient: OkHttpClient
) {

    private val sSubscribeOn = Schedulers.from(Executors.newFixedThreadPool(16))

    fun getMediaItem(mediaItem: MediaBrowser.MediaItem): Observable<MediaBrowser.MediaItem> {
        return getMediaItem(mediaItem.description)
    }

    /**
     * This observable never completes
     */
    fun getMediaItem(description: MediaDescription): Observable<MediaBrowser.MediaItem> {
        val mediaUri = description._getMediaUri()
        val metaExtras = description._getMediaMeta()
        val observable: Observable<MediaBrowser.MediaItem>
        if (metaExtras.isTvSeries) {
            observable = getTvSeriesInternal(description.mediaId)
        } else {
            observable = getMediaInternal(mediaUri).flatMap({ item ->
                val extras = item._getMediaMeta()
                if (!item.isPlayable || extras.isParsed) {
                    return@flatMap Observable.just(item)
                }
                return@flatMap Observable.using<MediaBrowser.MediaItem, ScannerService.Connection>({
                    ScannerService.bindService(mAppContext)
                }, {
                    connection -> connection.client.scan(item)
                }, ScannerService.Connection::close).startWith(item)
            })
        }
        return observable.subscribeOn(sSubscribeOn).observeOn(sObserveOn)
    }

    /**
     * This Observable does not complete, items will be emitted on change notifications.
     * Used to update the list views.
     */
    fun getMediaItemOnChange(mediaItem: MediaBrowser.MediaItem): rx.Observable<MediaBrowser.MediaItem> {
        return getMediaItemOnChange(mediaItem._getMediaUri())
    }

    /**
     * This Observable does not complete, items will be emitted on change notifications.
     * If lastUpdated is less than last recorded change an item will be emitted immediately
     */
    fun getMediaItemOnChange(mediaItem: MediaBrowser.MediaItem, lastUpdated: Long): rx.Observable<MediaBrowser.MediaItem> {
        val mediaUri = mediaItem._getMediaUri()
        val changeTime = mLastChanged[mediaUri]
        //if media has changed since they were updated send an item immediately
        if (changeTime != null && lastUpdated > 0 && changeTime > lastUpdated) {
            return getMediaInternal(mediaUri).subscribeOn(sSubscribeOn).observeOn(sObserveOn)
        } else {
            return getMediaItemOnChange(mediaUri)
        }
    }

    fun getMediaItemOnChange(mediaUri: Uri): Observable<MediaBrowser.MediaItem> {
        return getMediaUriChanges(mediaUri)
                .map<MediaItem>( Func1 { uri -> mDbClient.getMedia(mediaUri) })
                .observeOn(sObserveOn)
    }

    fun getMediaItemSingle(description: MediaDescription): Single<MediaBrowser.MediaItem> {
        val mediaUri = description._getMediaUri()
        return Single.create<MediaBrowser.MediaItem> { subscriber ->
            val item = mDbClient.getMedia(mediaUri)
            if (subscriber.isUnsubscribed) {
                return@create
            }
            if (item != null) {
                subscriber.onSuccess(item)
            } else {
                subscriber.onError(Exception("Missed database for " + mediaUri))
            }
        }.subscribeOn(sSubscribeOn).observeOn(sObserveOn)
    }

    internal fun getMediaInternal(mediaUri: Uri): Observable<MediaBrowser.MediaItem> {
        return Observable.create<MediaBrowser.MediaItem> { subscriber ->
            subscriber.add(registerMediaUriChanges(mediaUri, Action1 { uri ->
                val item1 = mDbClient.getMedia(mediaUri)
                if (subscriber.isUnsubscribed) {
                    return@Action1
                }
                if (item1 != null) {
                    subscriber.onNext(item1)
                } else {
                    Timber.w("getMediaItem() MediaItem should /really/ be in database by now")
                }
            }))
            val item = mDbClient.getMedia(mediaUri)
            if (subscriber.isUnsubscribed) {
                return@create
            }
            if (item != null) {
                subscriber.onNext(item)
            } else {
                subscriber.onError(Exception("Missed database for " + mediaUri))
            }
        }
    }

    internal fun getTvSeriesInternal(mediaId: String): Observable<MediaBrowser.MediaItem> {
        return Observable.create<MediaBrowser.MediaItem> { subscriber ->
            val id = java.lang.Long.valueOf(mediaId.substring(mediaId.indexOf(':') + 1))!!
            subscriber.add(registerMediaUriChanges(mDbClient.uris().tvSeries(id), Action1 { uri ->
                val item1 = mDbClient.tvdb().getTvSeries(id)
                if (subscriber.isUnsubscribed) {
                    return@Action1
                }
                if (item1 != null) {
                    subscriber.onNext(item1)
                }
            }))
            val item = mDbClient.tvdb().getTvSeries(mediaId)
            if (subscriber.isUnsubscribed) {
                return@create
            }
            if (item != null) {
                subscriber.onNext(item)
            } else {
                subscriber.onError(Exception("Missed database for " + mediaId))
            }
        }
    }

    /**
     * This observable completes ofter one item
     */
    fun getVideoFileInfo(mediaItem: MediaBrowser.MediaItem): Observable<VideoFileInfo> {
        val mediaUri = MediaDescriptionUtil.getMediaUri(mediaItem.description)
        val description = mediaItem.description
        return Observable.using<VideoFileInfo.Builder, Media>({ Media(mVlcInstance.get(), mediaUri) }, { media ->
            Observable.create<VideoFileInfo.Builder> { subscriber ->
                val timeout = Observable.timer(30, TimeUnit.SECONDS)
                        .subscribe { l ->
                            if (!subscriber.isUnsubscribed) {
                                subscriber.onError(TimeoutException("Took too long to parse"))
                            }
                        }
                subscriber.add(timeout)
                media.setEventListener { event ->
                    timeout.unsubscribe() //cancel error task
                    when (event.type) {
                        Media.Event.ParsedChanged -> {
                            val bob = VideoFileInfo.builder(mediaUri)
                                    .setTitle(MediaDescriptionUtil.getMediaTitle(description))
                                    .setDuration(media.duration)
                            for (ii in 0..media.trackCount - 1) {
                                val track = media.getTrack(ii) ?: continue
                                when (track.type) {
                                    Media.Track.Type.Audio -> {
                                        val audioTrack = track as Media.AudioTrack
                                        bob.addAudioTrack(audioTrack.codec, audioTrack.bitrate,
                                                audioTrack.rate, audioTrack.channels)
                                    }
                                    Media.Track.Type.Video -> {
                                        val videoTrack = track as Media.VideoTrack
                                        bob.addVideoTrack(videoTrack.codec, videoTrack.width,
                                                videoTrack.height, videoTrack.bitrate,
                                                videoTrack.frameRateNum, videoTrack.frameRateDen)
                                    }
                                    Media.Track.Type.Text -> {
                                    }//TODO
                                }
                            }
                            if (subscriber.isUnsubscribed) {
                                return@setEventListener
                            }
                            subscriber.onNext(bob)
                            subscriber.onCompleted()
                        }
                        else -> {
                            if (subscriber.isUnsubscribed) {
                                return@setEventListener
                            }
                            subscriber.onError(Exception("unexpected event"))
                        }
                    }
                }
                //TODO parse() doesn't synchronously parse
                media.parseAsync(Media.Parse.ParseNetwork)
            }
        }) { media ->
            media.setEventListener(null)
            media.release()
        }.zipWith<Long, VideoFileInfo>(
                getFileSize(mediaItem).toObservable(),
                Func2 { builder, size -> builder.setSize(size!!).build() }) //no observeon as media callback is posted to main thread.
    }

    fun getFileSize(mediaItem: MediaBrowser.MediaItem?): Single<Long> {
        val mediaUri = MediaItemUtil.getMediaUri(mediaItem)
        return Single.create<Long> { subscriber ->
            var value: Long = 0
            if (mediaItem != null && StringUtils.startsWith(mediaUri.toString(), "http")) {
                val request = Request.Builder()
                        .head()
                        .url(mediaUri.toString())
                        .build()
                val call = mOkClient.newCall(request)
                try {
                    val response = call.execute()
                    val len = response.header("Content-Length")
                    value = java.lang.Long.valueOf(len)!!
                } catch (e: IOException) {
                    Timber.w(e, "getFileSize")
                    value = 0
                } catch (e: NumberFormatException) {
                    Timber.w(e, "getFileSize")
                    value = 0
                } catch (e: NullPointerException) {
                    Timber.w(e, "getFileSize")
                    value = 0
                }

            } else {
                Timber.w("Invalid mediaUri=%=", mediaUri)
            }
            //always success, TODO if server doest support HEAD use GET Range: bytes=0-1
            if (!subscriber.isUnsubscribed) {
                subscriber.onSuccess(value)
            }
        }.doOnSuccess { value ->
            if (value > 0) {
                mDbClient.updateMediaFileSize(mediaUri, value!!)
            }
        }.subscribeOn(sSubscribeOn).observeOn(sObserveOn)
    }

    fun getVideoDescription(mediaItem: MediaBrowser.MediaItem): Observable<VideoDescInfo> {
        return Observable.create<VideoDescInfo> { subscriber ->
            val builder = VideoDescInfo.builder()
                    .setTitle(mediaItem.description.title)
                    .setSubtitle(mediaItem.description.subtitle)
            val metaExtras = mediaItem._getMediaMeta()
            if (metaExtras.isTvEpisode) {
                val e = mDbClient.tvdb().getEpisode(metaExtras.__internal1.toLong())
                if (e != null && e.overview != null) {
                    builder.setOverview(e.overview)
                }
            } else if (metaExtras.isMovie) {
                val m = mDbClient.moviedb().getMovie(metaExtras.__internal3.toLong())
                if (m != null && m.overview != null) {
                    builder.setOverview(m.overview)
                }
            }
            subscriber.onNext(builder.build())
            subscriber.onCompleted()
        } //TODO use subscribeOn?? were /are/ hitting the db
    }

    fun getChildren(parentItem: MediaBrowser.MediaItem): Observable<List<MediaBrowser.MediaItem>> {
        val metaExtras = parentItem._getMediaMeta()
        if (metaExtras.isTvSeries) {
            return getTvSeriesChildren(parentItem)
        } else if (metaExtras.isDirectory) {
            return getDirectoryChildren(parentItem)
        }
        Timber.e("Unimplemented mediatype=%d", metaExtras.mimeType)
        return Observable.empty<List<MediaBrowser.MediaItem>>()
    }

    /**
     * This observable never completes
     */
    fun getTvSeriesChildren(parentItem: MediaBrowser.MediaItem): Observable<List<MediaBrowser.MediaItem>> {
        Timber.d("getTvSeriesChildren(%s)", MediaItemUtil.getMediaTitle(parentItem))
        return Observable.create<List<MediaBrowser.MediaItem>> { subscriber ->
            subscriber.onNext(mDbClient.getTvEpisodes(parentItem.mediaId))
            //todo watch changes
        }.subscribeOn(sSubscribeOn).observeOn(sObserveOn)
    }

    /**
     * This observable never completes
     */
    fun getDirectoryChildren(parentItem: MediaBrowser.MediaItem): Observable<List<MediaBrowser.MediaItem>> {
        Timber.d("getDirectoryChildren(%s)", MediaItemUtil.getMediaTitle(parentItem))
        val parentMediaUri = MediaItemUtil.getMediaUri(parentItem)
        return getMediaUriChanges(parentMediaUri)
                .startWith(parentMediaUri)
                //ignore uri and use passed mediaitem
                .flatMap<List<MediaItem>>( Func1 { uri -> getDirectoryChildrenList(parentItem) })
                .subscribeOn(sSubscribeOn).observeOn(sObserveOn)
    }

    /**
     * This observable completes after one emission.
     * This observable does not utilize subscribeOn() or observeOn().
     */
    fun getDirectoryChildrenList(
            parentItem: MediaBrowser.MediaItem): rx.Observable<List<MediaBrowser.MediaItem>> {
        return Observable.using({
            org.videolan.libvlc.util.MediaBrowser(mVlcInstance.get(), null)
        }, { browser ->
            val parentUri = parentItem._getMediaUri()
            val future = VLCBrowserBrowseFuture.from(browser, parentUri)
            Observable.from(future)
        }, org.videolan.libvlc.util.MediaBrowser::release
        ).map<List<MediaItem>>( Func1 { medias ->
            val indexedItems = mDbClient.getChildren(parentItem)
            Timber.d("getDirectoryChildrenList.Map(%s) mediassize=%d indexedsize=%d",
                    parentItem._getMediaTitle(), medias.size, indexedItems.size)
            val mediaItems = ArrayList<MediaBrowser.MediaItem>(medias.size)
            for (media in medias) {
                var childItem: MediaBrowser.MediaItem? = null
                for (mediaItem in indexedItems) {
                    val uri = mediaItem._getMediaUri()
                    if (media.uri == uri) {
                        childItem = reconcileMedia(media, mediaItem)
                        break
                    }
                }
                if (childItem == null) {
                    childItem = mediaToMediaItem(media, parentItem)
                }
                mediaItems.add(childItem)
            }
            return@Func1 mediaItems
        }).doOnNext { mediaItems ->
            for (mediaItem in mediaItems) {
                val extras = mediaItem._getMediaMeta()
                //If new or on change do a lookup
                if (!extras.isParsed) {
                    mDbClient.insertMedia(mediaItem)
                    if (extras.isVideo) {
                        ScannerService.scan(mAppContext, mediaItem)
                    }
                }
            }
            mDbClient.removeOrphans(parentItem, mediaItems)
        }
    }

    internal val mNotifyWorker: Scheduler.Worker = sSubscribeOn.createWorker()
    internal val mChangeBus = PublishSubject.create<Uri>()
    //i don't know if this is all that smart, the idea here is to provide
    //a mechanism for the lists to update the progress on items. We only watch
    //for changes on items currently bound to a view. this provides a reference
    //so we can push a change to a newly bound view if the item has been updated
    //since the presenter was initially created.
    internal val mLastChanged: MutableMap<Uri, Long> = Collections.synchronizedMap(mutableMapOf())
    internal val mChangeObservable = mChangeBus.doOnNext { uri -> mLastChanged.put(uri, System.currentTimeMillis()) }

    fun getMediaUriChanges(mediaUri: Uri): Observable<Uri> {
        return mChangeObservable.filter { uri -> uri != null && uri == mediaUri }.doOnNext { uri -> Timber.d("onChange(%s)", uri) }
    }

    fun registerMediaUriChanges(mediaUri: Uri, onNext: Action1<Uri>): Subscription {
        return getMediaUriChanges(mediaUri).subscribe { uri -> onNext.call(mediaUri) }
    }

    fun notifyChange(mediaItem: MediaBrowser.MediaItem) {
        notifyChange(MediaItemUtil.getMediaUri(mediaItem))
    }

    fun notifyChange(mediaUri: Uri) {
        mNotifyWorker.schedule {
            Timber.d("notifyChange(%s)", mediaUri)
            mChangeBus.onNext(mediaUri)
        }
    }

    companion object {
        private val sObserveOn = AndroidSchedulers.mainThread()

        //mostly for directories minidlna may reuse an id and we get stuck with the old one
        private fun reconcileMedia(media: Media, mediaItem: MediaBrowser.MediaItem): MediaBrowser.MediaItem {
            val metaExtras = mediaItem._getMediaMeta()
            val mediaTitle = media.getMeta(Media.Meta.Title)
            val itemMediaTitle = metaExtras.displayName
            if (mediaTitle != itemMediaTitle) {
                metaExtras.isParsed = false
                metaExtras.displayName = mediaTitle
                val bob = mediaItem.description._newBuilder()
                if (metaExtras.isDirectory) {
                    bob.setTitle(mediaTitle)
                }
                val newItem = mediaItem._copy(bob, metaExtras)
                Timber.i("Updating mediaTitle %s -> %s", itemMediaTitle, mediaTitle)
                return newItem
            } else {
                return mediaItem
            }
        }

        fun mediaToMediaItem(
                media: Media,
                parentMediaItem: MediaBrowser.MediaItem?
        ): MediaBrowser.MediaItem {
            val title = media.getMeta(Media.Meta.Title)
            val metaExtras = guessMediaType(media)
            if (parentMediaItem != null) {
                metaExtras.parentMediaId = parentMediaItem.mediaId
                val parentExtras = parentMediaItem._getMediaMeta()
                metaExtras.__internal4 = parentExtras.__internal4
                metaExtras.displayName = title
            }
            val builder = MediaDescription.Builder()
                    .setTitle(title)
                    .setMediaId("media:" + media.uri)
            builder._setMediaUri(metaExtras, media.uri)
            builder._setMediaMeta(metaExtras)
            return MediaBrowser.MediaItem(builder.build(), metaExtras.mediaItemFlags)
        }

        private fun guessMediaType(media: Media): MediaMeta {
            val extras = defineMediaType(media)
            if (extras.isVideo) {
                val title = media.getMeta(Media.Meta.Title)
                if (Utils.matchesTvEpisode(title)) {
                    extras.mimeType = TvContract.Programs.CONTENT_ITEM_TYPE
                } else if (Utils.matchesMovie(title)) {
                    extras.mimeType = "vnd.opensilk.org/movie"
                }
            }
            return extras
        }

        private fun defineMediaType(media: Media): MediaMeta {
            val meta = MediaMeta()
            val type = media.type

            if (type == Media.Type.Directory) {
                meta.mimeType = DocumentsContract.Document.MIME_TYPE_DIR
                return meta
            }

            val title = media.getMeta(Media.Meta.Title)
            val uri = media.uri

            //Try title first
            var dotIndex = title?.lastIndexOf(".") ?: -1
            if (dotIndex != -1) {
                val fileExt = title!!.substring(dotIndex).toLowerCase(Locale.ENGLISH)
                if (Extensions.VIDEO.contains(fileExt)) {
                    //TODO extract real mime from extension
                    meta.mimeType = "video/unknown"
                    return meta
                }
            }

            //try uri if no match on title
            val index = uri.toString().indexOf('?')
            val location: String
            if (index == -1) {
                location = uri.toString()
            } else {
                location = uri.toString().substring(0, index)
            }
            dotIndex = location.lastIndexOf(".")
            if (dotIndex != -1) {
                val fileExt = location.substring(dotIndex).toLowerCase(Locale.ENGLISH)
                if (Extensions.VIDEO.contains(fileExt)) {
                    meta.mimeType = "video/unknown"
                    return meta
                }
            }

            //not a video file
            meta.mimeType = "application/unknown"
            return meta
        }
    }

}
