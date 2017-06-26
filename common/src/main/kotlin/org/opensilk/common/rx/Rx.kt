package org.opensilk.common.rx

import rx.*
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action0
import rx.functions.Action1

fun Subscription?.isSubscribed(): Boolean {
    return this != null && !this.isUnsubscribed
}

/**
 * Null safe unsubscribe.
 * @return true if unsubscribe() was called
 */
fun Subscription?.safeUnsubscribe(): Boolean {
    return this?.let {
        return@let if (!isUnsubscribed) {
            unsubscribe()
            true
        } else {
            false
        }
    } ?: false
}

fun unsubscribe(subscription: Subscription?) {
    subscription?.unsubscribe()
}

fun <T> Observable<T>.observeOnMainThread(): Observable<T> {
    return this.observeOn(AndroidSchedulers.mainThread())
}

fun <T> Observable<T>.subscribeIgnoreError(action: (T) -> Unit): Subscription {
    return this.subscribe(object : SimpleSubscriber<T>() {
        override fun onNext(t: T) {
            action(t)
        }
    })
}

fun <T> Single<T>.subscribeIgnoreError(action: (T) -> Unit): Subscription {
    return this.subscribe(object : SimpleSingleSubscriber<T>() {
        override fun onSuccess(value: T) {
            action(value)
        }
    })
}

open class SimpleSubscriber<T>: Subscriber<T>() {
    override fun onError(e: Throwable?) {
    }

    override fun onCompleted() {
    }

    override fun onNext(t: T) {
    }
}

open class SimpleSingleSubscriber<T>: SingleSubscriber<T>() {
    override fun onSuccess(value: T) {
    }

    override fun onError(error: Throwable?) {
    }
}