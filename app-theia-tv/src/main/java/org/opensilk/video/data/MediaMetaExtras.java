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
import android.net.Uri;
import android.os.Bundle;

import org.apache.commons.lang3.ObjectUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

/**
 * Created by drew on 3/21/16.
 */
public class MediaMetaExtras {

    public interface KEY {
        //todo rename these
        String KEY_MEDIA_TYPE = "meta_media_type";
        String KEY_SERVER_ID = "meta_server_id";
        String KEY_PARENT_ID = "meta_parent_id";
        String KEY_PARENT_URI = "meta_parent_uri";
        String IS_INDEXED = "meta_is_indexed";
        String KEY_MEDIA_TITLE = "media_media_title";
        String KEY_MEDIA_URI = "media_media_uri";
    }

    public interface MEDIA_TYPE {
        int UNKNOWN = 0;
        int MOVIE = 1;
        int TV_SERIES = 2;
        int TV_EPISODE = 3;
        int DIRECTORY = 4;
        int VIDEO = 5;
        int SPECIAL = 6;
    }

    private final Bundle meta;

    private MediaMetaExtras() {
        this(null);
    }

    private MediaMetaExtras(Bundle meta) {
        this.meta = meta != null ? meta : new Bundle();
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
        Bundle b = description.getExtras();
        if (b == null) {
            throw new RuntimeException(String.format(Locale.US,
                    "Description [%s] did not have extras this is an error!!", description.getTitle()));
        }
        return new MediaMetaExtras(b);
    }

    public static MediaMetaExtras from(Bundle bundle) {
        return new MediaMetaExtras(bundle);
    }

    public static MediaMetaExtras from(MediaBrowser.MediaItem mediaItem) {
        return from(mediaItem.getDescription());
    }

    public Bundle getBundle() {
        return meta;
    }

    public MediaMetaExtras setMediaType(int type) {
        meta.putInt(KEY.KEY_MEDIA_TYPE, type);
        return this;
    }

    public int getMediaType() {
        return meta.getInt(KEY.KEY_MEDIA_TYPE);
    }

    public boolean isTvSeries() {
        return getMediaType() == MEDIA_TYPE.TV_SERIES;
    }

    public boolean isTvEpisode() {
        return getMediaType() == MEDIA_TYPE.TV_EPISODE;
    }

    public boolean isMovie() {
        return getMediaType() == MEDIA_TYPE.MOVIE;
    }

    public boolean isVideo() {
        return getMediaType() == MEDIA_TYPE.VIDEO;
    }

    public boolean isDirectory() {
        return getMediaType() == MEDIA_TYPE.DIRECTORY;
    }

    public boolean isUnknown() {
        return getMediaType() == MEDIA_TYPE.UNKNOWN;
    }

    public boolean isSpecial() {
        return getMediaType() == MEDIA_TYPE.SPECIAL;
    }

    public boolean isVideoFile() {
        return isTvEpisode() || isMovie() || isVideo();
    }

    public MediaMetaExtras setServerId(String id) {
        meta.putString(KEY.KEY_SERVER_ID, id);
        return this;
    }

    public String getServerId() {
        return meta.getString(KEY.KEY_SERVER_ID);
    }

    public MediaMetaExtras setParentId(String id) {
        meta.putString(KEY.KEY_PARENT_ID, id);
        return this;
    }

    public String getParentId() {
        Timber.e("getParentId() Dont use this its not implemented in database");
        return meta.getString(KEY.KEY_PARENT_ID);
    }

    public MediaMetaExtras setParentUri(Uri uri) {
        meta.putParcelable(KEY.KEY_PARENT_URI, uri);
        return this;
    }

    public Uri getParentUri() {
        return meta.getParcelable(KEY.KEY_PARENT_URI);
    }

    public MediaMetaExtras setIndexed(boolean yes) {
        meta.putBoolean(KEY.IS_INDEXED, yes);
        return this;
    }

    /**
     * @return true if indexed, for directories this means it has been scanned
     *          for videos this means they have metadata from the lookup service
     */
    public boolean isIndexed() {
        return meta.getBoolean(KEY.IS_INDEXED, false);
    }

    public MediaMetaExtras setMediaTitle(CharSequence title) {
        meta.putCharSequence(KEY.KEY_MEDIA_TITLE, title);
        return this;
    }

    /**
     * @return Title of media for directories this will be the same as the title
     *          for tv episodes and movies this is the title returned by the dlna server
     *          that can be used for lookups.
     */
    public String getMediaTitle() {
        return meta.getCharSequence(KEY.KEY_MEDIA_TITLE, "").toString();
    }

    public MediaMetaExtras setMediaUri(Uri mediaUri) {
        meta.putParcelable(KEY.KEY_MEDIA_URI, mediaUri);
        return this;
    }

    /**
     * @return media uri, used on API < 23 where {@link MediaDescription#getMediaUri()} doesnt exist
     */
    public Uri getMediaUri() {
        return meta.getParcelable(KEY.KEY_MEDIA_URI);
    }

    public MediaMetaExtras setEpisodeId(long id) {
        meta.putLong("meta_episode_id", id);
        return this;
    }

    public long getEpisodeId() {
        return meta.getLong("meta_episode_id");
    }

    public MediaMetaExtras setSeriesId(long id) {
        meta.putLong("meta_series_id", id);
        return this;
    }

    public long getSeriesId() {
        return meta.getLong("meta_series_id");
    }

    public MediaMetaExtras setMovieId(long id) {
        meta.putLong("meta_movie_id", id);
        return this;
    }

    public long getMovieId() {
        return meta.getLong("meta_movie_id");
    }

    public MediaMetaExtras setBackdropUri(Uri backdropUri) {
        meta.putParcelable("meta_backdrop_uri", backdropUri);
        return this;
    }

    public Uri getBackdropUri() {
        return meta.getParcelable("meta_backdrop_uri");
    }

    public MediaMetaExtras setDate(String date){
        meta.putString("meta_date", date);
        return this;
    }

    public String getDate() {
        return meta.getString("meta_date");
    }

    public MediaMetaExtras setNumSeasons(int numSeasons) {
        meta.putInt("meta_num_seasons", numSeasons);
        return this;
    }

    public int getNumSeasons() {
        return meta.getInt("meta_num_seasons");
    }

    public MediaMetaExtras setSeasonNumber(int seasonNumber) {
        meta.putInt("meta_season_number", seasonNumber);
        return this;
    }

    public int getSeasonNumber() {
        return meta.getInt("meta_season_number");
    }

    public MediaMetaExtras setEpisodeNumber(int episodeNumber){
        meta.putInt("meta_episode_number", episodeNumber);
        return this;
    }

    public int getEpisodeNumber() {
        return meta.getInt("meta_episode_number");
    }

    public MediaMetaExtras setLastPosition(long pos) {
        meta.putLong("meta_last_position", pos);
        return this;
    }

    public long getLastPosition() {
        return meta.getLong("meta_last_position", -1);
    }

    public MediaMetaExtras setDuration(long dur) {
        meta.putLong("meta_duration", dur);
        return this;
    }

    public long getDuration() {
        return meta.getLong("meta_duration", -1);
    }


    public MediaMetaExtras setIconResource(int res){
        meta.putInt("meta_icon_res", res);
        return this;
    }

    /**
     * @return resource id to use if there is no icon uri (glide wont load vector drawables)
     */
    public int getIconResource(){
        return meta.getInt("meta_icon_res", -1);
    }

    public MediaMetaExtras setDirty(boolean dirty) {
        meta.putBoolean("meta_is_dirty", dirty);
        return this;
    }

    /**
     * @return true if mediaitem was updated and no longer reflects what is in the database
     */
    public boolean isDirty() {
        return meta.getBoolean("meta_is_dirty");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaMetaExtras that = (MediaMetaExtras) o;
        Set<String> keys = new HashSet<>(meta.keySet());
        Set<String> thatKeys = new HashSet<>(that.meta.keySet());
        //TODO its possible for one of us to have extra keys that are null so this isnt right
        if (ObjectUtils.notEqual(keys, thatKeys)) return false;
        for (String key : keys) {
            if (ObjectUtils.notEqual(meta.get(key), that.meta.get(key))) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return meta.hashCode();
    }
}
