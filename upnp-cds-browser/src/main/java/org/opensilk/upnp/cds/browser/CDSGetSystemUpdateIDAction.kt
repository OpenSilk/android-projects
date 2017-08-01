package org.opensilk.upnp.cds.browser

import org.fourthline.cling.controlpoint.ActionCallback
import org.fourthline.cling.controlpoint.ControlPoint
import org.fourthline.cling.model.action.ActionException
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes
import java.util.concurrent.atomic.AtomicReference

/**
 * Created by drew on 7/31/17.
 */
class CDSGetSystemUpdateIDAction(
        controlPoint: ControlPoint,
        service: Service<*,*>
) : ActionCallback(ActionInvocation(service.getAction("GetSystemUpdateID")), controlPoint) {

    class Result(val id: UnsignedIntegerFourBytes)

    val result = AtomicReference<Result>()
    val error = AtomicReference<ActionException>()

    override fun failure(invocation: ActionInvocation<out Service<*, *>>, operation: UpnpResponse?, defaultMsg: String?) {
        error.set(invocation.failure)
    }

    override fun success(invocation: ActionInvocation<out Service<*, *>>) {
        result.set(Result(invocation.getOutput("Id").value as UnsignedIntegerFourBytes))
    }
}