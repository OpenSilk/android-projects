package org.opensilk.traveltime.service;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;

/**
 * Runs the calendar sync job.
 *
 * Created by drew on 10/29/17.
 */
public class CalendarSyncJobService extends JobService {

    /**
     * Request the sync job be scheduled for execution at some point in the future
     *
     * @param context
     * @return true if job successfully scheduled
     */
    public static boolean scheduleSelf(Context context) {
        Log.i("SyncJob", "Scheduling calendar sync job");
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo ji = new JobInfo.Builder(1202, new ComponentName(context, CalendarSyncJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build();
        return js.schedule(ji) == JobScheduler.RESULT_SUCCESS;
    }

    @Inject CalendarSync calendarSync;

    Disposable calendarSyncDisposable = Disposables.disposed();

    @Override
    public void onCreate() {
        AndroidInjection.inject(this);
        super.onCreate();
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        if (params.getJobId() != 1202) {
            return false;
        }
        calendarSyncDisposable = Completable.fromRunnable(
                new Runnable() {
                    @Override
                    public void run() {
                        Log.i("SyncJob", "Running calendar sync");
                        calendarSync.syncCalendar();
                    }
                }
        ).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                new Action() {
                    @Override
                    public void run() throws Exception {
                        jobFinished(params, false);
                    }
                }
        );
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        boolean finished = calendarSyncDisposable.isDisposed();
        calendarSyncDisposable.dispose();
        return finished;
    }

}
