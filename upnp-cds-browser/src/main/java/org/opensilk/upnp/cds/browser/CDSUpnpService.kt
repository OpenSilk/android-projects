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
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.os.Message
import org.apache.commons.lang3.StringUtils
import org.eclipse.jetty.util.log.Log
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
import org.fourthline.cling.transport.impl.AsyncServletStreamServerConfigurationImpl
import org.fourthline.cling.transport.impl.AsyncServletStreamServerImpl
import org.fourthline.cling.transport.impl.RecoveringSOAPActionProcessorImpl
import org.fourthline.cling.transport.impl.jetty.JettyServletContainer
import org.fourthline.cling.transport.spi.NetworkAddressFactory
import org.fourthline.cling.transport.spi.SOAPActionProcessor
import org.fourthline.cling.transport.spi.StreamServer
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
class CDSUpnpService
@Inject constructor(
        @ForApplication private val mContext: Context
) : UpnpService {

    private val mUpnpService = Service()

    override fun getRouter(): AndroidRouter {
        return mUpnpService.router
    }

    override fun getProtocolFactory(): ProtocolFactory {
        return mUpnpService.protocolFactory
    }

    override fun getConfiguration(): UpnpServiceConfiguration {
        return mUpnpService.configuration
    }

    override fun getRegistry(): Registry {
        return mUpnpService.registry
    }

    override fun getControlPoint(): ControlPoint {
        return mUpnpService.controlPoint
    }

    /**
     * Does nothing, we never shutdown, user is responsible for cleaning up themselves
     */
    override fun shutdown() {
        //
    }

    /**
     * Executes the shutdown task
     */
    class ShutdownDelayHandler: Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == 1) {
                ShutdownTask(msg.obj as Service).execute()
            }
        }
    }

    /**
     * Executes the shutdown on a background thread
     */
    class ShutdownTask(val mService: Service): AsyncTask<Void, Int, Int>() {
        override fun onPreExecute() {
            val router = mService.router
            if (router is AndroidRouter) {
                router.unregisterBroadcastReceiver()
            }
        }

        override fun doInBackground(vararg params: Void?): Int {
            mService.shutdown()
            return 0
        }
    }

    /**
     * Our custom upnp service class
     */
    inner class Service: UpnpServiceImpl(ServiceConfiguration()) {
        override fun createRouter(protocolFactory: ProtocolFactory, registry: Registry?): Router {
            return AndroidRouter(getConfiguration(), protocolFactory, mContext)
        }

        override fun getRouter(): AndroidRouter {
            return super.getRouter() as AndroidRouter
        }
    }

    /**
     * Our custom upnp service configuration
     */
    class ServiceConfiguration : AndroidUpnpServiceConfiguration() {
        init {
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
            Logger.getLogger("org.fourthline.cling.transport.spi.SOAPActionProcessor").level = Level.FINER

            //fix jetty logging
            Log.__logClass = JettyAndroidLogger::class.java.name
            Log.__logClass = JettyAndroidLogger::class.java.name
        }

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

        override fun createStreamServer(networkAddressFactory: NetworkAddressFactory): StreamServer<*> {
            // Use Jetty, start/stop a new shared instance of JettyServletContainer
            return AsyncServletStreamServerImpl(
                    AsyncServletStreamServerConfigurationImpl(
                            JettyServletContainer.INSTANCE,
                            networkAddressFactory.streamListenPort
                    )
            )
        }

        override fun getRegistryMaintenanceIntervalMillis(): Int {
            return 2500//10000;
        }
    }

}