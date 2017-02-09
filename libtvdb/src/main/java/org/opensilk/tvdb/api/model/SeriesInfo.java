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
 * Created by drew on 3/19/16.
 */
@Root
public class SeriesInfo {
    private final Series series;
    private final List<Episode> episodes;

    public SeriesInfo(
            @Element(name = "Series") Series series,
            @ElementList(name = "Episode") List<Episode> episodes
    ) {
        this.series = series;
        this.episodes = episodes;
    }

    @Element(name = "Series")
    public Series getSeries() {
        return series;
    }

    @ElementList(inline = true, entry = "Episode", type = Episode.class, required = false)
    public List<Episode> getEpisodes() {
        return episodes;
    }
}
