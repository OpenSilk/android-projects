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
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

/**
 * Created by drew on 4/18/16.
 */
@Root(strict = false)
public class Updates {

    private final Long time;
    private final List<Long> series;
    private final List<Long> episodes;

    public Updates(
            @Element(name = "Time") Long time,
            @ElementList(name = "Series") List<Long> series,
            @ElementList(name = "Episode") List<Long> episodes
    ) {
        this.time = time;
        this.series = series;
        this.episodes = episodes;
    }

    @Element(name = "Time", type = Long.class)
    public Long getTime() {
        return time;
    }

    @ElementList(inline = true, entry = "Series", type = Long.class, required = false)
    public List<Long> getSeries() {
        return series;
    }

    @ElementList(inline = true, entry = "Episode", type = Long.class, required = false)
    public List<Long> getEpisodes() {
        return episodes;
    }
}
