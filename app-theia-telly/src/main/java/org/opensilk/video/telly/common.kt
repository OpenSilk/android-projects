package org.opensilk.video.telly

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import org.opensilk.common.app.ScopedActivity
import org.opensilk.video.UpnpHolderService
import timber.log.Timber

/**
 * Created by drew on 6/1/17.
 */
abstract class BaseVideoActivity: ScopedActivity() {
    //temporary solution, the activity should never use this
    //Allows application to inject the component after the scope has been created
    final override val activityComponent: Any = DaggerServiceReference()
    private var mUpnpServiceHolder : UpnpHolderService.HolderBinder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!bindService(Intent(this, UpnpHolderService::class.java), upnpServiceConnection, Context.BIND_AUTO_CREATE)) {
            Timber.e("Failed to bind to the UpnpHolderService")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(upnpServiceConnection)
        mUpnpServiceHolder = null
    }

    private val upnpServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mUpnpServiceHolder = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mUpnpServiceHolder = service as? UpnpHolderService.HolderBinder
        }
    }
}