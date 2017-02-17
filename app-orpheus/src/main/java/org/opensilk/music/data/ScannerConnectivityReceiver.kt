package org.opensilk.music.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Created by drew on 6/28/16.
 */
class ScannerConnectivityReceiver(): BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context!!
        intent!!
        if ("android.net.conn.CONNECTIVITY_CHANGE" != intent.action) {
            return
        }
    }
}