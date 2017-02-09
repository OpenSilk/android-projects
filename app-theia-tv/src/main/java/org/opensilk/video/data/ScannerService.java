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

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.support.annotation.Nullable;

import org.opensilk.common.core.app.ScopedService;
import org.opensilk.common.dagger.DaggerService;
import org.opensilk.video.VideoApp;
import org.opensilk.video.VideoAppComponent;
import org.opensilk.video.util.Utils;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;

import mortar.MortarScope;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.android.schedulers.HandlerScheduler;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by drew on 4/6/16.
 */
public class ScannerService extends ScopedService {

    static final String ACTION_SCAN = "scan";
    static final String ACTION_REMOVE = "remove";
    static final String EXTRA_MEDIAITEM = "media_item";
    static final String ACTION_FULL_RESCAN = "full_rescan";
    static final String EXTRA_REQUEST_NOTIFY = "request_notify";

    public static void scan(Context context, MediaBrowser.MediaItem mediaItem) {
        Intent i = new Intent(context, ScannerService.class)
                .setAction(ACTION_SCAN)
                .putExtra(EXTRA_MEDIAITEM, mediaItem);
        context.startService(i);
    }

    public static void remove(Context context, MediaBrowser.MediaItem mediaItem) {
        Intent i = new Intent(context, ScannerService.class)
                .setAction(ACTION_REMOVE)
                .putExtra(EXTRA_MEDIAITEM, mediaItem);
        context.startService(i);
    }

    static Scheduler sObserveOn = AndroidSchedulers.mainThread();
    static Scheduler sSubscribeOn = Schedulers.io();
    HandlerThread mHandlerThread;
    Scheduler mHandlerScheduler;

    private ScannerServiceComponent mComponent;
    private final CompositeSubscription mSubscriptions = new CompositeSubscription();

    @Inject VideosProviderClient mDbClient;
    @Inject DataService mDataService;
    @Inject TMDbLookup mTMDbLookup;
    @Inject TVDbLookup mTVDbLookup;
    @Inject NilLookup mNilLookup;

    private boolean mHasNotifiedStart;

    @Override
    protected void onBuildScope(MortarScope.Builder builder) {
        VideoAppComponent appComponent = DaggerService.getDaggerComponent(getApplicationContext());
        builder.withService(DaggerService.DAGGER_SERVICE, appComponent.newScannerServiceComponent());
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) {
        return new Client(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("onCreate()");
        mComponent = DaggerService.getDaggerComponent(this);
        mComponent.inject(this);
        mHandlerThread = new HandlerThread("MetadataService", Process.THREAD_PRIORITY_LESS_FAVORABLE);
        mHandlerThread.start();
        mHandlerScheduler = HandlerScheduler.from(new Handler(mHandlerThread.getLooper()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("onDestroy()");
        mSubscriptions.unsubscribe();
        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("onStartCommand(%s)", intent);
        if (intent == null) {
            return START_NOT_STICKY;
        }
        if (!mHasNotifiedStart || intent.hasExtra(EXTRA_REQUEST_NOTIFY)) {
            mHasNotifiedStart = true;
            getContentResolver().notifyChange(mDbClient.uris().scanStart(), null);
        }
        if (ACTION_SCAN.equals(intent.getAction()) && intent.hasExtra(EXTRA_MEDIAITEM)) {
            doScan(intent.getParcelableExtra(EXTRA_MEDIAITEM), startId);
        } else if (ACTION_REMOVE.equals(intent.getAction()) && intent.hasExtra(EXTRA_MEDIAITEM)) {
            doRemove(intent.getParcelableExtra(EXTRA_MEDIAITEM), startId);
        } else if (ACTION_FULL_RESCAN.equals(intent.getAction())) {
            doRescanAll(startId);
        }
        return START_NOT_STICKY;
    }

    void doRemove(MediaBrowser.MediaItem mediaItem, int startId) {
        Subscription s = Observable.create(subscriber -> {
            //remove
            int num = removeRecursive(mediaItem);
            //one emission
            subscriber.onNext(num);
            subscriber.onCompleted();
        }).subscribeOn(sSubscribeOn).subscribe(num -> {
            Timber.i("doRemove(%s)->[OnNext(%d)]", MediaItemUtil.getMediaTitle(mediaItem), num);
            //re add ourselves
            MediaDescription.Builder b = MediaDescriptionUtil.newBuilder(mediaItem.getDescription());
            b.setExtras(MediaMetaExtras.from(mediaItem).setIndexed(false).getBundle());
            MediaBrowser.MediaItem newItem = new MediaBrowser.MediaItem(b.build(), mediaItem.getFlags());
            mDbClient.insertMedia(newItem);
            mDataService.notifyChange(newItem);
        }, e -> {
            Timber.w(e, "Remove %s", MediaItemUtil.getMediaTitle(mediaItem));
            stopSelf(startId);
        }, () -> {
            stopSelf(startId);
        });
        mSubscriptions.add(s);
    }

    int removeRecursive(MediaBrowser.MediaItem mediaItem) {
        int numremoved = 0;
        List<MediaBrowser.MediaItem> mediaItems = mDbClient.getChildren(mediaItem);
        ListIterator<MediaBrowser.MediaItem> cII = mediaItems.listIterator();
        while (cII.hasNext()) {
            MediaBrowser.MediaItem child = cII.next();
            MediaMetaExtras extras = MediaMetaExtras.from(child);
            if (!extras.isDirectory()) {
                numremoved += mDbClient.removeMedia(child);
                cII.remove();
            }
        }
        for (MediaBrowser.MediaItem dirChild : mediaItems) {
            numremoved += removeRecursive(dirChild);
        }
        numremoved += mDbClient.removeMedia(mediaItem);
        return numremoved;
    }

    void doScan(MediaBrowser.MediaItem mediaItem, int startId) {
        Subscription scanSub = scan(mediaItem).subscribe(item -> {
            //pass
        }, e -> {
            Timber.e(e, "doScan(%s)->[OnError]", MediaItemUtil.getMediaTitle(mediaItem));
            stopSelf(startId);
        }, () -> {
            Timber.d("doScan(%s)->[OnComplete]", MediaItemUtil.getMediaTitle(mediaItem));
//            getContentResolver().notifyChange(mDbClient.uris().scanComplete(), null);
            stopSelf(startId);
        });
        mSubscriptions.add(scanSub);
    }

    void doRescanAll(final int startId) {
        Subscription rescanSub = rescanAll().subscribe((mediaItem) -> {
            //pass
        }, e -> {
            Timber.e(e, "doRescan->[OnError]");
            stopSelf(startId);
        }, () -> {
            Timber.d("doRescan->[OnComplete]");
//            getContentResolver().notifyChange(mDbClient.uris().scanComplete(), null);
            stopSelf(startId);
        });
        mSubscriptions.add(rescanSub);
    }

    /**
     * Observes on main thread
     */
    public Observable<MediaBrowser.MediaItem> rescanAll() {
        return Observable.<MediaBrowser.MediaItem>create(subscriber -> {
            List<MediaBrowser.MediaItem> dirs = mDbClient.getTopLevelDirectories();
            for (MediaBrowser.MediaItem mediaItem : dirs) {
                subscriber.onNext(mediaItem);
            }
            subscriber.onCompleted();
        }).flatMap(this::scanDirectory).subscribeOn(sSubscribeOn).observeOn(sObserveOn);
    }

    /**
     * Observes on main thread
     */
    public Observable<MediaBrowser.MediaItem> scan(MediaBrowser.MediaItem mediaItem) {
        Observable<MediaBrowser.MediaItem> observable;
        MediaMetaExtras metaExtras = MediaMetaExtras.from(mediaItem);
        if (metaExtras.isDirectory()) {
            observable = scanDirectory(mediaItem).subscribeOn(sSubscribeOn).observeOn(sObserveOn);
        } else if (metaExtras.isVideoFile()) {
            observable = lookupVideo(mediaItem).observeOn(sObserveOn);
        } else {
            observable = Observable.error(new Exception(String.format(Locale.US,
                    "scan(%s) Unknown mediaType", MediaItemUtil.getMediaTitle(mediaItem))));
        }
        return observable;
    }

    Observable<MediaBrowser.MediaItem> scanDirectory(MediaBrowser.MediaItem parentItem) {
        MediaMetaExtras parentExtras = MediaMetaExtras.from(parentItem);
        if (!parentItem.isBrowsable() && !parentExtras.isDirectory()) {
            return Observable.error(new Throwable(String.format(Locale.US,
                    "%s is not a directory.", MediaItemUtil.getMediaTitle(parentItem))));
        }
        Timber.d("scanDirectory(%s)", MediaItemUtil.getMediaTitle(parentItem));
        Uri parentUri = MediaItemUtil.getMediaUri(parentItem);
        return mDataService.getDirectoryChildrenList(parentItem)
                .flatMap(Observable::from)
                .flatMap(mediaItem -> {
                    final MediaMetaExtras extras = MediaMetaExtras.from(mediaItem);
                    if (extras.isVideoFile()) {
                        return lookupVideo(mediaItem)
                                .doOnError(e -> {
                                    Timber.w(e, "scanDirectory(%s)->[Ignored.OnError(%s)]",
                                            MediaItemUtil.getMediaTitle(parentItem),
                                            MediaItemUtil.getMediaTitle(mediaItem));
                                }).onExceptionResumeNext(Observable.empty());
                    } else if (extras.isDirectory()) {
                        //recursive call
                        return scanDirectory(mediaItem);
                    }
                    return Observable.<MediaBrowser.MediaItem>empty().doOnCompleted(() -> {
                        Timber.w("scanDirectory(%s)->[Ignored mediaItem %s]",
                                MediaItemUtil.getMediaTitle(parentItem),
                                MediaItemUtil.getMediaTitle(mediaItem));
                    });
                }).doOnCompleted(() -> {
                    mDbClient.markIndexed(parentUri, true);
                    mDataService.notifyChange(parentItem);
//                    Timber.d("scanDirectory(%s)->[OnComplete]", MediaItemUtil.getMediaTitle(parentItem));
                });
    }

    Observable<MediaBrowser.MediaItem> lookupVideo(MediaBrowser.MediaItem mediaItem) {
//        Timber.d("lookup(%s)", MediaItemUtil.getMediaTitle(mediaItem));
        return getLookupService(mediaItem).lookup(mediaItem)
                .doOnNext(item -> {
                    mDbClient.insertMedia(item);
                    Timber.d("lookupVideo(%s)->notifyChange(%s)",
                            MediaItemUtil.getMediaTitle(mediaItem),
                            MediaItemUtil.getMediaUri(mediaItem));
                    mDataService.notifyChange(item);
                })
//                .doOnCompleted(() -> {
//                    Timber.d("lookup(%s)->[OnComplete]", MediaItemUtil.getMediaTitle(mediaItem));
//                })
                //TODO handle synchronization in lookup services and remove this
                .subscribeOn(mHandlerScheduler)
                ;
    }

    LookupService getLookupService(MediaBrowser.MediaItem mediaItem) {
        MediaMetaExtras metaExtras = MediaMetaExtras.from(mediaItem);
        if (metaExtras.isTvEpisode() && VideoApp.hasTVDBKey()) {
            return mTVDbLookup;
        } else if (metaExtras.isMovie() && VideoApp.hasTMDBKey()) {
            return mTMDbLookup;
        } else {
            return mNilLookup;
        }
    }

    public static class Client extends Binder {
        WeakReference<ScannerService> service;

        public Client(ScannerService service) {
            this.service = new WeakReference<>(service);
        }

        public Observable<MediaBrowser.MediaItem> rescanAll() {
            ScannerService s = service.get();
            if (s == null) {
                return Observable.error(new NullPointerException("lost service reference"));
            }
            return s.rescanAll();
        }

        public Observable<MediaBrowser.MediaItem> scan(MediaBrowser.MediaItem mediaItem) {
            ScannerService s = service.get();
            if (s == null) {
                return Observable.error(new NullPointerException("lost service reference"));
            }
            return s.scan(mediaItem);
        }
    }

    public final static class Connection implements Closeable {
        private final Context context;
        private final ServiceConnection serviceConnection;
        private final ScannerService.Client service;
        public Connection(
                Context context,
                ServiceConnection serviceConnection,
                ScannerService.Client service
        ) {
            this.context = new ContextWrapper(context);
            this.serviceConnection = serviceConnection;
            this.service = service;
        }
        @Override public void close() {
            context.unbindService(serviceConnection);
        }
        public ScannerService.Client getClient() {
            return service;
        }
    }

    public static Connection bindService(Context context) throws InterruptedException {
        Utils.ensureNotOnMainThread(context);
        final BlockingQueue<ScannerService.Client> q = new LinkedBlockingQueue<>(1);
        ServiceConnection keyChainServiceConnection = new ServiceConnection() {
            volatile boolean mConnectedAtLeastOnce = false;
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (!mConnectedAtLeastOnce) {
                    mConnectedAtLeastOnce = true;
                    //Always on space available
                    q.offer((ScannerService.Client) service);
                }
            }
            @Override public void onServiceDisconnected(ComponentName name) {}
        };
        //noinspection ConstantConditions
        boolean isBound = context.bindService(new Intent(context, ScannerService.class),
                keyChainServiceConnection,
                Context.BIND_AUTO_CREATE);
        if (!isBound) {
            throw new AssertionError("could not bind to KeyChainService");
        }
        return new Connection(context, keyChainServiceConnection, q.take());
    }

}
