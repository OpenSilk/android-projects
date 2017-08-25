package org.opensilk.video

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.internal.schedulers.ExecutorScheduler
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Created by drew on 7/21/17.
 */
object AppSchedulers {
    val diskIo: Scheduler by lazy {
        ExecutorScheduler(Executors.newFixedThreadPool(2))
    }
    val networkIo: Scheduler = Schedulers.io()
    val main: Scheduler = AndroidSchedulers.mainThread()
    val background: Scheduler by lazy {
        ExecutorScheduler(Executors.newSingleThreadExecutor())
    }
    val newThread: Scheduler = networkIo
}