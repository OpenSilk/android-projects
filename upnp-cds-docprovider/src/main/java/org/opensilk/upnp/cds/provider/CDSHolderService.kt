package org.opensilk.upnp.cds.browser

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import javax.inject.Inject

/**
 * Created by drew on 5/19/17.
 */
class CDSHolderService : Service() {

    @Inject lateinit var mUpnpService: CDSUpnpService

    override fun onCreate() {
        super.onCreate()

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


}