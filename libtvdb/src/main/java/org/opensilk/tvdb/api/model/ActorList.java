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

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

/**
 * Created by drew on 3/20/16.
 */
@Root
public class ActorList {

    private final List<Actor> actors;

    public ActorList(@ElementList(name = "Actor") List<Actor> actors) {
        this.actors = actors;
    }

    @ElementList(inline = true, entry = "Actor", type = Actor.class)
    public List<Actor> getActors() {
        return actors;
    }

}
