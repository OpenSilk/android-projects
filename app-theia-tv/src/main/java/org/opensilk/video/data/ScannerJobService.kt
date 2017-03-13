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

package org.opensilk.video.data

import android.app.job.JobParameters
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.browse.MediaBrowser
import android.os.IBinder

import java.lang.ref.WeakReference
import java.util.WeakHashMap

import rx.Observer
import rx.Subscriber
import rx.Subscription
import rx.observers.Subscribers
import rx.subscriptions.CompositeSubscription
import rx.subscriptions.Subscriptions
import timber.log.Timber

/**
 * Created by drew on 4/10/16.
 */
class ScannerJobService : android.app.job.JobService() {

    internal val mJobs = WeakHashMap<Int, JobToken>()

    override fun onStartJob(params: JobParameters): Boolean {
        Timber.d("onStartJob(%s)", params.jobId)
        val jobid = params.jobId
        if (jobid == 102) {
            val token = JobToken(this, params)
            bindService(Intent(this, ScannerService::class.java), token, Context.BIND_AUTO_CREATE)
            mJobs.put(jobid, token)
            return true
        }
        return false
    }

    override fun onStopJob(params: JobParameters): Boolean {
        Timber.d("onStopJob(%s)", params.jobId)
        val jobid = params.jobId
        if (jobid == 102) {
            val token = mJobs[jobid]
            if (token != null) {
                token.unsubscribe()
                return true
            }
        }
        return false
    }

    internal class JobToken(mService: ScannerJobService, val mParams: JobParameters) : ServiceConnection, Observer<MediaBrowser.MediaItem> {
        val mService: WeakReference<ScannerJobService>
        val mSubscriptions = CompositeSubscription()
        var mConnection: ScannerService.Connection? = null

        init {
            this.mService = WeakReference(mService)
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mConnection = ScannerService.Connection(mService.get()!!.applicationContext, this, service as ScannerService.Client)
            val subscriber = Subscribers.from(this)
            subscriber.add(Subscriptions.create {
                if (mConnection != null) {
                    mConnection!!.close()
                }
            })
            val s = mConnection!!.client.rescanAll().subscribe(subscriber)
            mSubscriptions.add(s)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            val s = mService.get()
            s?.jobFinished(mParams, true)
        }

        override fun onCompleted() {
            Timber.d("onCompleted(%d)", mParams.jobId)
            val s = mService.get()
            s?.jobFinished(mParams, false)
        }

        override fun onError(e: Throwable) {
            Timber.w(e, "JobId=%d", mParams.jobId)
            onCompleted()
        }

        override fun onNext(mediaItem: MediaBrowser.MediaItem) {
            //pass
        }

        fun unsubscribe() {
            mSubscriptions.unsubscribe()
        }
    }

}
