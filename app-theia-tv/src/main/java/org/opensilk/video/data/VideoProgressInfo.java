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

import android.media.browse.MediaBrowser;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Created by drew on 4/9/16.
 */
public class VideoProgressInfo {

    public static VideoProgressInfo from(MediaBrowser.MediaItem mediaItem) {
        MediaMetaExtras metaExtras = MediaMetaExtras.from(mediaItem.getDescription());
        return builder().setDuration(metaExtras.getDuration())
                .setPosition(metaExtras.getLastPosition())
                .build();
    }

    private final long position;
    private final long duration;
    private final float completion;

    private VideoProgressInfo(Builder b) {
        this.position = b.position;
        this.duration = b.duration;
        this.completion = b.completion;
    }

    public long getPosition() {
        return position;
    }

    public long getDuration() {
        return duration;
    }

    public int getProgress() {
        return (int) (completion * 100);
    }

    public float getCompletion() {
        return completion;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("progress", getProgress())
                .append("position", getPosition())
                .append("duration", getDuration())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long position;
        private long duration;
        private float completion;

        public Builder setPosition(long position) {
            this.position = position;
            return this;
        }

        public Builder setDuration(long duration) {
            this.duration = duration;
            return this;
        }

        public VideoProgressInfo build() {
            if (position > 0 && duration > 0) {
                completion = (float) position / (float) duration;
            }
            return new VideoProgressInfo(this);
        }
    }
}
