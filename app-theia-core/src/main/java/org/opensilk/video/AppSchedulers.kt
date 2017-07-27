package org.opensilk.video

import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers
import rx.android.schedulers.HandlerScheduler
import rx.internal.schedulers.ExecutorScheduler
import rx.schedulers.Schedulers
import java.util.concurrent.Executors

/**
 * Created by drew on 7/21/17.
 */
object AppSchedulers {
    val diskIo: Scheduler = Schedulers.io()
    val networkIo: Scheduler = Schedulers.io()
    val main: Scheduler = AndroidSchedulers.mainThread()
    val callback: Scheduler by lazy {
        ExecutorScheduler(Executors.newSingleThreadExecutor())
    }
    val background: Scheduler by lazy {
        ExecutorScheduler(Executors.newSingleThreadExecutor())
    }
}