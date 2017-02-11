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
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.webkit.MimeTypeMap;

import org.apache.commons.lang3.ObjectUtils;
import org.opensilk.common.media.MediaMeta;
import org.opensilk.common.media.MediaMetaKt;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

/**
 * Created by drew on 3/21/16.
 */
@Deprecated
public class MediaMetaExtras {

    public interface MEDIA_TYPE {
        int UNKNOWN = 0;
        int MOVIE = 1;
        int TV_SERIES = 2;
        int TV_EPISODE = 3;
        int DIRECTORY = 4;
        int VIDEO = 5;
        int SPECIAL = 6;
    }

    private final MediaMeta meta;

    private MediaMetaExtras() {
        this(null);
    }

    private MediaMetaExtras(MediaMeta meta) {
        this.meta = meta != null ? meta : MediaMeta.empty();
    }

    public static MediaMetaExtras tvSeries() {
        return new MediaMetaExtras().setMediaType(MEDIA_TYPE.TV_SERIES);
    }

    public static MediaMetaExtras tvEpisode() {
        return new MediaMetaExtras().setMediaType(MEDIA_TYPE.TV_EPISODE);
    }

    public static MediaMetaExtras movie() {
        return new MediaMetaExtras().setMediaType(MEDIA_TYPE.MOVIE);
    }

    public static MediaMetaExtras video() {
        return new MediaMetaExtras().setMediaType(MEDIA_TYPE.VIDEO);
    }

    public static MediaMetaExtras directory() {
        return new MediaMetaExtras().setMediaType(MEDIA_TYPE.DIRECTORY);
    }

    public static MediaMetaExtras unknown() {
        return new MediaMetaExtras().setMediaType(MEDIA_TYPE.UNKNOWN);
    }

    public static MediaMetaExtras special() {
        return new MediaMetaExtras().setMediaType(MEDIA_TYPE.SPECIAL);
    }

    public static MediaMetaExtras from(MediaDescription description) {
        return new MediaMetaExtras(MediaMeta.from(description));
    }

    public static MediaMetaExtras from(MediaBrowser.MediaItem mediaItem) {
        return from(mediaItem.getDescription());
    }

    public Bundle getBundle() {
        return MediaMetaKt.getBundle(meta);
    }

    public MediaMetaExtras setMediaType(int type) {
        switch (type) {
            case MEDIA_TYPE.UNKNOWN:
                meta.setMimeType("application/unknown");
                break;
            case MEDIA_TYPE.MOVIE:
                meta.setMimeType("vnd.opensilk.org/movie");
                break;
            case MEDIA_TYPE.TV_SERIES:
                meta.setMimeType(TvContract.Programs.CONTENT_TYPE);
                break;
            case MEDIA_TYPE.TV_EPISODE:
                meta.setMimeType(TvContract.Programs.CONTENT_ITEM_TYPE);
                break;
            case MEDIA_TYPE.DIRECTORY:
                meta.setMimeType(DocumentsContract.Document.MIME_TYPE_DIR);
                break;
            case MEDIA_TYPE.VIDEO:
                meta.setMimeType("video/unknown");
                break;
            case MEDIA_TYPE.SPECIAL:
                meta.setMimeType("vnd.opensilk.org/special");
                break;
        }
        return this;
    }

    public int getMediaType() {
        if (meta.isMovie()) {
            return MEDIA_TYPE.MOVIE;
        } else if (meta.isTvSeries()) {
            return MEDIA_TYPE.TV_SERIES;
        } else if (meta.isTvEpisode()) {
            return MEDIA_TYPE.TV_EPISODE;
        } else if (meta.isDirectory()) {
            return MEDIA_TYPE.DIRECTORY;
        } else if (meta.isVideo()) {
            return MEDIA_TYPE.VIDEO;
        } else if (meta.getMimeType().equals("vnd.opensilk.org/special")) {
            return MEDIA_TYPE.SPECIAL;
        } else {
            return MEDIA_TYPE.UNKNOWN;
        }
    }

    public boolean isTvSeries() {
        return getMediaType() == MEDIA_TYPE.TV_SERIES;
    }

    public boolean isTvEpisode() {
        return meta.isTvEpisode();
    }

    public boolean isMovie() {
        return meta.isMovie();
    }

    public boolean isVideo() {
        return meta.isVideo();
    }

    public boolean isDirectory() {
        return meta.isDirectory();
    }

    public boolean isUnknown() {
        return meta.getMimeType().equals("application/unknown");
    }

    public boolean isSpecial() {
        return meta.getMimeType().equals("vnd.opensilk.org/special");
    }

    public boolean isVideoFile() {
        return isVideo();
    }

    public MediaMetaExtras setServerId(String id) {
        meta.set__internal4(id);
        return this;
    }

    public String getServerId() {
        return meta.get__internal4();
    }

    public MediaMetaExtras setParentId(String id) {
        meta.setParentMediaId(id);
        return this;
    }

    public String getParentId() {
        return meta.getParentMediaId();
    }

    public MediaMetaExtras setParentUri(Uri uri) {
        meta.setParentMediaId(uri.toString());
        return this;
    }

    public Uri getParentUri() {
        return Uri.parse(meta.getParentMediaId());
    }

    public MediaMetaExtras setIndexed(boolean yes) {
        meta.setParsed(yes);
        return this;
    }

    /**
     * @return true if indexed, for directories this means it has been scanned
     *          for videos this means they have metadata from the lookup service
     */
    public boolean isIndexed() {
        return meta.isParsed();
    }

    public MediaMetaExtras setMediaTitle(CharSequence title) {
        meta.setDisplayName(title.toString());
        return this;
    }

    /**
     * @return Title of media for directories this will be the same as the title
     *          for tv episodes and movies this is the title returned by the dlna server
     *          that can be used for lookups.
     */
    public String getMediaTitle() {
        return meta.getDisplayName();
    }

    public MediaMetaExtras setMediaUri(Uri mediaUri) {
        meta.setMediaUri(mediaUri);
        return this;
    }

    /**
     * @return media uri, used on API < 23 where {@link MediaDescription#getMediaUri()} doesnt exist
     */
    public Uri getMediaUri() {
        return meta.getMediaUri();
    }

    public MediaMetaExtras setEpisodeId(long id) {
        meta.set__internal1(String.valueOf(id));
        return this;
    }

    public long getEpisodeId() {
        return Long.valueOf(meta.get__internal1());
    }

    public MediaMetaExtras setSeriesId(long id) {
        meta.set__internal2(String.valueOf(id));
        return this;
    }

    public long getSeriesId() {
        return Long.valueOf(meta.get__internal2());
    }

    public MediaMetaExtras setMovieId(long id) {
        meta.set__internal3(String.valueOf(id));
        return this;
    }

    public long getMovieId() {
        return Long.valueOf(meta.get__internal3());
    }

    public MediaMetaExtras setBackdropUri(Uri backdropUri) {
        meta.setBackdropUri(backdropUri);
        return this;
    }

    public Uri getBackdropUri() {
        return meta.getBackdropUri();
    }

    public MediaMetaExtras setDate(String date){
        meta.setReleaseDate(date);
        return this;
    }

    public String getDate() {
        return meta.getReleaseDate();
    }

    public MediaMetaExtras setNumSeasons(int numSeasons) {
        meta.setSeasonCount(numSeasons);
        return this;
    }

    public int getNumSeasons() {
        return meta.getSeasonCount();
    }

    public MediaMetaExtras setSeasonNumber(int seasonNumber) {
        meta.setSeasonNumber(seasonNumber);
        return this;
    }

    public int getSeasonNumber() {
        return meta.getSeasonNumber();
    }

    public MediaMetaExtras setEpisodeNumber(int episodeNumber){
        meta.setEpisodeNumber(episodeNumber);
        return this;
    }

    public int getEpisodeNumber() {
        return meta.getEpisodeNumber();
    }

    public MediaMetaExtras setLastPosition(long pos) {
        meta.setLastPlaybackPosition(pos);
        return this;
    }

    public long getLastPosition() {
        return meta.getLastPlaybackPosition();
    }

    public MediaMetaExtras setDuration(long dur) {
        meta.setDuration(dur);
        return this;
    }

    public long getDuration() {
        return meta.getDuration();
    }


    public MediaMetaExtras setIconResource(int res){
        meta.setArtworkResourceId(res);
        return this;
    }

    /**
     * @return resource id to use if there is no icon uri (glide wont load vector drawables)
     */
    public int getIconResource(){
        return meta.getArtworkResourceId();
    }

    @Override
    public boolean equals(Object o) {
        return meta.equals(o);
    }

    @Override
    public int hashCode() {
        return meta.hashCode();
    }
}
