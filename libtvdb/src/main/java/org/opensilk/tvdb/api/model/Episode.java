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
public class Episode {

    private final Long id;
    private final String episodeName;
    private final String firstAired;
    private final String overview;
    private final Integer episodeNumber;
    private final Integer seasonNumber;
    private final Long seasonId;
    private final Long seriesId;

    public Episode(
            @Element(name = "id") Long id,
            @Element(name = "EpisodeName") String episodeName,
            @Element(name = "FirstAired") String firstAired,
            @Element(name = "Overview") String overview,
            @Element(name = "EpisodeNumber") Integer episodeNumber,
            @Element(name = "SeasonNumber") Integer seasonNumber,
            @Element(name = "seasonid") Long seasonId,
            @Element(name = "seriesid") Long seriesId
    ) {
        this.id = id;
        this.episodeName = episodeName;
        this.firstAired = firstAired;
        this.overview = overview;
        this.episodeNumber = episodeNumber;
        this.seasonNumber = seasonNumber;
        this.seasonId = seasonId;
        this.seriesId = seriesId;
    }

    @Element(name = "id")
    public Long getId() {
        return id;
    }

    @Element(name = "EpisodeName", required = false)
    public String getEpisodeName() {
        return episodeName;
    }

    @Element(name = "FirstAired", required = false)
    public String getFirstAired() {
        return firstAired;
    }

    @Element(name = "Overview", required = false)
    public String getOverview() {
        return overview;
    }

    @Element(name = "EpisodeNumber")
    public Integer getEpisodeNumber() {
        return episodeNumber;
    }

    @Element(name = "SeasonNumber")
    public Integer getSeasonNumber() {
        return seasonNumber;
    }

    @Element(name = "seasonid")
    public Long getSeasonId() {
        return seasonId;
    }

    @Element(name = "seriesid")
    public Long getSeriesId() {
        return seriesId;
    }
}
