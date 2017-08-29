package org.opensilk.video

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import org.opensilk.dagger2.ForApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by drew on 8/28/17.
 */
class UsbDevicesObserver @Inject constructor(
        @ForApp private val mContext: Context
): BroadcastReceiver(), LifecycleObserver {

    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d("onReceive($intent)")
        if (intent == null) {
            return
        }

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun registerReceiver() {
        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        mContext.registerReceiver(this, filter)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun unregisterReceiver() {
        mContext.unregisterReceiver(this)
    }
}