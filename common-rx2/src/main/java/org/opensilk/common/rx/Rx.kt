package org.opensilk.common.rx

import android.os.CancellationSignal
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer

private val NIL_THROWABLE_CONSUMER = Consumer<Throwable> {  }

/**
 * Null safe dispose
 * @return true if dispose() was called
 */
fun Disposable?.safeDispose(): Boolean {
    return this?.let {
        return@let if (!isDisposed) {
            dispose()
            true
        } else false
    } ?: false
}

fun <T> Observable<T>.observeOnMainThread(): Observable<T> {
    return this.observeOn(AndroidSchedulers.mainThread())
}

fun <T> Observable<T>.subscribeIgnoreError(action: Consumer<T>): Disposable {
    return this.subscribe(action, NIL_THROWABLE_CONSUMER)
}

fun <T> Single<T>.subscribeIgnoreError(action: Consumer<T>): Disposable {
    return this.subscribe(action, NIL_THROWABLE_CONSUMER)
}

fun <T> Maybe<T>.subscribeIgnoreError(action: Consumer<T>): Disposable {
    return this.subscribe(action, NIL_THROWABLE_CONSUMER)
}

fun <T> Single<T>.observeOnMainThread(): Single<T> {
    return this.observeOn(AndroidSchedulers.mainThread())
}

fun <T> ObservableEmitter<T>.cancellationSignal(): CancellationSignal {
    val c = CancellationSignal()
    this.setCancellable({ c.cancel() })
    return c
}
