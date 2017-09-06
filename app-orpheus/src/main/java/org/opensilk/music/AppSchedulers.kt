package org.opensilk.music

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.internal.schedulers.ExecutorScheduler
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Executors

/**
 * Created by drew on 9/5/17.
 */
object AppSchedulers {
    val diskIo: Scheduler by lazy {
        ExecutorScheduler(Executors.newFixedThreadPool(2))
    }
    val networkIo: Scheduler = Schedulers.io()
    val main: Scheduler = AndroidSchedulers.mainThread()
    val background: Scheduler = Schedulers.single()
    val newThread: Scheduler = networkIo
    val prefetch: Scheduler by lazy {
        ExecutorScheduler(Executors.newFixedThreadPool(2))
    }
}