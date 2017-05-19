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

import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.AudioItem;
import org.fourthline.cling.support.model.item.VideoItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by drew on 12/19/16.
 */

public class BasicView extends Feature {

    public static final String NAME = "samsung.com_BASICVIEW";

    private final List<Container> containers = new ArrayList<>();

    public BasicView(String name, int version) {
        super(name, version);
    }

    void addContainer(Container container) {
        containers.add(container);
    }

    public List<Container> getContainers() {
        return Collections.unmodifiableList(containers);
    }

    public String getAudioItemId() {
        Container m = getContainer(AudioItem.CLASS);
        return m != null ? m.getId() : null;
    }

    public String getVideoItemId() {
        Container m = getContainer(VideoItem.CLASS);
        return m != null ? m.getId() : null;
    }

    private Container getContainer(DIDLObject.Class clazz) {
        for (Container c: containers) {
            if (!c.getDescMetadata().isEmpty()) {
                if (clazz.getValue().equals((c.getDescMetadata().get(0).getType()))) {
                    return c;
                }
            }
        }
        return null;
    }

}
