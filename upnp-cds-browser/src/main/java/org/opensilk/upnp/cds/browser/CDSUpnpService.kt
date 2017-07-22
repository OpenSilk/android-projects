/*
 * Copyright (c) 2016 OpenSilk Productions LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.upnp.cds.browser

import android.content.Context
import org.apache.commons.lang3.StringUtils
import org.fourthline.cling.UpnpService
import org.fourthline.cling.UpnpServiceConfiguration
import org.fourthline.cling.UpnpServiceImpl
import org.fourthline.cling.android.AndroidRouter
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration
import org.fourthline.cling.controlpoint.ControlPoint
import org.fourthline.cling.model.UnsupportedDataException
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.control.ActionResponseMessage
import org.fourthline.cling.model.types.ServiceType
import org.fourthline.cling.model.types.UDAServiceType
import org.fourthline.cling.protocol.ProtocolFactory
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.transport.Router
import org.fourthline.cling.transport.impl.RecoveringSOAPActionProcessorImpl
import org.fourthline.cling.transport.spi.SOAPActionProcessor
import org.opensilk.common.dagger.ForApplication
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

val CDSserviceType = UDAServiceType("ContentDirectory", 1)

/**
 * Created by drew on 12/21/16.
 */
@Singleton
class CDSUpnpService @Inject constructor(
        @ForApplication context: Context) : UpnpService {

    private val mContext: Context = context
    private var mUpnpService: UpnpService
    private var mShutdown = true

    init {
        mUpnpService = createUpnpService()
        mShutdown = false

        // Fix the logging integration between java.util.logging and Android internal logging
        org.seamless.util.logging.LoggingUtil.resetRootHandler(
                org.seamless.android.FixedAndroidLogHandler()
        )
        // enable logging as needed for various categories of Cling:
        Logger.getLogger("org.fourthline.cling").level = Level.INFO//.FINE);
        Logger.getLogger("org.fourthline.cling.transport.spi.DatagramProcessor").level = Level.INFO
        Logger.getLogger("org.fourthline.cling.transport.spi.DatagramIO").level = Level.INFO
        Logger.getLogger("org.fourthline.cling.protocol.ProtocolFactory").level = Level.INFO
        Logger.getLogger("org.fourthline.cling.model.message.UpnpHeaders").level = Level.INFO
//            Logger.getLogger("org.fourthline.cling.transport.spi.SOAPActionProcessor").setLevel(Level.FINER);
    }

    override fun getRouter(): Router {
        return ensureService().router
    }

    override fun getProtocolFactory(): ProtocolFactory {
        return ensureService().protocolFactory
    }

    override fun getConfiguration(): UpnpServiceConfiguration {
        return ensureService().configuration
    }

    override fun getRegistry(): Registry {
        return ensureService().registry
    }

    override fun getControlPoint(): ControlPoint {
        return ensureService().controlPoint
    }

    override fun shutdown() {
        if (!mShutdown) {
            synchronized(this) {
                if (!mShutdown) {
                    mUpnpService.shutdown()
                    mShutdown = true
                }
            }
        }
    }

    fun ensureService(): UpnpService {
        if (mShutdown) {
            synchronized(this) {
                if (mShutdown) {
                    mUpnpService = createUpnpService()
                    mShutdown = false
                }
            }
        }
        return mUpnpService
    }

    private fun createUpnpService(): UpnpService {
        return object : UpnpServiceImpl(createConfiguration()) {
            override fun createRouter(protocolFactory: ProtocolFactory, registry: Registry?): Router {
                return AndroidRouter(getConfiguration(), protocolFactory, mContext)
            }

            @Synchronized override fun shutdown() {
                // First have to remove the receiver, so Android won't complain about it leaking
                // when the main UI thread exits.
                (getRouter() as AndroidRouter).unregisterBroadcastReceiver()

                // Now we can concurrently run the Cling shutdown code, without occupying the
                // Android main UI thread. This will complete probably after the main UI thread
                // is done.
                super.shutdown(true)
            }
        }
    }

    private fun createConfiguration(): UpnpServiceConfiguration {
        return object : AndroidUpnpServiceConfiguration() {
            override fun getExclusiveServiceTypes(): Array<ServiceType> {
                return arrayOf(CDSserviceType)
            }

            override fun createSOAPActionProcessor(): SOAPActionProcessor {
                return object : RecoveringSOAPActionProcessorImpl() {
                    @Throws(UnsupportedDataException::class)
                    override fun readBody(responseMsg: ActionResponseMessage, actionInvocation: ActionInvocation<*>) {
                        try {
                            super.readBody(responseMsg, actionInvocation)
                        } catch (e: Exception) {
                            //Hack for X_GetFeatureList embedding this in the body
                            val fixedBody = StringUtils.remove(getMessageBody(responseMsg),
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                            responseMsg.setBody(fixedBody)
                            super.readBody(responseMsg, actionInvocation)
                        }
                    }
                }
            }

            override fun getRegistryMaintenanceIntervalMillis(): Int {
                return 2500//10000;
            }
        }
    }

}