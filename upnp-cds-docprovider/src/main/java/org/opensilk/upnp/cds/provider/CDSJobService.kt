package org.opensilk.upnp.cds.browser

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService

/**
 * Created by drew on 5/19/17.
 */
class CDSJobService : JobService() {
    override fun onStopJob(params: JobParameters?): Boolean {
        TODO("not implemented")
        val j : JobScheduler;
        j.schedule(JobInfo.Builder().)
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        TODO("not implemented")
    }
}