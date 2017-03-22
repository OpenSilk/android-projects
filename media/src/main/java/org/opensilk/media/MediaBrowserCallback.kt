package org.opensilk.media

import android.media.browse.MediaBrowser
import android.service.media.MediaBrowserService

/**
 * Created by drew on 3/18/17.
 *
 * Wrapper for MediaBrowser.ConnectionCallback to avoid inner classes
 */
class MediaBrowserCallback(private val mListener: Listener) : MediaBrowser.ConnectionCallback() {

    /**
     * Wrapper interface for MediaBrowser.ConnectionCallback
     */
    interface Listener {
        fun onBrowserConnected()
        fun onBrowserDisconnected()
    }

    override fun onConnected() {
        mListener.onBrowserConnected()
    }

    override fun onConnectionSuspended() {
        mListener.onBrowserDisconnected()
    }

    override fun onConnectionFailed() {
        //For our purposes if the MediaBrowserService rejects us it is a fatal error
        throw RuntimeException("MediaBrowser failed to connect")
    }
}