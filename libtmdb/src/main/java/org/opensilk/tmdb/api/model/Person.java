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

package org.opensilk.tmdb.api.model;

/**
 * Created by drew on 4/2/16.
 */
public class Person {

    private final long id;
    private final String name;
    private final int order;
    private final String profile_path;

    public Person(long id, String name, int order, String profile_path) {
        this.id = id;
        this.name = name;
        this.order = order;
        this.profile_path = profile_path;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

    public String getProfilePath() {
        return profile_path;
    }
}
