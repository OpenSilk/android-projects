package org.opensilk.upnp.cds.browser

import android.util.Log
import org.eclipse.jetty.util.log.AbstractLogger
import org.eclipse.jetty.util.log.Logger

/**
 * Created by drew on 7/29/17.
 */
class JettyAndroidLogger @JvmOverloads constructor(private val mName: String = ""): AbstractLogger() {
    override fun warn(msg: String?, vararg args: Any?) {
        Log.w(mName, msg?.format(*args))
    }

    override fun warn(thrown: Throwable?) {
        Log.w(mName, "", thrown)
    }

    override fun warn(msg: String?, thrown: Throwable?) {
        Log.w(mName, msg, thrown)
    }

    override fun getName(): String {
        return if (mName.isBlank()) javaClass.simpleName else mName
    }

    override fun info(msg: String?, vararg args: Any?) {
        Log.i(mName, msg?.format(*args))
    }

    override fun info(thrown: Throwable?) {
        Log.i(mName, "", thrown)
    }

    override fun info(msg: String?, thrown: Throwable?) {
        Log.i(mName, msg, thrown)
    }

    override fun newLogger(fullname: String?): Logger {
        return JettyAndroidLogger(fullname ?: "")
    }

    override fun setDebugEnabled(enabled: Boolean) {
        //noop
    }

    override fun ignore(ignored: Throwable?) {
        //noop
    }

    override fun isDebugEnabled(): Boolean {
        return false
    }

    override fun debug(msg: String?, vararg args: Any?) {
    }

    override fun debug(thrown: Throwable?) {
    }

    override fun debug(msg: String?, thrown: Throwable?) {
    }
}