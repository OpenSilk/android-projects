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

import org.apache.commons.lang3.StringUtils;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * Created by drew on 3/19/16.
 */
@Root(strict = false)
public class Banner {

    private final Long id;
    private final String bannerPath;
    private final String bannerType;
    private final String bannerType2;
    private final Float rating;
    private final Integer ratingCount;
    private final String thumbnailPath;
    private final Integer season;

    public Banner(
            @Element(name = "id") Long id,
            @Element(name = "BannerPath") String bannerPath,
            @Element(name = "BannerType") String bannerType,
            @Element(name = "BannerType2") String bannerType2,
            @Element(name = "Rating") Float rating,
            @Element(name = "RatingCount") Integer ratingCount,
            @Element(name = "ThumbnailPath") String thumbnailPath,
            @Element(name = "Season") Integer season
    ) {
        this.id = id;
        this.bannerPath = bannerPath;
        this.bannerType = bannerType;
        this.bannerType2 = bannerType2;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.thumbnailPath = thumbnailPath;
        this.season = season;
    }

    @Element(name = "id")
    public Long getId() {
        return id;
    }

    @Element(name = "BannerPath")
    public String getBannerPath() {
        return bannerPath;
    }


    /*
     * types:
     *
     * season, season
     * season, seasonwide
     * series, graphical
     * poster, 680x1000
     * fanart, 1920x1080
     * fanart, 1280x720
     */

    /**
     * This can be poster, fanart, series or season.
     */

    @Element(name = "BannerType")
    public String getBannerType() {
        return bannerType;
    }

    /**
     * For series banners it can be text, graphical, or blank.
     * For season banners it can be season or seasonwide.
     * For fanart it can be 1280x720 or 1920x1080.
     * For poster it will always be 680x1000
     */
    @Element(name = "BannerType2")
    public String getBannerType2() {
        return bannerType2;
    }

    @Element(name = "Rating", required = false)
    public Float getRating() {
        return rating;
    }

    @Element(name = "RatingCount", required = false)
    public Integer getRatingCount() {
        return ratingCount;
    }

    @Element(name = "ThumbnailPath", required = false)
    public String getThumbnailPath() {
        return thumbnailPath;
    }

    @Element(name = "Season", required = false) //Only when bannerType == "season"
    public Integer getSeason() {
        return season;
    }

    public boolean isPoster() {
        return StringUtils.equalsIgnoreCase(bannerType, "poster");
    }

    public boolean isSeason() {
        return StringUtils.equalsIgnoreCase(bannerType, "season")
                && StringUtils.equalsIgnoreCase(bannerType2, "season");
    }

    public boolean isSeries() {
        return StringUtils.equalsIgnoreCase(bannerType, "series");
    }

    public boolean isFanart() {
        return StringUtils.equalsIgnoreCase(bannerType, "fanart");
    }
}
