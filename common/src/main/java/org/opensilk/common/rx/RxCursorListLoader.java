/*
 * Copyright (c) 2016 OpenSilk Productions LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.common.rx;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import org.opensilk.common.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

/**
 * Created by drew on 4/10/16.
 */
public abstract class RxCursorListLoader<T> implements RxListLoader<T> {

    protected final Context context;

    protected Scheduler subscribeOnScheduler = Schedulers.io();
    protected Scheduler observeOnScheduler = AndroidSchedulers.mainThread();

    protected Uri uri;
    protected String[] projection;
    protected String selection;
    protected String[] selectionArgs;
    protected String sortOrder;
    protected boolean notifyForDescendants;

    public RxCursorListLoader(Context context) {
        this(context, null, null, null, null, null);
    }

    public RxCursorListLoader(Context context,
                              Uri uri, String[] projection, String selection,
                              String[] selectionArgs, String sortOrder) {
        this.context = context;
        this.uri = uri;
        this.projection = projection;
        this.selection = selection;
        this.selectionArgs = selectionArgs;
        this.sortOrder = sortOrder;
    }

    /**
     * Do your thing.
     * Any thrown exceptions will be passed to onError().
     * Return null to skip item
     */
    protected abstract @Nullable T makeFromCursor(Cursor c) throws Exception;

    /**
     * @return Observable that emits lists, this observable never terminates
     *          it registers a change listener on the uri and emmits a new list
     *          on notify.
     */
    public Observable<List<T>> getListObservable() {
        return Observable.create(new Observable.OnSubscribe<List<T>>() {
            @Override
            public void call(final Subscriber<? super List<T>> subscriber) {
                if (!pushListAction.call(subscriber)) {
                    return;
                }
                final ContentObserver co = new ContentObserver(null) {
                    @Override
                    public void onChange(boolean selfChange) {
                        final Scheduler.Worker worker = subscribeOnScheduler.createWorker();
                        worker.schedule(new Action0() {
                            @Override
                            public void call() {
                                pushListAction.call(subscriber);
                                worker.unsubscribe();
                            }
                        });
                    }
                };
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        context.getContentResolver().unregisterContentObserver(co);
                    }
                }));
                context.getContentResolver().registerContentObserver(uri, notifyForDescendants, co);
            }
        });
    }

    private final Func1<Subscriber<? super List<T>>, Boolean> pushListAction =
            new Func1<Subscriber<? super List<T>>, Boolean>() {
                @Override
                public Boolean call(Subscriber<? super List<T>> subscriber) {
                    try {
                        List<T> list = getList();
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(list);
                            return true;
                        }
                    } catch (Exception e) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onError(e);
                        }
                    }
                    return false;
                }
    };

    private List<T> getList() throws Exception {
        Cursor c = null;
        try {
            if (context == null || uri == null) {
                throw new NullPointerException("Context and Uri must not be null");
            }
            c = getCursor();
            if (c == null) {
                throw new NullPointerException("Unable to obtain cursor");
            }
            if (c.moveToFirst()) {
                List<T> list = new ArrayList<>(c.getCount());
                do {
                    T item = makeFromCursor(c);
                    if (item == null) {
                        continue;
                    }
                    list.add(item);
                } while (c.moveToNext());
                return list;
            }
            return Collections.emptyList();
        } finally {
            if (c != null) c.close();
        }
    }

    protected Cursor getCursor() {
        return context.getContentResolver().query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );
    }

    public RxCursorListLoader<T> setUri(Uri uri) {
        this.uri = Preconditions.checkNotNull(uri, "Uri must not be null");
        return this;
    }

    public RxCursorListLoader<T> setProjection(String[] projection) {
        this.projection = projection;
        return this;
    }

    public RxCursorListLoader<T> setSelection(String selection) {
        this.selection = selection;
        return this;
    }

    public RxCursorListLoader<T> setSelectionArgs(String[] selectionArgs) {
        this.selectionArgs = selectionArgs;
        return this;
    }

    public RxCursorListLoader<T> setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    public RxCursorListLoader<T> setSubscribeScheduler(Scheduler scheduler) {
        this.subscribeOnScheduler = Preconditions.checkNotNull(scheduler, "Scheduler must not be null");
        return this;
    }

    public RxCursorListLoader<T> setObserveOnScheduler(Scheduler scheduler) {
        this.observeOnScheduler = Preconditions.checkNotNull(scheduler, "Scheduler must not be null");
        return this;
    }

    public RxCursorListLoader<T> setNotifyForDescendants(boolean notifyForDescendants) {
        this.notifyForDescendants = notifyForDescendants;
        return this;
    }

    protected void dump(Throwable throwable) {
        Timber.e(throwable, "%s(uri=%s\nprojection=%s\nselection=%s\nselectionArgs=%s\nsortOrder=%s)",
                RxCursorListLoader.class.getSimpleName(),
                uri,
                projection != null ? Arrays.toString(projection) : null,
                selection,
                selectionArgs != null ? Arrays.toString(selectionArgs) : null,
                sortOrder);
    }
}

