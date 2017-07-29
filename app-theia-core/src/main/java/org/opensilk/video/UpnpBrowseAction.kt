package org.opensilk.video

import org.fourthline.cling.controlpoint.ControlPoint
import org.fourthline.cling.model.action.ActionException
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.support.contentdirectory.callback.Browse
import org.fourthline.cling.support.model.BrowseFlag
import org.fourthline.cling.support.model.BrowseResult
import org.fourthline.cling.support.model.DIDLContent
import org.fourthline.cling.support.model.SortCriterion
import java.util.concurrent.atomic.AtomicReference

/**
 * Created by drew on 7/29/17.
 */
class UpnpBrowseAction(
        controlPoint: ControlPoint,
        service: Service<*,*>,
        containerId: String,
        browseFlag: BrowseFlag = BrowseFlag.DIRECT_CHILDREN,
        filter: String = Browse.CAPS_WILDCARD,
        firstResult: Long = 0,
        maxResults: Long = 999,
        orderby: Array<SortCriterion> = emptyArray()
) : Browse(service, containerId, browseFlag, filter, firstResult, maxResults, *orderby) {

    init {
        setControlPoint(controlPoint)
    }

    val result = AtomicReference<BrowseResult>()
    val error = AtomicReference<ActionException>()

    override fun receivedRaw(actionInvocation: ActionInvocation<out Service<*, *>>, browseResult: BrowseResult): Boolean {
        result.set(browseResult)
        return false
    }

    override fun updateStatus(status: Status?) {
        //unused
    }

    override fun received(actionInvocation: ActionInvocation<out Service<*, *>>?, didl: DIDLContent?) {
        //unused
    }

    override fun failure(invocation: ActionInvocation<out Service<*, *>>, operation: UpnpResponse?, defaultMsg: String?) {
        error.set(invocation.failure)
    }

}