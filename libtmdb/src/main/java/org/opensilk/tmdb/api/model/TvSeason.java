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

import java.util.List;

/**
 * Created by drew on 4/2/16.
 */
public class TvSeason {

    private final long id;
    private final String name;
    private final int season_number;
    private final String air_date;
    private final String poster_path;
    final List<TvEpisode> episodes;

    public TvSeason(
            long id,
            String name,
            int season_number,
            String air_date,
            String poster_path,
            List<TvEpisode> episodes
    ) {
        this.id = id;
        this.name = name;
        this.season_number = season_number;
        this.air_date = air_date;
        this.poster_path = poster_path;
        this.episodes = episodes;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSeasonNumber() {
        return season_number;
    }

    public String getAirDate() {
        return air_date;
    }

    public String getPosterPath() {
        return poster_path;
    }

    public List<TvEpisode> getEpisodes() {
        return episodes;
    }
}
