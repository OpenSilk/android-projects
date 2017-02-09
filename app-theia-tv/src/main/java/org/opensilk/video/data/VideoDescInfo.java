/*
 * Copyright (c) 2016 OpenSilk Productions LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.video.data;

import android.media.MediaDescription;
import android.media.browse.MediaBrowser;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by drew on 4/2/16.
 */
public class VideoDescInfo {

    public static VideoDescInfo from(MediaBrowser.MediaItem mediaItem) {
        MediaDescription description = mediaItem.getDescription();
        return builder()
                .setTitle(description.getTitle())
                .setSubtitle(description.getSubtitle())
                .setOverview(description.getDescription())
                .build();
    }

    private final String title;
    private final String subtitle;
    private final String overview;

    private VideoDescInfo(Builder builder) {
        this.title = builder.title;
        this.subtitle = builder.subtitle;
        this.overview = builder.overview;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public boolean hasSubtitle() {
        return !StringUtils.isEmpty(subtitle);
    }

    public String getOverview() {
        return overview;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private String subtitle;
        private String overview;

        public Builder setTitle(CharSequence title) {
            this.title = title != null ? title.toString() : "";
            return this;
        }

        public Builder setSubtitle(CharSequence subtitle) {
            this.subtitle = subtitle != null ? subtitle.toString() : "";
            return this;
        }

        public Builder setOverview(CharSequence overview) {
            this.overview = overview != null ? overview.toString() : "";
            return this;
        }

        public VideoDescInfo build() {
            return new VideoDescInfo(this);
        }
    }
}
