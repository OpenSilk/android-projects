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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by drew on 6/18/14.
 */
public class Features {

    public static final String NAMESPACE = "urn:schemas-upnp-org:av:avs";

    private final List<Feature> features;

    public Features() {
        features = new ArrayList<>();
    }

    void addFeature(Feature feature) {
        features.add(feature);
    }

    public List<Feature> getFeatures() {
        return Collections.unmodifiableList(features);
    }

}
