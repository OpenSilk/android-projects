package org.opensilk.upnp.cds.browser

import android.os.Build
import org.fourthline.cling.model.ServerClientTokens
import org.fourthline.cling.transport.spi.AbstractStreamClientConfiguration
import java.util.concurrent.ExecutorService

/**
 * Created by drew on 8/5/17.
 */
class OkStreamClientConfig(
        executorService: ExecutorService
) : AbstractStreamClientConfiguration(executorService) {

    override fun getUserAgentValue(majorVersion: Int, minorVersion: Int): String {
        // TODO: UPNP VIOLATION: Synology NAS requires User-Agent to contain
        // "Android" to return DLNA protocolInfo required to stream to Samsung TV
        // see: http://two-play.com/forums/viewtopic.php?f=6&t=81
        val tokens = ServerClientTokens(majorVersion, minorVersion)
        tokens.osName = "Android"
        tokens.osVersion = Build.VERSION.RELEASE
        return tokens.toString()
    }

}