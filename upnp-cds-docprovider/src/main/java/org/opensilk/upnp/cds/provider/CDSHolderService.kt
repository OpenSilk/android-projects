package org.opensilk.upnp.cds.browser

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * Created by drew on 5/19/17.
 */
class CDSHolderService : Service() {

    val mHolder: Holder = Holder()
    var mUpnpService: CDSUpnpService? = null

    override fun onBind(intent: Intent?): IBinder {
        return mHolder
    }

    override fun onDestroy() {
        super.onDestroy()
        mUpnpService = null
    }

    inner class Holder: Binder() {
        fun setCDSUpnpService(service: CDSUpnpService?) {
            mUpnpService = service
        }
    }
}