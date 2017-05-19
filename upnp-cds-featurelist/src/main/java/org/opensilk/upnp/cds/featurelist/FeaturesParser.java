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

import org.seamless.xml.ParserException;
import org.seamless.xml.SAXParser;
import org.xml.sax.InputSource;

import java.io.StringReader;

/**
 * Parse a FeatureList response
 *
 * example input (and only one i tested):
 * <pre>
 *      <Features xmlns="urn:schemas-upnp-org:av:avs" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *              xsi:schemaLocation="urn:schemas-upnp-org:av:avs http://www.upnp.org/schemas/av/avs.xsd">
 *          <Feature name="samsung.com_BASICVIEW" version="1">
 *              <container id="1" type="object.item.audioItem"/>
 *              <container id="2" type="object.item.videoItem"/>
 *              <container id="3" type="object.item.imageItem"/>
 *          </Feature>
 *      </Features>
 * </pre>
 *
 * Created by drew on 6/18/14.
 */
public class FeaturesParser extends SAXParser {

    private final Features features;

    public FeaturesParser() {
        this.features = new Features();
        setContentHandler(new FeaturesHandler(features));
    }

    public Features parse(String xml) throws ParserException {
        if (xml != null && xml.length() > 0) {
            parse(new InputSource(new StringReader(xml)));
        }
        return features;
    }

}
