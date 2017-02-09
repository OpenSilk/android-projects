/*
 * Copyright (c) 2016 OpenSilk Productions LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.tvdb.api.model;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Created by drew on 3/20/16.
 */
@Root(strict = false)
public class Actor implements Comparable<Actor> {

    private final Long id;
    private final String name;
    private final String role;
    private final Integer sortOrder;
    private final String imagePath;

    public Actor(
            @Element(name = "id") Long id,
            @Element(name = "Name") String name,
            @Element(name = "Role") String role,
            @Element(name = "SortOrder") Integer sortOrder,
            @Element(name = "Image") String imagePath
    ) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.sortOrder = sortOrder;
        this.imagePath = imagePath;
    }

    @Element(name = "id")
    public Long getId() {
        return id;
    }

    @Element(name = "Name", required = false)
    public String getName() {
        return name != null ? name : "No Name";
    }

    @Element(name = "Role", required = false)
    public String getRole() {
        return role != null ? role : "No Role";
    }

    @Element(name = "SortOrder", required = false)
    public Integer getSortOrder() {
        return sortOrder;
    }

    @Element(name = "Image", required = false)
    public String getImagePath() {
        return imagePath;
    }

    @Override
    public int compareTo(Actor another) {
        return sortOrder - another.sortOrder;
    }
}
