package org.opensilk.music

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.support.v4.content.WakefulBroadcastReceiver

/**
 *
 */
class PlaybackMediaButtonReceiver: WakefulBroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (Intent.ACTION_MEDIA_BUTTON == intent?.action) {
            intent.component = ComponentName(context, PlaybackService::class.java)
            intent.putExtra("orpheus.FROM_MEDIA_BUTTON", true)
            WakefulBroadcastReceiver.startWakefulService(context, intent)
        }
    }
}