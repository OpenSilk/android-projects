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

package org.opensilk.video.data;

import android.app.job.JobParameters;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.browse.MediaBrowser;
import android.os.IBinder;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.observers.Subscribers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

/**
 * Created by drew on 4/10/16.
 */
public class ScannerJobService extends android.app.job.JobService {

    final WeakHashMap<Integer, JobToken> mJobs = new WeakHashMap<>();

    @Override
    public boolean onStartJob(JobParameters params) {
        Timber.d("onStartJob(%s)" , params.getJobId());
        int jobid = params.getJobId();
        if (jobid == 102) {
            JobToken token = new JobToken(this, params);
            bindService(new Intent(this, ScannerService.class), token, BIND_AUTO_CREATE);
            mJobs.put(jobid, token);
            return true;
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Timber.d("onStopJob(%s)", params.getJobId());
        int jobid = params.getJobId();
        if (jobid == 102) {
            JobToken token = mJobs.get(jobid);
            if (token != null){
                token.unsubscribe();
                return true;
            }
        }
        return false;
    }

    static class JobToken implements ServiceConnection, Observer<MediaBrowser.MediaItem> {
        final WeakReference<ScannerJobService> mService;
        final JobParameters mParams;
        final CompositeSubscription mSubscriptions = new CompositeSubscription();
        ScannerService.Connection mConnection;

        public JobToken(ScannerJobService mService, JobParameters mParams) {
            this.mService = new WeakReference<ScannerJobService>(mService);
            this.mParams = mParams;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mConnection = new ScannerService.Connection(mService.get(),
                    this, (ScannerService.Client) service);
            Subscriber<MediaBrowser.MediaItem> subscriber = Subscribers.from(this);
            subscriber.add(Subscriptions.create(() -> {
                if (mConnection != null) {
                    mConnection.close();
                }
            }));
            Subscription s = mConnection.getClient().rescanAll().subscribe(subscriber);
            mSubscriptions.add(s);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            ScannerJobService s = mService.get();
            if (s != null) {
                s.jobFinished(mParams, true);
            }
        }

        @Override
        public void onCompleted() {
            Timber.d("onCompleted(%d)", mParams.getJobId());
            ScannerJobService s = mService.get();
            if (s != null) {
                s.jobFinished(mParams, false);
            }
        }

        @Override
        public void onError(Throwable e) {
            Timber.w(e, "JobId=%d", mParams.getJobId());
            onCompleted();
        }

        @Override
        public void onNext(MediaBrowser.MediaItem mediaItem) {
            //pas
        }

        void unsubscribe() {
            mSubscriptions.unsubscribe();
        }
    }

}
