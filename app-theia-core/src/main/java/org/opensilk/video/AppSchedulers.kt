package org.opensilk.video

import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * Created by drew on 7/21/17.
 */
object AppSchedulers {
    val diskIo: Scheduler = Schedulers.io()
    val networkIo: Scheduler = Schedulers.io()
    val main: Scheduler = AndroidSchedulers.mainThread()
}