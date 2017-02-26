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

package org.opensilk.common.loader

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.lang.kotlin.observable
import rx.schedulers.Schedulers
import rx.subscriptions.Subscriptions
import timber.log.Timber
import java.util.*

/**
 * Created by drew on 4/10/16.
 */
abstract class RxCursorListLoader<T>
@JvmOverloads
constructor(
        protected val context: Context,
        var uri: Uri,
        var projection: Array<String>? = null,
        var selection: String? = null,
        var selectionArgs: Array<String>? = null,
        var sortOrder: String? = null
) : RxListLoader<T> {

    var subscribeOnScheduler = Schedulers.io()
    var observeOnScheduler = AndroidSchedulers.mainThread()
    var notifyForDescendants: Boolean = false

    /**
     * Do your thing.
     * Return null to skip item
     */
    protected abstract fun makeFromCursor(c: Cursor): T?

    /**
     * @return Observable that emits lists, this observable never terminates
     * *          it registers a change listener on the uri and emmits a new list
     * *          on notify.
     */
    override val listObservable: Observable<List<T>>
        get() {
            return observable { subscriber ->
                val pushList = {
                    context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use {
                        if (it.moveToFirst()) {
                            val list = ArrayList<T>(it.count)
                            do {
                                val item = makeFromCursor(it) ?: continue
                                list.add(item)
                            } while (it.moveToNext())
                            subscriber.onNext(list)
                        } else {
                            subscriber.onNext(emptyList())
                        }
                    } ?: subscriber.onError(NullPointerException("Unable to obtain cursor"))
                }
                val co = object : ContentObserver(null) {
                    override fun onChange(selfChange: Boolean) {
                        val worker = subscribeOnScheduler.createWorker()
                        worker.schedule {
                            pushList()
                            worker.unsubscribe()
                        }
                    }
                }
                subscriber.add(Subscriptions.create { context.contentResolver.unregisterContentObserver(co) })
                context.contentResolver.registerContentObserver(uri, notifyForDescendants, co)
                pushList()
            }
        }

    protected fun dump(throwable: Throwable) {
        Timber.e(throwable, "%s(uri=%s\nprojection=%s\nselection=%s\nselectionArgs=%s\nsortOrder=%s)",
                RxCursorListLoader::class.java.simpleName,
                uri,
                if (projection != null) Arrays.toString(projection) else null,
                selection,
                if (selectionArgs != null) Arrays.toString(selectionArgs) else null,
                sortOrder)
    }
}

