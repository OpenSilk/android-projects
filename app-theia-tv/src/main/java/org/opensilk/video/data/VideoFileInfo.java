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

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import org.opensilk.video.util.Utils;

import java.util.Locale;

/**
 * We dont notify listeners here as everything is immutable instead we notify the
 * presenter which will cause rebind. In hindsight, don't need to be observable
 *
 * Created by drew on 4/1/16.
 */
public class VideoFileInfo extends BaseObservable {

    public static class AudioTrackInfo {
        private final String codec;
        private final int bitrate;
        private final int rate;
        private final int channels;

        public AudioTrackInfo(String codec, int bitrate, int rate, int channels) {
            this.codec = codec;
            this.bitrate = bitrate;
            this.rate = rate;
            this.channels = channels;
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "%s %s %dHz %d channels",
                    codec,
                    Utils.humanReadableBitrate(bitrate),
                    rate,
                    channels);
        }

        @BindingAdapter("android:text")
        public static void bindInfoToTextView(TextView textView, AudioTrackInfo audioTrackInfo) {
            textView.setVisibility(audioTrackInfo != null ? View.VISIBLE : View.INVISIBLE);
            textView.setText(audioTrackInfo != null ? audioTrackInfo.toString() : "");
        }
    }

    public static class VideoTrackInfo {
        private final String codec;
        private final int width;
        private final int height;
        private final int bitrate;
        private final int frameRate;
        private final int frameRateDen;

        public VideoTrackInfo(String codec, int width, int height, int bitrate, int frameRate, int frameRateDen) {
            this.codec = codec;
            this.width = width;
            this.height = height;
            this.bitrate = bitrate;
            this.frameRate = frameRate;
            this.frameRateDen = frameRateDen;
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "%s %dx%d %s %.02ffps",
                    codec,
                    width,
                    height,
                    Utils.humanReadableBitrate(bitrate),
                    (float) frameRate / frameRateDen);
        }

        @BindingAdapter("android:text")
        public static void bindInfoToTextView(TextView textView, VideoTrackInfo videoTrackInfo) {
            textView.setVisibility(videoTrackInfo != null ? View.VISIBLE : View.INVISIBLE);
            textView.setText(videoTrackInfo != null ? videoTrackInfo.toString() : "");
        }
    }

    public static VideoFileInfo from(MediaBrowser.MediaItem mediaItem) {
        MediaDescription description = mediaItem.getDescription();
        return builder(MediaDescriptionUtil.getMediaUri(description))
                .setTitle(MediaDescriptionUtil.getMediaTitle(description))
                .build();
    }

    private final Uri uri;
    private final String title;
    private final long sizeBytes;
    private final long durationMilli;
    private final AudioTrackInfo firstAudioTrack;
    private final AudioTrackInfo secondAudioTrack;
    private final VideoTrackInfo firstVideoTrack;

    private VideoFileInfo(Builder builder) {
        this.uri = builder.uri;
        this.title = builder.title;
        this.sizeBytes = builder.sizeBytes;
        this.durationMilli = builder.durationMilli;
        this.firstAudioTrack = builder.firstAudioTrack;
        this.secondAudioTrack = builder.secondAudioTrack;
        this.firstVideoTrack = builder.firstVideoTrack;
    }

    public Uri getUri() {
        return uri;
    }

    @Bindable
    public String getTitle() {
        return title;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    @Bindable
    public String getSizeString() {
        return Utils.humanReadableSize(sizeBytes);
    }

    public long getDurationMilli() {
        return durationMilli;
    }

    @Bindable
    public String getDurationString() {
        return Utils.humanReadableDuration(durationMilli);
    }

    @Bindable
    public AudioTrackInfo getFirstAudioTrack() {
        return firstAudioTrack;
    }

    @Bindable
    public AudioTrackInfo getSecondAudioTrack() {
        return secondAudioTrack;
    }

    @Bindable
    public VideoTrackInfo getFirstVideoTrack() {
        return firstVideoTrack;
    }

    public Builder newBuilder() {
        Builder b = builder(uri)
                .setDuration(durationMilli)
                .setSize(sizeBytes)
                .setTitle(title)
                ;
        b.firstAudioTrack = firstAudioTrack;
        b.secondAudioTrack = secondAudioTrack;
        b.firstVideoTrack = firstVideoTrack;
        return b;
    }

    public static Builder builder(Uri uri) {
        return new Builder(uri);
    }

    public static class Builder {
        private Uri uri;
        private String title;
        private long sizeBytes;
        private long durationMilli;
        private AudioTrackInfo firstAudioTrack;
        private AudioTrackInfo secondAudioTrack;
        private VideoTrackInfo firstVideoTrack;

        public Builder(Uri uri) {
            this.uri = uri;
        }

        public Builder setTitle(CharSequence title) {
            this.title = title != null ? title.toString() : null;
            return this;
        }

        public Builder setSize(long sizeBytes) {
            this.sizeBytes = sizeBytes;
            return this;
        }

        public Builder setDuration(long durationMilli) {
            this.durationMilli = durationMilli;
            return this;
        }

        public Builder addAudioTrack(String codec, int bitrate, int rate, int channels) {
            final AudioTrackInfo audioTrackInfo = new AudioTrackInfo(codec, bitrate,
                    rate, channels);
            if (firstAudioTrack == null) {
                firstAudioTrack = audioTrackInfo;
            } else if (secondAudioTrack == null) {
                secondAudioTrack = audioTrackInfo;
            } //else ignore
            return this;
        }

        public Builder addVideoTrack(String codec, int width, int height, int bitrate, int frameRate, int frameRateDen) {
            final VideoTrackInfo videoTrackInfo = new VideoTrackInfo(codec, width, height,
                    bitrate, frameRate, frameRateDen);
            if (firstVideoTrack == null) {
                firstVideoTrack = videoTrackInfo;
            } //else ignore
            return this;
        }

        public VideoFileInfo build() {
            return new VideoFileInfo(this);
        }

    }
}
