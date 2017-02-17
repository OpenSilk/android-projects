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

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.*

import org.opensilk.common.app.ScopedService
import org.opensilk.video.VideoApp
import org.opensilk.video.VideoAppComponent
import org.opensilk.video.util.Utils

import java.io.Closeable
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

import javax.inject.Inject

import mortar.MortarScope
import org.opensilk.common.dagger2.getDaggerComponent
import org.opensilk.common.dagger2.withDaggerComponent
import org.opensilk.common.dagger.AppContextComponent
import rx.Observable
import rx.Scheduler
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.android.schedulers.HandlerScheduler
import rx.functions.Func1
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import timber.log.Timber

/**
 * Created by drew on 4/6/16.
 */
class ScannerService : ScopedService() {
    internal lateinit var mHandlerThread: HandlerThread
    internal lateinit var mHandlerScheduler: Scheduler

    private lateinit var mComponent: ScannerServiceComponent
    private lateinit var mSubscriptions: CompositeSubscription

    @Inject internal lateinit var mDbClient: VideosProviderClient
    @Inject internal lateinit var mDataService: DataService
    @Inject internal lateinit var mTMDbLookup: TMDbLookup
    @Inject internal lateinit var mTVDbLookup: TVDbLookup
    @Inject internal lateinit var mNilLookup: NilLookup

    private var mHasNotifiedStart: Boolean = false

    override fun onBuildScope(builder: MortarScope.Builder) {
        val appComponent = getDaggerComponent<VideoAppComponent>(this)
        builder.withDaggerComponent(appComponent.newScannerServiceComponent())
    }

    override fun onBind(intent: Intent): IBinder? {
        return Client(this)
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate()")
        mComponent = getDaggerComponent(this)
        mComponent.inject(this)
        mHandlerThread = HandlerThread("MetadataService", Process.THREAD_PRIORITY_LESS_FAVORABLE)
        mHandlerThread.start()
        mHandlerScheduler = HandlerScheduler.from(Handler(mHandlerThread.looper))
        mSubscriptions = CompositeSubscription()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy()")
        mSubscriptions.unsubscribe()
        mHandlerThread.quitSafely()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand(%s)", intent)
        if (intent == null) {
            return Service.START_NOT_STICKY
        }
        if (!mHasNotifiedStart || intent.hasExtra(EXTRA_REQUEST_NOTIFY)) {
            mHasNotifiedStart = true
            contentResolver.notifyChange(mDbClient.uris().scanStart(), null)
        }
        if (ACTION_SCAN == intent.action && intent.hasExtra(EXTRA_MEDIAITEM)) {
            doScan(intent.getParcelableExtra<MediaBrowser.MediaItem>(EXTRA_MEDIAITEM), startId)
        } else if (ACTION_REMOVE == intent.action && intent.hasExtra(EXTRA_MEDIAITEM)) {
            doRemove(intent.getParcelableExtra<MediaBrowser.MediaItem>(EXTRA_MEDIAITEM), startId)
        } else if (ACTION_FULL_RESCAN == intent.action) {
            doRescanAll(startId)
        }
        return Service.START_NOT_STICKY
    }

    internal fun doRemove(mediaItem: MediaBrowser.MediaItem, startId: Int) {
        val s = Observable.create<Any> { subscriber ->
            //remove
            val num = removeRecursive(mediaItem)
            //one emission
            subscriber.onNext(num)
            subscriber.onCompleted()
        }.subscribeOn(sSubscribeOn).subscribe({ num ->
            Timber.i("doRemove(%s)->[OnNext(%d)]", MediaItemUtil.getMediaTitle(mediaItem), num)
            //re add ourselves
            val b = MediaDescriptionUtil.newBuilder(mediaItem.description)
            b.setExtras(MediaMetaExtras.from(mediaItem).setIndexed(false).bundle)
            val newItem = MediaBrowser.MediaItem(b.build(), mediaItem.flags)
            mDbClient.insertMedia(newItem)
            mDataService.notifyChange(newItem)
        }, { e ->
            Timber.w(e, "Remove %s", MediaItemUtil.getMediaTitle(mediaItem))
            stopSelf(startId)
        }) { stopSelf(startId) }
        mSubscriptions.add(s)
    }

    internal fun removeRecursive(mediaItem: MediaBrowser.MediaItem): Int {
        var numremoved = 0
        val mediaItems = mDbClient.getChildren(mediaItem)
        val cII = mediaItems.listIterator()
        while (cII.hasNext()) {
            val child = cII.next()
            val extras = MediaMetaExtras.from(child)
            if (!extras.isDirectory) {
                numremoved += mDbClient.removeMedia(child)
                cII.remove()
            }
        }
        for (dirChild in mediaItems) {
            numremoved += removeRecursive(dirChild)
        }
        numremoved += mDbClient.removeMedia(mediaItem)
        return numremoved
    }

    internal fun doScan(mediaItem: MediaBrowser.MediaItem, startId: Int) {
        val scanSub = scan(mediaItem).subscribe({ item ->
            //pass
        }, { e ->
            Timber.e(e, "doScan(%s)->[OnError]", MediaItemUtil.getMediaTitle(mediaItem))
            stopSelf(startId)
        }) {
            Timber.d("doScan(%s)->[OnComplete]", MediaItemUtil.getMediaTitle(mediaItem))
            //            getContentResolver().notifyChange(mDbClient.uris().scanComplete(), null);
            stopSelf(startId)
        }
        mSubscriptions.add(scanSub)
    }

    internal fun doRescanAll(startId: Int) {
        val rescanSub = rescanAll().subscribe({ mediaItem ->
            //pass
        }, { e ->
            Timber.e(e, "doRescan->[OnError]")
            stopSelf(startId)
        }) {
            Timber.d("doRescan->[OnComplete]")
            //            getContentResolver().notifyChange(mDbClient.uris().scanComplete(), null);
            stopSelf(startId)
        }
        mSubscriptions.add(rescanSub)
    }

    /**
     * Observes on main thread
     */
    fun rescanAll(): Observable<MediaBrowser.MediaItem> {
        return Observable.create<MediaBrowser.MediaItem> { subscriber ->
            val dirs = mDbClient.topLevelDirectories
            for (mediaItem in dirs) {
                subscriber.onNext(mediaItem)
            }
            subscriber.onCompleted()
        }.flatMap<MediaBrowser.MediaItem>(
                Func1 { this.scanDirectory(it) }
        ).subscribeOn(sSubscribeOn).observeOn(sObserveOn)
    }

    /**
     * Observes on main thread
     */
    fun scan(mediaItem: MediaBrowser.MediaItem): Observable<MediaBrowser.MediaItem> {
        val observable: Observable<MediaBrowser.MediaItem>
        val metaExtras = MediaMetaExtras.from(mediaItem)
        if (metaExtras.isDirectory) {
            observable = scanDirectory(mediaItem).subscribeOn(sSubscribeOn).observeOn(sObserveOn)
        } else if (metaExtras.isVideoFile) {
            observable = lookupVideo(mediaItem).observeOn(sObserveOn)
        } else {
            observable = Observable.error(Exception(String.format(Locale.US,
                    "scan(%s) Unknown mediaType", MediaItemUtil.getMediaTitle(mediaItem))))
        }
        return observable
    }

    internal fun scanDirectory(parentItem: MediaBrowser.MediaItem): Observable<MediaBrowser.MediaItem> {
        val parentExtras = MediaMetaExtras.from(parentItem)
        if (!parentItem.isBrowsable && !parentExtras.isDirectory) {
            return Observable.error<MediaBrowser.MediaItem>(Throwable(String.format(Locale.US,
                    "%s is not a directory.", MediaItemUtil.getMediaTitle(parentItem))))
        }
        Timber.d("scanDirectory(%s)", MediaItemUtil.getMediaTitle(parentItem))
        val parentUri = MediaItemUtil.getMediaUri(parentItem)
        return mDataService.getDirectoryChildrenList(parentItem)
                .flatMap<MediaBrowser.MediaItem>(Func1 { Observable.from(it) })
                .flatMap<MediaBrowser.MediaItem>(Func1 { mediaItem ->
                    val extras = MediaMetaExtras.from(mediaItem)
                    if (extras.isVideoFile) {
                        return@Func1 lookupVideo(mediaItem)
                                .doOnError({ e ->
                                    Timber.w(e, "scanDirectory(%s)->[Ignored.OnError(%s)]",
                                            MediaItemUtil.getMediaTitle(parentItem),
                                            MediaItemUtil.getMediaTitle(mediaItem))
                                }).onExceptionResumeNext(Observable.empty())
                    } else if (extras.isDirectory) {
                        //recursive call
                        return@Func1 scanDirectory(mediaItem)
                    }
                    return@Func1 Observable.empty<MediaBrowser.MediaItem>().doOnCompleted {
                        Timber.w("scanDirectory(%s)->[Ignored mediaItem %s]",
                                MediaItemUtil.getMediaTitle(parentItem),
                                MediaItemUtil.getMediaTitle(mediaItem))
                    }
                }).doOnCompleted {
                    mDbClient.markIndexed(parentUri, true)
                    mDataService.notifyChange(parentItem)
                    //Timber.d("scanDirectory(%s)->[OnComplete]", MediaItemUtil.getMediaTitle(parentItem));
        }
    }

    internal fun lookupVideo(mediaItem: MediaBrowser.MediaItem): Observable<MediaBrowser.MediaItem> {
        //        Timber.d("lookup(%s)", MediaItemUtil.getMediaTitle(mediaItem));
        return getLookupService(mediaItem).lookup(mediaItem)
                .doOnNext { item ->
                    mDbClient.insertMedia(item)
                    Timber.d("lookupVideo(%s)->notifyChange(%s)",
                            MediaItemUtil.getMediaTitle(mediaItem),
                            MediaItemUtil.getMediaUri(mediaItem))
                    mDataService.notifyChange(item)
                }
                //                .doOnCompleted(() -> {
                //                    Timber.d("lookup(%s)->[OnComplete]", MediaItemUtil.getMediaTitle(mediaItem));
                //                })
                //TODO handle synchronization in lookup services and remove this
                .subscribeOn(mHandlerScheduler)
    }

    internal fun getLookupService(mediaItem: MediaBrowser.MediaItem): LookupService {
        val metaExtras = MediaMetaExtras.from(mediaItem)
        if (metaExtras.isTvEpisode && VideoApp.hasTVDBKey()) {
            return mTVDbLookup
        } else if (metaExtras.isMovie && VideoApp.hasTMDBKey()) {
            return mTMDbLookup
        } else {
            return mNilLookup
        }
    }

    class Client(service: ScannerService) : Binder() {
        internal var service: WeakReference<ScannerService>

        init {
            this.service = WeakReference(service)
        }

        fun rescanAll(): Observable<MediaBrowser.MediaItem> {
            val s = service.get() ?: return Observable.error<MediaBrowser.MediaItem>(NullPointerException("lost service reference"))
            return s.rescanAll()
        }

        fun scan(mediaItem: MediaBrowser.MediaItem): Observable<MediaBrowser.MediaItem> {
            val s = service.get() ?: return Observable.error<MediaBrowser.MediaItem>(NullPointerException("lost service reference"))
            return s.scan(mediaItem)
        }
    }

    class Connection(
            context: Context,
            private val serviceConnection: ServiceConnection,
            val client: ScannerService.Client
    ) : Closeable {
        private val context: Context

        init {
            this.context = ContextWrapper(context)
        }

        override fun close() {
            context.unbindService(serviceConnection)
        }
    }

    companion object {

        internal val ACTION_SCAN = "scan"
        internal val ACTION_REMOVE = "remove"
        internal val EXTRA_MEDIAITEM = "media_item"
        internal val ACTION_FULL_RESCAN = "full_rescan"
        internal val EXTRA_REQUEST_NOTIFY = "request_notify"

        fun scan(context: Context, mediaItem: MediaBrowser.MediaItem) {
            val i = Intent(context, ScannerService::class.java)
                    .setAction(ACTION_SCAN)
                    .putExtra(EXTRA_MEDIAITEM, mediaItem)
            context.startService(i)
        }

        fun remove(context: Context, mediaItem: MediaBrowser.MediaItem) {
            val i = Intent(context, ScannerService::class.java)
                    .setAction(ACTION_REMOVE)
                    .putExtra(EXTRA_MEDIAITEM, mediaItem)
            context.startService(i)
        }

        internal var sObserveOn = AndroidSchedulers.mainThread()
        internal var sSubscribeOn = Schedulers.io()

        @Throws(InterruptedException::class)
        fun bindService(context: Context): Connection {
            Utils.ensureNotOnMainThread(context)
            val q = LinkedBlockingQueue<ScannerService.Client>(1)
            val keyChainServiceConnection = object : ServiceConnection {
                @Volatile internal var mConnectedAtLeastOnce = false
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    if (!mConnectedAtLeastOnce) {
                        mConnectedAtLeastOnce = true
                        //Always on space available
                        q.offer(service as ScannerService.Client)
                    }
                }

                override fun onServiceDisconnected(name: ComponentName) {}
            }

            val isBound = context.bindService(Intent(context, ScannerService::class.java),
                    keyChainServiceConnection,
                    Context.BIND_AUTO_CREATE)
            if (!isBound) {
                throw AssertionError("could not bind to KeyChainService")
            }
            return Connection(context, keyChainServiceConnection, q.take())
        }
    }

}
