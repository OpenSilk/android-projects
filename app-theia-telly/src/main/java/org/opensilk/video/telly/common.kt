package org.opensilk.video.telly

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import org.opensilk.common.app.ScopedActivity
import timber.log.Timber

/**
 * Created by drew on 6/1/17.
 */
abstract class BaseVideoActivity: ScopedActivity() {
    //temporary solution, the activity should never use this
    //Allows application to inject the component after the scope has been created
    override val activityComponent: Any = DaggerServiceReference()
    private var upnpServiceHolder : UpnpHolderService.HolderBinder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!bindService(Intent(this, UpnpHolderService::class.java), upnpServiceConnection, Context.BIND_AUTO_CREATE)) {
            Timber.e("Failed to bind to the UpnpHolderService")
        }
    }

    private val upnpServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            upnpServiceHolder = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            upnpServiceHolder = service as UpnpHolderService.HolderBinder
        }
    }
}