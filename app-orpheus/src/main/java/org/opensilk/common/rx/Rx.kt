package org.opensilk.common.rx

import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers

fun notSubscribed(subscription: Subscription?): Boolean {
    return subscription == null || subscription.isUnsubscribed
}

fun isSubscribed(subscription: Subscription?): Boolean {
    return !notSubscribed(subscription)
}

fun unsubscribe(subscription: Subscription?) {
    subscription?.unsubscribe()
}

fun <T> Observable<T>.observeOnMainThread(): Observable<T> {
    return this.observeOn(AndroidSchedulers.mainThread())
}
