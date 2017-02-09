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
public class TvEpisode {

    private final long id;
    private final String name;
    private final String overview;
    private final int episode_number;
    private final int season_number;
    private final String air_date;
    private final List<Person> guest_stars;

    public TvEpisode(
            long id,
            String name,
            String overview,
            int episode_number,
            int season_number,
            String air_date,
            List<Person> guest_stars
    ) {
        this.id = id;
        this.name = name;
        this.overview = overview;
        this.episode_number = episode_number;
        this.season_number = season_number;
        this.air_date = air_date;
        this.guest_stars = guest_stars;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOverview() {
        return overview;
    }

    public int getEpisodeNumber() {
        return episode_number;
    }

    public int getSeasonNumber() {
        return season_number;
    }

    public String getAirDate() {
        return air_date;
    }

    public List<Person> getGuestStars() {
        return guest_stars;
    }
}
