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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Created by drew on 3/19/16.
 */
@Root(strict = false)
public class Series {

    private final Long id;
    private final String seriesName;
    private final String overview;
    private final String fanartPath;
    private final String posterPath;
    private final String firstAired;

    public Series(
            @Element(name = "id") Long id,
            @Element(name = "SeriesName") String seriesName,
            @Element(name = "Overview") String overview,
            @Element(name = "fanart") String fanartPath,
            @Element(name = "poster") String posterPath,
            @Element(name = "FirstAired") String firstAired
            ) {
        this.id = id;
        this.seriesName = seriesName;
        this.overview = overview;
        this.fanartPath = fanartPath;
        this.posterPath = posterPath;
        this.firstAired = firstAired;
    }

    @Element(name = "id")
    public Long getId() {
        return id;
    }

    @Element(name = "SeriesName")
    public String getSeriesName() {
        return seriesName;
    }

    @Element(name = "Overview", required = false)
    public String getOverview() {
        return overview;
    }

    @Element(name = "fanart", required = false)
    public String getFanartPath() {
        return fanartPath;
    }

    @Element(name = "poster", required = false)
    public String getPosterPath() {
        return posterPath;
    }

    @Element(name = "FirstAired", required = false)
    public String getFirstAired() {
        return firstAired;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("seriesName", seriesName)
                .append("fanart", fanartPath)
                .append("poster", posterPath)
                .build();
    }
}
