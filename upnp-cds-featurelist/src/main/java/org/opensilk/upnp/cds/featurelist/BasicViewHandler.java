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

import org.fourthline.cling.support.model.DescMeta;
import org.fourthline.cling.support.model.container.Container;
import org.seamless.xml.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Created by drew on 12/19/16.
 */
class BasicViewHandler extends SAXParser.Handler<BasicView> {

    BasicViewHandler(BasicView instance, SAXParser.Handler parent) {
        super(instance, parent);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (!Features.NAMESPACE.equals(uri)) return;

        if (localName.equals("container")) {
            DescMeta desc = new DescMeta();
            Container container = new Container();

            if (attributes.getValue("id") == null) {
                return;
            }

            desc.setId(attributes.getValue("id"));
            container.setId(attributes.getValue("id"));

            if (attributes.getValue("type") == null) {
                return;
            }

            desc.setType(attributes.getValue("type"));

            container.addDescMetadata(desc);

            getInstance().addContainer(container);
        }
    }

}
