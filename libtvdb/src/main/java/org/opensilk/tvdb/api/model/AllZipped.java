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

import java.util.List;

/**
 * Created by drew on 3/19/16.
 */
public class AllZipped {

    private final Series series;
    private final List<Episode> episodes;
    private final List<Banner> banners;
    private final List<Actor> actors;

    private AllZipped(Builder builder) {
        this.series = builder.series;
        this.episodes = builder.episodes;
        this.banners = builder.banners;
        this.actors = builder.actors;
    }

    public Series getSeries() {
        return series;
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }

    public List<Banner> getBanners() {
        return banners;
    }

    public List<Actor> getActors() {
        return actors;
    }

    public static class Builder {

        private Series series;
        private List<Episode> episodes;
        private List<Banner> banners;
        private List<Actor> actors;

        public Builder setSeries(Series series) {
            this.series = series;
            return this;
        }

        public Builder setEpisodes(List<Episode> episodes) {
            this.episodes = episodes;
            return this;
        }

        public Builder setBanners(List<Banner> banners) {
            this.banners = banners;
            return this;
        }

        public Builder setActors(List<Actor> actors) {
            this.actors = actors;
            return this;
        }

        public AllZipped build() {
            return new AllZipped(this);
        }

    }
}
