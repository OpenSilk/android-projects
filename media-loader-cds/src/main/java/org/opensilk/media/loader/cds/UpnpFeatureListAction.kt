package org.opensilk.media.loader.cds

import org.fourthline.cling.controlpoint.ControlPoint
import org.fourthline.cling.model.action.ActionException
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Service
import org.opensilk.upnp.cds.featurelist.Features
import org.opensilk.upnp.cds.featurelist.XGetFeatureListCallback
import java.util.concurrent.atomic.AtomicReference

/**
 * Represents synchronous action
 * Created by drew on 8/1/17.
 */
class UpnpFeatureListAction(
        controlPoint: ControlPoint,
        service: Service<*,*>
): XGetFeatureListCallback(service) {

    init {
        setControlPoint(controlPoint)
    }

    val features = AtomicReference<Features>()
    val error = AtomicReference<ActionException>()

    override fun received(actionInvocation: ActionInvocation<out Service<*, *>>, features: Features?) {
        this.features.set(features)
    }

    override fun failure(invocation: ActionInvocation<out Service<*, *>>, operation: UpnpResponse?, defaultMsg: String?) {
        this.error.set(invocation.failure)
    }

}