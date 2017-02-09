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

/**
 * Created by drew on 3/20/16.
 */
public class Image {

    private final Integer height;
    private final Integer width;
    private final String file_path;
    private final Float vote_average;
    private final Integer vote_count;

    public Image(
            Integer height,
            Integer width,
            String file_path,
            Float vote_average,
            Integer vote_count
    ) {
        this.height = height;
        this.width = width;
        this.file_path = file_path;
        this.vote_average = vote_average;
        this.vote_count = vote_count;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getWidth() {
        return width;
    }

    public String getFilePath() {
        return file_path;
    }

    public Float getVoteAverage() {
        return vote_average;
    }

    public Integer getVoteCount() {
        return vote_count;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("width", width)
                .append("height", height)
                .build();
    }
}
