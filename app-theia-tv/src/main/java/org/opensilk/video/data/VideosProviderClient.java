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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.opensilk.common.dagger.ForApplication;
import org.opensilk.tmdb.api.model.Image;
import org.opensilk.tmdb.api.model.ImageList;
import org.opensilk.tmdb.api.model.Movie;
import org.opensilk.tmdb.api.model.TMDbConfig;
import org.opensilk.tvdb.api.model.Actor;
import org.opensilk.tvdb.api.model.AllZipped;
import org.opensilk.tvdb.api.model.Banner;
import org.opensilk.tvdb.api.model.Episode;
import org.opensilk.tvdb.api.model.Series;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

/**
 * Created by drew on 4/1/16.
 */
public class VideosProviderClient {

    private final ContentResolver mResolver;
    private final VideosUris mUris;
    private final TVDbClient mTvDbClient;
    private final MovieDbClient mMovieDbClient;

    @Inject
    public VideosProviderClient(
            @ForApplication Context mContext,
            VideosUris mUris,
            @Named("tvdb_root") String tvdbRoot
    ) {
        this.mResolver = mContext.getContentResolver();
        this.mUris = mUris;
        this.mTvDbClient = new TVDbClient(Uri.parse(tvdbRoot));
        this.mMovieDbClient = new MovieDbClient();
    }

    public VideosUris uris() {
        return mUris;
    }

    public TVDbClient tvdb() {
        return mTvDbClient;
    }

    public MovieDbClient moviedb() {
        return mMovieDbClient;
    }

    public Integer getMediaType(@NonNull Uri mediaUri) {
        Cursor c = mResolver.query(mUris.media(),
                new String[]{"media_category"},
                "media_uri=?", new String[]{mediaUri.toString()}, null);
        try {
            if (c != null && c.moveToFirst()) {
                return c.getInt(0);
            }
            return null;
        } finally {
            closeCursor(c);
        }
    }

    public boolean insertMedia(MediaBrowser.MediaItem mediaItem) {
        ContentValues cv = new ContentValues(10);
        MediaDescription description = mediaItem.getDescription();
        MediaMetaExtras metaExtras = MediaMetaExtras.from(description.getExtras());
        Uri mediaUri = MediaDescriptionUtil.getMediaUri(description);
        cv.put("_display_name", metaExtras.getMediaTitle());
        String descriptionTitle = description.getTitle() != null ?
                description.getTitle().toString() : null;
        cv.put("_title", descriptionTitle);
        String descriptionSubtitle = description.getSubtitle() != null ?
                description.getSubtitle().toString() : null;
        cv.put("_subtitle", descriptionSubtitle);

        cv.put("parent_media_uri", metaExtras.getParentUri().toString());
        cv.put("server_id", metaExtras.getServerId());
        cv.put("media_category", metaExtras.getMediaType());
        if (description.getIconUri() != null) {
            cv.put("artwork_uri", description.getIconUri().toString());
        }
        if (metaExtras.getBackdropUri() != null) {
            cv.put("backdrop_uri", metaExtras.getBackdropUri().toString());
        }
        cv.put("is_indexed", metaExtras.isIndexed() ? 1 : 0);
        //Not setting last_played or duration service does that

        if (metaExtras.isTvEpisode()) {
            cv.put("episode_id", metaExtras.getEpisodeId());
            cv.put("series_id", metaExtras.getSeriesId());
        } else if (metaExtras.isMovie()) {
            cv.put("movie_id", metaExtras.getMovieId());
        }

        try {
            int num = mResolver.update(mUris.media(), cv, "media_uri=?", new String[]{mediaUri.toString()});
            if (num > 0) {
                Timber.d("Updated %d rows for %s", num, metaExtras.getMediaTitle());
                return true;
            }
            cv.put("media_uri", mediaUri.toString());
            cv.put("date_added", System.currentTimeMillis());
            return mResolver.insert(mUris.media(), cv) != null;
        } catch (SQLiteException e) {
            Timber.w(e, "Failed updating %s values=%s", metaExtras.getMediaTitle(), cv.toString());
            return false;
        }
    }

    public int removeMedia(MediaBrowser.MediaItem mediaItem) {
        Uri mediaUri = MediaDescriptionUtil.getMediaUri(mediaItem.getDescription());
        if (mediaUri == null) {
            Timber.e("Refusing delete of %s no mediaUri", MediaItemUtil.getMediaTitle(mediaItem));
            return 0;
        }
        int num = mResolver.delete(mUris.media(), "media_uri=?", new String[]{mediaUri.toString()});
        Timber.d("Deleted %d rows for %s", num, mediaUri);
        return num;
    }

    public void removeOrphans(MediaBrowser.MediaItem parentItem, List<MediaBrowser.MediaItem> childItems) {
        List<MediaBrowser.MediaItem> indexedItems = getChildren(parentItem);
        for (MediaBrowser.MediaItem item : childItems) {
            Uri itemUri = MediaItemUtil.getMediaUri(item);
            ListIterator<MediaBrowser.MediaItem> indexedII = indexedItems.listIterator();
            while (indexedII.hasNext()) {
                MediaBrowser.MediaItem indexedItem = indexedII.next();
                Uri indexedItemUri = MediaItemUtil.getMediaUri(indexedItem);
                if (itemUri.equals(indexedItemUri)) {
                    indexedII.remove();
                    break;
                }
            }
        }
        for (MediaBrowser.MediaItem mediaItem : indexedItems) {
            removeMediaRecursive(mediaItem);
        }
    }

    private void removeMediaRecursive(MediaBrowser.MediaItem mediaItem) {
        if (mediaItem.isBrowsable()) {
            List<MediaBrowser.MediaItem> orphanChildren = getChildren(mediaItem);
            for (MediaBrowser.MediaItem orphanChild : orphanChildren) {
                removeMediaRecursive(orphanChild);
            }
        }
        Timber.i("Removing orphaned media %s@%s", MediaItemUtil.getMediaTitle(mediaItem),
                MediaItemUtil.getMediaUri(mediaItem));
        removeMedia(mediaItem);
    }

    public @Nullable MediaBrowser.MediaItem getMedia(@NonNull Uri mediaUri) {
        Cursor c = mResolver.query(mUris.media(), MEDIA_PROJ,
                "media_uri=?", new String[]{mediaUri.toString()}, null);
        try {
            if (c != null && c.moveToFirst()) {
                return buildMedia(c);
            }
            return null;
        } finally {
            closeCursor(c);
        }
    }

    public List<MediaBrowser.MediaItem> getTvEpisodes(String mediaId) {
        long id;
        try {
            id = Long.valueOf(StringUtils.removeStart(mediaId, "tv_series:"));
        } catch (NumberFormatException e) {
            Timber.e(e, "getTvEpisodes(%s)", mediaId);
            return Collections.emptyList();
        }
        Cursor c = mResolver.query(mUris.media(), MEDIA_PROJ,
                "media_category=? AND series_id=?", new String[]{
                        String.valueOf(MediaMetaExtras.MEDIA_TYPE.TV_EPISODE),
                        String.valueOf(id)
                }, null);
        try {
            if (c != null && c.moveToFirst()) {
                ArrayList<MediaBrowser.MediaItem> list = new ArrayList<>(c.getCount());
                do {
                    list.add(buildMedia(c));
                } while (c.moveToNext());
                return list;
            }
            return Collections.emptyList();
        } finally {
            closeCursor(c);
        }
    }

    static final String[] MEDIA_PROJ = new String[]{
            "_display_name", "parent_media_uri", "server_id",
            "_title", "_subtitle", "artwork_uri", "is_indexed",
            "last_position", "duration", "media_category", "media_uri",
            "series_id", "episode_id", "movie_id", "backdrop_uri"};

    MediaBrowser.MediaItem buildMedia(Cursor c) {
        String displayName = c.getString(0);
        Uri parentUri = Uri.parse(c.getString(1));
        String serverId = c.getString(2);
        String title = c.getString(3);
        String subtitle = c.getString(4);
        Uri artworkUri = StringUtils.isEmpty(c.getString(5)) ? null :
                Uri.parse(c.getString(5));
        boolean isIndexed = c.getInt(6) == 1;
        long lastPosition = c.isNull(7) ? -1 : c.getLong(7);
        long duration = c.isNull(8) ? -1 : c.getLong(8);
        int type = c.getInt(9);
        Uri mediaUri = Uri.parse(c.getString(10));
        long seriesId = c.isNull(11) ? -1 : c.getLong(11);
        long episodeId = c.isNull(12) ? -1 : c.getLong(12);
        long movieId = c.isNull(13) ? -1 : c.getLong(13);
        Uri backdropUri = c.isNull(14) ? null : Uri.parse(c.getString(14));

        String pfx = "media:";
        int flag = 0;
        switch (type) {
            case MediaMetaExtras.MEDIA_TYPE.DIRECTORY:
                pfx = "directory:";
                flag = MediaBrowser.MediaItem.FLAG_BROWSABLE;
                break;
            case MediaMetaExtras.MEDIA_TYPE.TV_EPISODE:
                pfx = "tv_episode:";
                flag = MediaBrowser.MediaItem.FLAG_PLAYABLE;
                break;
            case MediaMetaExtras.MEDIA_TYPE.MOVIE:
                pfx = "movie:";
                flag = MediaBrowser.MediaItem.FLAG_PLAYABLE;
                break;
            case MediaMetaExtras.MEDIA_TYPE.VIDEO:
                pfx = "video:";
                flag = MediaBrowser.MediaItem.FLAG_PLAYABLE;
                break;
        }

        MediaDescription.Builder builder = new MediaDescription.Builder()
                .setMediaId(pfx+mediaUri)
                .setTitle(StringUtils.isEmpty(title) ? displayName : title)
                .setSubtitle(subtitle)
                .setIconUri(artworkUri)
                ;

        MediaMetaExtras metaExtras = MediaMetaExtras.unknown()
                .setMediaType(type)
                .setMediaTitle(displayName)
                .setParentUri(parentUri)
                .setServerId(serverId)
                .setIndexed(isIndexed)
                .setLastPosition(lastPosition)
                .setDuration(duration)
                .setEpisodeId(episodeId)
                .setSeriesId(seriesId)
                .setMovieId(movieId)
                .setBackdropUri(backdropUri)
                ;

        MediaDescriptionUtil.setMediaUri(builder, metaExtras, mediaUri);
        builder.setExtras(metaExtras.getBundle());
        return new MediaBrowser.MediaItem(builder.build(), flag);
    }

    public @NonNull List<MediaBrowser.MediaItem> getChildren(@NonNull MediaBrowser.MediaItem mediaItem) {
        Uri parentUri = MediaDescriptionUtil.getMediaUri(mediaItem.getDescription());
        return getChildren(parentUri);
    }

    public @NonNull List<MediaBrowser.MediaItem> getChildren(@NonNull Uri parentUri) {
        Cursor c = mResolver.query(mUris.media(), MEDIA_PROJ,
                "parent_media_uri=?", new String[]{parentUri.toString()}, "_display_name");
        try {
            if (c != null && c.moveToFirst()) {
                ArrayList<MediaBrowser.MediaItem> lst = new ArrayList<>(c.getCount());
                do {
                    lst.add(buildMedia(c));
                } while (c.moveToNext());
                return lst;
            }
            return Collections.emptyList();
        } finally {
            closeCursor(c);
        }
    }

    public List<MediaBrowser.MediaItem> getTopLevelDirectories() {
        Cursor c = mResolver.query(mUris.media(), new String[]{"media_uri", "parent_media_uri"},
                String.format(Locale.US, "media_category=%d AND is_indexed=1",
                        MediaMetaExtras.MEDIA_TYPE.DIRECTORY), null, null);
        try {
            if (c != null && c.moveToFirst()) {
                List<Pair<String, String>> pairs = new ArrayList<>();
                do {
                    String mediaUri = c.getString(0);
                    String parentUri = c.getString(1);
                    pairs.add(Pair.of(parentUri, mediaUri));
                } while (c.moveToNext());
                Tree<String> tree = Tree.newTree(pairs);
                List<MediaBrowser.MediaItem> mediaItems = new ArrayList<>();
                for (Tree.Node<String> node : tree.getNodes()) {
                    MediaBrowser.MediaItem mediaItem = getMedia(Uri.parse(node.getSelf()));
                    if (mediaItem != null) {
                        mediaItems.add(mediaItem);
                    }
                }
                return mediaItems;
            }
            return Collections.emptyList();
        } finally {
            closeCursor(c);
        }
    }

    public void markIndexed(Uri mediaUri, boolean indexed) {
        try {
            ContentValues values = new ContentValues();
            values.put("is_indexed", indexed ? 1 : 0);
            mResolver.update(mUris.media(), values, "media_uri=?", new String[]{mediaUri.toString()});
        } catch (SQLiteException e) {
            Timber.w("Failed to mark %s as indexed", mediaUri);
        }
    }

    public int updateMediaLastPosition(@NonNull Uri mediaUri, long position) {
        ContentValues values = new ContentValues(1);
        values.put("last_position", position);
        values.put("last_played", System.currentTimeMillis());
        return updateMedia(mediaUri, values);
    }

    public long updateMediaDuration(@NonNull Uri mediaUri, long duration) {
        ContentValues values = new ContentValues(1);
        values.put("duration", duration);
        return updateMedia(mediaUri, values);
    }

    public long updateMediaFileSize(@NonNull Uri mediaUri, long size) {
        ContentValues values = new ContentValues(1);
        values.put("file_size", size);
        return updateMedia(mediaUri, values);
    }

    private int updateMedia(@NonNull Uri mediaUri, ContentValues values) {
        try {
            return mResolver.update(mUris.media(), values, "media_uri=?", new String[]{mediaUri.toString()});
        } catch (SQLiteException e) {
            Timber.w(e, "Unable to update media %s", mediaUri);
            return 0;
        }
    }

    public class TVDbClient {
        private final Uri tvdbRoot;

        public TVDbClient(Uri tvdbRoot) {
            this.tvdbRoot = tvdbRoot;
        }

        public Uri rootUri() {
            return tvdbRoot;
        }

        public Uri bannerRootUri() {
            return Uri.withAppendedPath(tvdbRoot, "banners/");
        }

        public Uri makeBannerUri(String path) {
            return Uri.withAppendedPath(bannerRootUri(), path);
        }

        public MediaBrowser.MediaItem getTvSeries(String mediaId) {
            try {
                long id = Long.valueOf(StringUtils.removeStart(mediaId, "tv_series:"));
                return getTvSeries(id);
            } catch (NumberFormatException e) {
                Timber.e(e, "getTvSeries(%s)", mediaId);
                return null;
            }
        }

        public MediaBrowser.MediaItem getTvSeries(long id) {
            Cursor c = mResolver.query(mUris.tvSeries(id), TV_SERIES_PROJ , null, null, null);
            try {
                if (c != null && c.moveToFirst()) {
                    return buildTvSeries(c);
                }
                return null;
            } finally {
                closeCursor(c);
            }
        }

        public final String[] TV_SERIES_PROJ = new String[]{"_display_name",
                "overview", "poster_path", "backdrop_path", "_id"};

        public MediaBrowser.MediaItem buildTvSeries(Cursor c) {
            String displayName = c.getString(0);
            String overview = c.getString(1);
            String posterPath = c.getString(2);
            String backdropPath = c.getString(3);
            long id = c.getLong(4);
            MediaDescription.Builder builder = new MediaDescription.Builder()
                    .setMediaId("tv_series:"+id)
                    .setTitle(displayName)
                    .setDescription(overview);
            MediaMetaExtras metaExtras = MediaMetaExtras.tvSeries();
            if (!StringUtils.isEmpty(posterPath)) {
                builder.setIconUri(makeBannerUri(posterPath));
            }
            if (!StringUtils.isEmpty(backdropPath)) {
                metaExtras.setBackdropUri(makeBannerUri(backdropPath));
            }
            builder.setExtras(metaExtras.getBundle());
            return new MediaBrowser.MediaItem(builder.build(),
                    MediaBrowser.MediaItem.FLAG_BROWSABLE| MediaBrowser.MediaItem.FLAG_PLAYABLE);
        }

        public String makeSubtitle(String seriesName, int seasonNumber, int episodeNumber) {
            return String.format(Locale.getDefault(), "%s - S%02dE%02d",
                    seriesName, seasonNumber, episodeNumber);
        }

        public long getSeriesAssociation(String query) {
            Cursor c = mResolver.query(mUris.tvLookups(),
                    new String[]{"series_id"}, "q=?", new String[]{query}, null);
            try {
                long id = -1;
                if (c != null && c.moveToFirst()) {
                    id = c.getLong(0);
                }
                return id;
            } finally {
                closeCursor(c);
            }
        }

        public void setSeriesAssociation(String query, long series_id){
            ContentValues cv = new ContentValues(2);
            cv.put("q", query);
            cv.put("series_id", series_id);
            Uri uri = mResolver.insert(mUris.tvLookups(), cv);
            //
        }

        public @Nullable Episode getEpisode(long episodeId) {
            Cursor c = mResolver.query(mUris.tvEpisodes(), new String[]{"_id",
                    "_display_name", "first_aired", "episode_number",
                    "season_number", "series_id", "overview",
            }, "_id=?", new String[]{String.valueOf(episodeId)}, null);
            try {
                if (c != null && c.moveToFirst()) {
                    long id = c.getLong(0);
                    String displayName = c.getString(1);
                    String firstAired = c.getString(2);
                    int episode = c.getInt(3);
                    int season = c.getInt(4);
                    long seriesId = c.getLong(5);
                    String overview = c.getString(6);
                    return new Episode(id, displayName, firstAired, overview,
                            episode, season, null, seriesId);
                }
                return null;
            } finally {
                closeCursor(c);
            }
        }

        public @Nullable Series getSeries(long id) {
            Cursor c = mResolver.query(mUris.tvSeries(id), new String[]{
                    "_display_name", "overview", "first_aired",
                    "poster_path", "backdrop_path"
            }, null, null, null);
            try {
                if (c != null && c.moveToFirst()) {
                    String displayName = c.getString(0);
                    String overview = c.getString(1);
                    String firstAired = c.getString(2);
                    String posterPath = c.getString(3);
                    String backdropPath = c.getString(4);
                    return new Series(id, displayName, overview, backdropPath, posterPath, firstAired);
                }
                return null;
            } finally {
                closeCursor(c);
            }
        }

        public void insertAllZipped(AllZipped allZipped) {
            insertTvSeries(allZipped.getSeries());
            if (allZipped.getEpisodes() != null && !allZipped.getEpisodes().isEmpty()) {
                for (Episode episode : allZipped.getEpisodes()) {
                    insertEpisode(episode);
                }
            }
            if (allZipped.getBanners() != null && !allZipped.getBanners().isEmpty()) {
                for (Banner banner : allZipped.getBanners()) {
                    insertBanner(allZipped.getSeries().getId(), banner);
                }
            }
            if (allZipped.getActors() != null && !allZipped.getActors().isEmpty()) {
                for (Actor actor : allZipped.getActors()) {
                    insertActor(allZipped.getSeries().getId(), actor);
                }
            }
        }

        public Uri insertTvSeries(Series series) {
            ContentValues values = new ContentValues(10);
            values.put("_id", series.getId());
            values.put("_display_name", series.getSeriesName());
            if (!StringUtils.isEmpty(series.getOverview())) {
                values.put("overview", series.getOverview());
            }
            if (!StringUtils.isEmpty(series.getFirstAired())) {
                values.put("first_aired", series.getFirstAired());
            }
            if (!StringUtils.isEmpty(series.getPosterPath())) {
                values.put("poster_path", series.getPosterPath());
            }
            if (!StringUtils.isEmpty(series.getFanartPath())) {
                values.put("backdrop_path", series.getFanartPath());
            }
            return mResolver.insert(mUris.tvSeries(), values);
        }

        public Uri insertEpisode(Episode episode) {
            ContentValues values = new ContentValues(10);
            values.put("_id", episode.getId());
            if (!StringUtils.isEmpty(episode.getEpisodeName())) {
                values.put("_display_name", episode.getEpisodeName());
            }
            if (!StringUtils.isEmpty(episode.getOverview())) {
                values.put("overview", episode.getOverview());
            }
            if (!StringUtils.isEmpty(episode.getFirstAired())) {
                values.put("first_aired", episode.getFirstAired());
            }
            values.put("episode_number", episode.getEpisodeNumber());
            values.put("season_number", episode.getSeasonNumber());
            values.put("series_id", episode.getSeriesId());
            return mResolver.insert(mUris.tvEpisodes(), values);
        }

        public @NonNull List<Episode> getEpisodes(long seriesId) {
            Cursor c = mResolver.query(mUris.tvEpisodes(), new String[]{"_id",
                "_display_name", "episode_number", "season_number"},
                    "series_id="+seriesId, null, "season_number, episode_number");
            try {
                if (c != null && c.moveToFirst()) {
                    List<Episode> episodes = new ArrayList<>(c.getCount());
                    do {
                        long id = c.getLong(0);
                        String name = c.getString(1);
                        int episode_number = c.getInt(2);
                        int season_number = c.getInt(3);
                        episodes.add(new Episode(id, name, null, null,
                                episode_number, season_number, null, seriesId));
                    } while (c.moveToNext());
                    return episodes;
                }
                return Collections.emptyList();
            } finally {
                closeCursor(c);
            }
        }

        public Uri insertBanner(long seriesId, Banner banner) {
            ContentValues values = new ContentValues(10);
            values.put("_id", banner.getId());
            values.put("path", banner.getBannerPath());
            values.put("type", banner.getBannerType());
            values.put("type2", banner.getBannerType2());
            if (banner.getRating() != null) {
                values.put("rating", banner.getRating());
            }
            if (banner.getRatingCount() != null) {
                values.put("rating_count", banner.getRatingCount());
            }
            if (banner.getThumbnailPath() != null) {
                values.put("thumb_path", banner.getThumbnailPath());
            }
            if (banner.getSeason() != null) {
                values.put("season", banner.getSeason());
            }
            values.put("series_id", seriesId);
            return mResolver.insert(mUris.tvBanners(), values);
        }

        public List<Banner> getBanners(long series_id) {
            return getBanners(series_id, -1);
        }

        public List<Banner> getBanners(long series_id, int seasonNumber) {
            ArrayList<Banner> banners = new ArrayList<>();
            String selection;
            if (seasonNumber < 0) {
                selection = "series_id=" + series_id;
            } else {
                selection = String.format(Locale.US, "series_id=%d AND type='season' " +
                        "AND type2='season' AND season=%d", series_id, seasonNumber);
            }
            Cursor c = mResolver.query(mUris.tvBanners(), new String[]{"path", "type",
                    "type2", "rating", "rating_count", "thumb_path", "season", "_id"},
                    selection, null, "rating DESC");
            try {
                if (c != null && c.moveToFirst()) {
                    do {
                        String path = c.getString(0);
                        String type = c.getString(1);
                        String type2 = c.getString(2);
                        Float rating = c.isNull(3) ? null : c.getFloat(3);
                        Integer ratingCount = c.isNull(4) ? null : c.getInt(4);
                        String thumbPath = c.getString(5);
                        Integer season = c.isNull(6) ? null : c.getInt(6);
                        Long id = c.getLong(7);
                        banners.add(new Banner(id, path, type, type2, rating,
                                ratingCount, thumbPath, season));
                    } while (c.moveToNext());
                }
                return banners;
            } finally {
                closeCursor(c);
            }
        }

        public Uri insertActor(long seriesId, Actor actor) {
            ContentValues values = new ContentValues(10);
            values.put("_id", actor.getId());
            values.put("_display_name", actor.getName());
            values.put("role", actor.getRole());
            values.put("sort_order", actor.getSortOrder());
            if (!StringUtils.isEmpty(actor.getImagePath())) {
                values.put("image_path", actor.getImagePath());
            }
            values.put("series_id", seriesId);
            return mResolver.insert(mUris.tvActors(), values);
        }
    }

    public class MovieDbClient {

        public Uri makePosterUri(String base, String path) {
            return Uri.parse(base + "w342" + path);
        }

        public Uri makeBackdropUri(String base, String path) {
            return Uri.parse(base + "w1280" + path);
        }

        public void updateConfig(TMDbConfig config) {
            ContentValues values = new ContentValues(11);
            values.put("image_base_url", config.getImages().getBaseUrl());
            try {
                mResolver.update(mUris.movies(), values, null, null);
                mResolver.update(mUris.movieImages(), values, null, null);
            } catch (SQLiteException e) {
                Timber.e(e, "updateConfig %s", config);
            }
        }

        public void setMovieAssociation(String q, long id) {
            ContentValues contentValues = new ContentValues(2);
            contentValues.put("q", q);
            contentValues.put("movie_id", id);
            mResolver.insert(mUris.movieLookups(), contentValues);
        }

        public long getMovieAssociation(String q) {
            Cursor c = mResolver.query(mUris.movieLookups(),
                    new String[]{"movie_id"}, "q=?", new String[]{q}, null);
            try {
                long id = -1;
                if (c != null && c.moveToFirst()) {
                    id = c.getLong(0);
                }
                return id;
            } finally {
                closeCursor(c);
            }
        }

        public @Nullable Movie getMovie(long id) {
            Cursor c = mResolver.query(mUris.movie(id), new String[] {"_display_name",
                    "overview", "release_date", "poster_path", "backdrop_path"},
                    null, null, null);
            try {
                if (c != null && c.moveToFirst()) {
                    String displayName = c.getString(0);
                    String overview = c.getString(1);
                    String releaseDate = c.getString(2);
                    String posterPath =  c.getString(3);
                    String backdropPath = c.getString(4);
                    return new Movie(id, displayName, null, overview, releaseDate, posterPath, backdropPath);
                }
                return null;
            } finally {
                closeCursor(c);
            }
        }

        public Uri insertMovie(Movie movie, TMDbConfig config) {
            ContentValues values = new ContentValues(10);
            values.put("_id", movie.getId());
            values.put("_display_name", movie.getTitle());
            values.put("overview", movie.getOverview());
            values.put("release_date", movie.getReleaseDate());
            values.put("poster_path", movie.getPosterPath());
            values.put("backdrop_path", movie.getBackdropPath());
            values.put("image_base_url", config.getImages().getSecureBaseUrl());
            return mResolver.insert(mUris.movies(), values);
        }

        public void insertImages(ImageList imageList, TMDbConfig config) {
            int numPosters = imageList.getPosters() != null ? imageList.getPosters().size() : 0;
            int numBackdrops = imageList.getPosters() != null ? imageList.getBackdrops().size() : 0;
            ContentValues[] contentValues = new ContentValues[numPosters+numBackdrops];
            int idx = 0;
            if (numPosters > 0) {
                for (Image image : imageList.getPosters()) {
                    ContentValues values = makeImageValues(image);
                    values.put("movie_id", imageList.getId());
                    values.put("image_base_url", config.getImages().getSecureBaseUrl());
                    values.put("image_type", "poster");
                    contentValues[idx++] = values;
                }
            }
            if (numBackdrops > 0) {
                for (Image image : imageList.getBackdrops()) {
                    ContentValues values = makeImageValues(image);
                    values.put("movie_id", imageList.getId());
                    values.put("image_base_url", config.getImages().getSecureBaseUrl());
                    values.put("image_type", "backdrop");
                    contentValues[idx++] = values;
                }
            }
            mResolver.bulkInsert(mUris.movieImages(), contentValues);
        }

        ContentValues makeImageValues(Image image) {
            ContentValues values = new ContentValues(10);
            values.put("height", image.getHeight());
            values.put("width", image.getWidth());
            values.put("file_path", image.getFilePath());
            values.put("vote_average", image.getVoteAverage());
            values.put("vote_count", image.getVoteCount());
            return values;
        }

    }

    static void closeCursor(Cursor c) {
        if (c != null && !c.isClosed()) {
            try {
                c.close();
            } catch (Exception e) {
                Timber.w(e, "closeCursor()");
            }
        }
    }

}
