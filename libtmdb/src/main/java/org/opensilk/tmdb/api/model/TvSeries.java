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
public class TvSeries {

    private final long id;
    private final String name;
    private final String overview;
    private final String poster_path;
    private final String backdrop_path;
    private final String first_air_date;
    private final List<TvEpisode> episodes;

    public TvSeries(
            long id,
            String name,
            String overview,
            String poster_path,
            String backdrop_path,
            String first_air_date,
            List<TvEpisode> episodes
    ) {
        this.id = id;
        this.name = name;
        this.overview = overview;
        this.poster_path = poster_path;
        this.backdrop_path = backdrop_path;
        this.first_air_date = first_air_date;
        this.episodes = episodes;
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

    public String getPosterPath() {
        return poster_path;
    }

    public String getBackdropPath() {
        return backdrop_path;
    }

    public String getFirstAirDate() {
        return first_air_date;
    }

    public List<TvEpisode> getEpisodes() {
        return episodes;
    }
}
