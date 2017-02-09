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

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

/**
 * Created by drew on 3/20/16.
 */
public class TMDbConfig {

    private final Images images;

    public TMDbConfig(Images images) {
        this.images = images;
    }

    public Images getImages() {
        return images;
    }

    public static class Images {
        private final String base_url;
        private final String secure_base_url;
        private final List<String> profile_sizes;
        private final List<String> poster_sizes;
        private final List<String> backdrop_sizes;
        private final List<String> still_sizes;
        private final List<String> logo_sizes;

        public Images(
                String base_url,
                String secure_base_url,
                List<String> profile_sizes,
                List<String> poster_sizes,
                List<String> backdrop_sizes,
                List<String> still_sizes,
                List<String> logo_sizes
        ) {
            this.base_url = base_url;
            this.secure_base_url = secure_base_url;
            this.profile_sizes = profile_sizes;
            this.poster_sizes = poster_sizes;
            this.backdrop_sizes = backdrop_sizes;
            this.still_sizes = still_sizes;
            this.logo_sizes = logo_sizes;
        }

        public String getBaseUrl() {
            return base_url;
        }

        public String getSecureBaseUrl() {
            return secure_base_url;
        }

        public List<String> getProfileSizes() {
            return profile_sizes;
        }

        public List<String> getPosterSizes() {
            return poster_sizes;
        }

        public List<String> getBackdropSizes() {
            return backdrop_sizes;
        }

        public List<String> getStillSizes() {
            return still_sizes;
        }

        public List<String> getLogoSizes() {
            return logo_sizes;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("base_url", base_url)
                    .append("secure_base_url", secure_base_url)
                    .build();
        }
    }
}
