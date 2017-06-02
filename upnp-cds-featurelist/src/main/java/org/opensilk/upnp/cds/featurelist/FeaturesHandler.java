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

import android.util.Log;

import org.fourthline.cling.model.types.Datatype;
import org.seamless.xml.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Created by drew on 12/20/16.
 */
class FeaturesHandler extends SAXParser.Handler<Features> {

    FeaturesHandler(Features instance, SAXParser parser) {
        super(instance, parser);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (!Features.NAMESPACE.equals(uri)) return;

        if (localName.equals("Feature")) {
            String name = attributes.getValue("name");
            String versionString = attributes.getValue("version");
            if (versionString == null) {
                versionString = "1";
            }
            Integer version = (Integer) Datatype.Builtin.INT.getDatatype().valueOf(versionString);

            if (BasicView.NAME.equals(name)) {
                BasicView basicView = new BasicView(name, version);
                getParser().setContentHandler(new BasicViewHandler(basicView, this));
                getInstance().addFeature(basicView);
            } else {
                Log.w("FeaturesParser", "Unknown feature: " + name);
            }
        }

    }

    @Override
    protected boolean isLastElement(String uri, String localName, String qName) {
        return Features.NAMESPACE.equals(uri) && "Features".equals(localName);
    }
}
