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
import android.net.Uri;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.opensilk.common.util.VersionUtils;

import timber.log.Timber;

/**
 * Created by drew on 3/21/16.
 */
public class MediaDescriptionUtil {

    public static MediaDescription.Builder setMediaUri(MediaDescription.Builder builder, MediaMetaExtras metaExtras, Uri uri) {
        if (VersionUtils.hasApi23()) {
            return builder.setMediaUri(uri);
        } else {
            metaExtras.setMediaUri(uri);
            return builder;
        }
    }

    public static Uri getMediaUri(MediaDescription description) {
        if (VersionUtils.hasApi23()) {
            return description.getMediaUri();
        } else {
            MediaMetaExtras metaExtras = MediaMetaExtras.from(description);
            return metaExtras.getMediaUri();
        }
    }

    public static MediaDescription.Builder newBuilder(MediaDescription description) {
        MediaDescription.Builder bob = new MediaDescription.Builder()
                .setIconUri(description.getIconUri())
                .setMediaId(description.getMediaId())
                .setExtras(description.getExtras())
                .setSubtitle(description.getSubtitle())
                .setTitle(description.getTitle())
                .setDescription(description.getDescription())
                ;
        if (VersionUtils.hasApi23()) {
            bob.setMediaUri(description.getMediaUri());
        }
        return bob;
    }

    public static String getMediaTitle(MediaDescription description) {
        MediaMetaExtras metaExtras = MediaMetaExtras.from(description);
        String mediaTitle = metaExtras.getMediaTitle();
        if (StringUtils.isEmpty(mediaTitle)) {
            Timber.e("MediaTitle not set in %s", MediaDescriptionUtil.getMediaUri(description));
            mediaTitle = description.getTitle() != null ? description.getTitle().toString() : "";
        }
        return mediaTitle;
    }

    public static String toString(MediaDescription description) {
        return new ToStringBuilder(description)
                .append("title", description.getTitle())
                .append("mediaId", description.getMediaId())
                .append("mediaUri", getMediaUri(description))
                .append("mediaTitle", getMediaTitle(description))
                .build();

    }

}
