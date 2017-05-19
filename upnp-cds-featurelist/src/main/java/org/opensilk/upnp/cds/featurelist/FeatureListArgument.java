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

import org.fourthline.cling.model.meta.ActionArgument;
import org.fourthline.cling.model.types.Datatype;
import org.fourthline.cling.model.types.StringDatatype;

/**
 * Created by drew on 12/20/16.
 */ //XXX Minidlna does not advertise the FeatureList state variable
class FeatureListArgument extends ActionArgument {
    public FeatureListArgument() {
        super("FeatureList", "FeatureList", Direction.OUT);
    }

    @Override
    public Datatype getDatatype() {
        return new StringDatatype();
    }
}
