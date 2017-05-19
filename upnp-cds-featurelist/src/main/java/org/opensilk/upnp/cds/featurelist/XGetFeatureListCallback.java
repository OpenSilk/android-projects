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

package org.opensilk.upnp.cds.featurelist;

import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.action.ActionArgumentValue;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.meta.Action;
import org.fourthline.cling.model.meta.Service;

import java.lang.reflect.Method;

/**
 * Created by drew on 6/17/14.
 */
public abstract class XGetFeatureListCallback extends ActionCallback {

    public XGetFeatureListCallback(Service service) {
        super(buildActionInvocation(service));
    }

    @Override
    public void success(ActionInvocation actionInvocation) {
        ActionArgumentValue aav = actionInvocation.getOutput(new FeatureListArgument());
        if (aav == null) {
            failure(actionInvocation, null);
        } else {
            try {
                Features f = new FeaturesParser().parse(aav.toString());
                received(actionInvocation, f);
            } catch (Exception e) {
                failure(actionInvocation, null);
            }
        }
    }

    /*
     * X_GetFeatureList isnt an advertised action so we have to hack cling a bit to get it working
     */
    static ActionInvocation buildActionInvocation(Service service) {
        try {
            ActionInvocation actionInvocation = new ActionInvocation(new XGetFeatureListAction());
            // package method must be reflected
            Method m = Action.class.getDeclaredMethod("setService", Service.class);
            m.setAccessible(true);
            m.invoke(actionInvocation.getAction(), service);
            return actionInvocation;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void received(ActionInvocation actionInvocation, Features features);

}
