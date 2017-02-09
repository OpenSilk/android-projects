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
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.ServiceScope;
import org.opensilk.tvdb.api.TVDb;
import org.opensilk.tvdb.api.model.AllZipped;
import org.opensilk.tvdb.api.model.Banner;
import org.opensilk.tvdb.api.model.Episode;
import org.opensilk.tvdb.api.model.Series;
import org.opensilk.video.VideoAppPreferences;
import org.opensilk.video.util.Utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import timber.log.Timber;

/**
 * Created by drew on 4/11/16.
 */
@ServiceScope
public class TVDbLookup extends LookupService {

    final String mTVDbApiKey;
    final TVDb mApi;
    final VideosProviderClient mClient;
    final VideoAppPreferences mPreferences;

    final Map<Long, AllZipped> mSeenSeries = Collections.synchronizedMap(new HashMap<>());
    final Map<String, Long> mSeriesAssociations = Collections.synchronizedMap(new HashMap<>());

    @Inject
    public TVDbLookup(
            @Named("tvdb_api_key") String mTVDbApiKey,
            TVDb mApi,
            VideosProviderClient mClient,
            VideoAppPreferences mPreferences
    ) {
        this.mTVDbApiKey = mTVDbApiKey;
        this.mApi = mApi;
        this.mClient = mClient;
        this.mPreferences = mPreferences;
    }

    Observable<Boolean> mUpdateObservable;
    synchronized Observable<Boolean> getUpdate() {
        if (mUpdateObservable == null) {
            mUpdateObservable = Observable.defer(() -> {
                waitTurn();
                long lastUpdate = mPreferences.getLong("tvdb_lastupdate", 0);
                if (lastUpdate > 0) {
                    return mApi.updatesObservable("all", lastUpdate).flatMap(updates -> {
                        if (updates.getSeries() != null && updates.getSeries().size() > 0) {
                            //fetch all the updates and return true
                            return Observable.mergeDelayError(Observable.from(updates.getSeries())
                                    .filter(seriesId -> mClient.tvdb().getSeries(seriesId) != null)
                                    .map(seriesId -> fetchAllZipped(seriesId).retry(1))
                            ).doOnNext(allZipped -> {
                                Timber.d("Updated series=%s", allZipped.getSeries().getSeriesName());
                                //ignore returned allseries only notify if there were any updates
                            }).isEmpty().map(empty -> !empty).doOnCompleted(() -> {
                                //only update if successfully fetched all series
                                mPreferences.putLong("tvdb_lastupdate", updates.getTime());
                            })
                            //if failed skip update and wait for next run
                            .onExceptionResumeNext(Observable.just(false));
                        } else {
                            //no updates, return false;
                            return Observable.just(false);
                        }
                    });
                } else {
                    //get sentinel and return true
                    return mApi.updatesObservable("none").doOnNext(updates -> {
                        mPreferences.putLong("tvdb_lastupdate", updates.getTime());
                    }).map(updates -> true);
                }
            }).replay(1).autoConnect();
        }
        return mUpdateObservable;
    }

    @Override
    public rx.Observable<MediaBrowser.MediaItem> lookup(final MediaBrowser.MediaItem mediaItem) {
        final MediaDescription description = mediaItem.getDescription();
        final MediaMetaExtras metaExtras = MediaMetaExtras.from(description);
        final String mediaTitle = MediaDescriptionUtil.getMediaTitle(description);
        final String query = Utils.extractSeriesName(mediaTitle);
        if (StringUtils.isEmpty(query)) {
            return rx.Observable.error(new Exception("Failed to extract series name from " + mediaTitle));
        }
        final int seasonNumber = Utils.extractSeasonNumber(mediaTitle);
        final int episodeNumber = Utils.extractEpisodeNumber(mediaTitle);
        if (seasonNumber < 0 || episodeNumber < 0) {
            return rx.Observable.error(new Exception("Failed to extract season and episode from " + mediaTitle));
        }

        rx.Observable<MediaBrowser.MediaItem> cacheObservable = rx.Observable.create(subscriber -> {
//            Timber.d("lookup(%s)", mediaTitle);
            Long series_id = mSeriesAssociations.get(query);
            if (series_id == null) {
                series_id = mClient.tvdb().getSeriesAssociation(query);
                if (series_id != -1) {
                    mSeriesAssociations.put(query, series_id);
                }
            }
            if (series_id != -1) {
                AllZipped allZipped = mSeenSeries.get(series_id);
                if (allZipped == null) {
                    Series series = mClient.tvdb().getSeries(series_id);
                    List<Episode> episodes = mClient.tvdb().getEpisodes(series_id);
                    List<Banner> banners = mClient.tvdb().getBanners(series_id);
                    if (series != null && !episodes.isEmpty()) {
                        allZipped = new AllZipped.Builder()
                                .setSeries(series)
                                .setEpisodes(episodes)
                                .setBanners(banners)
                                .build();
                        mSeenSeries.put(series_id, allZipped);
                    }
                }
                if (allZipped != null){
                    if (subscriber.isUnsubscribed()) {
                        return;
                    }
                    subscriber.onNext(buildMediaItem(mediaItem, allZipped, seasonNumber, episodeNumber));
                    subscriber.onCompleted();
                    return;
                }
            }
            if (subscriber.isUnsubscribed()) {
                return;
            }
            subscriber.onError(new Exception("Missed cache q=" + query));
        });

        Observable<MediaBrowser.MediaItem> networkObservable = rx.Observable.defer(() -> {
            waitTurn();
            Timber.d("lookup(%s)->[series=%s]", mediaTitle, query);
            return mApi.getSeriesObservable(query, "en")
                    .flatMap(seriesList -> {
                        List<Series> series = seriesList.getSeries();
                        if (series != null && !series.isEmpty()) {
                            Series s = series.get(0);
                            return fetchAllZipped(s.getId())
                                    .doOnNext(allZipped -> {
                                        //TODO just do this up there ^ ??
                                        mClient.tvdb().setSeriesAssociation(query, allZipped.getSeries().getId());
                                        mSeriesAssociations.put(query, allZipped.getSeries().getId());
                                    });
                        } else {
                            return Observable.error(new Exception("no results"));
                        }
                    }).map(allZipped -> {
                        return buildMediaItem(mediaItem, allZipped,
                                seasonNumber, episodeNumber);
                    });
        });

        return getUpdate().flatMap(wasUpdated -> {
            if (!metaExtras.isIndexed() || wasUpdated) {
                return cacheObservable.onExceptionResumeNext(networkObservable);
            } else {
                return Observable.empty();
            }
        });
    }

    Observable<AllZipped> fetchAllZipped(long seriesId) {
        return Observable.defer(() -> {
            waitTurn();
            return mApi.allZippedObservable(mTVDbApiKey, seriesId, "en");
        }).doOnNext(allZipped -> {
            mClient.tvdb().insertAllZipped(allZipped);
            mSeenSeries.put(allZipped.getSeries().getId(), allZipped);
        });
    }

    @Nullable MediaBrowser.MediaItem buildMediaItem(
            MediaBrowser.MediaItem mediaItem, AllZipped allZipped,
            int seasonNumber, int episodeNumber
    ) {
        MediaDescription description = mediaItem.getDescription();
        MediaMetaExtras metaExtras = MediaMetaExtras.from(description);
        MediaDescription.Builder builder = MediaDescriptionUtil.newBuilder(description);

        Series series = allZipped.getSeries();
        metaExtras.setSeriesId(series.getId());
        metaExtras.setIndexed(true);

        if (!StringUtils.isEmpty(series.getPosterPath())){
            builder.setIconUri(mClient.tvdb().makeBannerUri(series.getPosterPath()));
        }

        Episode episode = findEpisode(allZipped.getEpisodes(), seasonNumber, episodeNumber);
        if (episode != null) {
            metaExtras.setEpisodeId(episode.getId());
            builder.setTitle(episode.getEpisodeName());
            builder.setSubtitle(mClient.tvdb().makeSubtitle(series.getSeriesName(),
                    episode.getSeasonNumber(), episode.getEpisodeNumber()));
        }
        Banner banner = findBanner(allZipped.getBanners(), seasonNumber);
        if (banner != null) {
            builder.setIconUri(mClient.tvdb().makeBannerUri(banner.getBannerPath()));
        }
        Banner backdrop = findBackdrop(allZipped.getBanners());
        if (backdrop != null) {
            metaExtras.setBackdropUri(mClient.tvdb().makeBannerUri(backdrop.getBannerPath()));
        }
        builder.setExtras(metaExtras.getBundle());
        MediaBrowser.MediaItem newMediaItem = new MediaBrowser.MediaItem(builder.build(), mediaItem.getFlags());
//        Timber.d("lookup(%s)->buildMediaItem(series=%s)", mediaTitle, series.getSeriesName());
//            mClient.insertMedia(newMediaItem);
        return newMediaItem;
//            mDataService.notifyChange(newMediaItem);
    }

    @Nullable Episode findEpisode(List<Episode> episodes, int seasonNumber, int episodeNumber) {
        for (Episode episode : episodes) {
            if(episode.getSeasonNumber() == seasonNumber
                    && episode.getEpisodeNumber() == episodeNumber) {
                return episode;
            }
        }
        return null;
    }

    @Nullable Banner findBanner(List<Banner> banners, int seasonNumber) {
        Banner bestMatch = null;
        for (Banner banner : banners) {
            if (banner.isSeason() && banner.getSeason() != null
                    && banner.getSeason() == seasonNumber) {
                if (bestMatch != null) {
                    if (bestMatch.getRating() == null && banner.getRating() != null) {
                        bestMatch = banner;
                    } else if (bestMatch.getRating() != null && banner.getRating() != null
                            && bestMatch.getRating() <  banner.getRating()) {
                        bestMatch = banner;
                    }
                } else {
                    bestMatch = banner;
                }
            }
        }
        return bestMatch;
    }

    @Nullable Banner findBackdrop(List<Banner> banners) {
        Banner bestMatch = null;
        for (Banner banner : banners) {
            if (!banner.isFanart()) {
                continue;
            }
            if (bestMatch == null) {
                bestMatch = banner;
                continue;
            }
            if (StringUtils.equals(banner.getBannerType2(), "1920x1080") &&
                    !StringUtils.equals(bestMatch.getBannerType2(), "1920x1080")) {
                bestMatch = banner;
                continue;
            }
            if (bestMatch.getRating() == null && banner.getRating() != null) {
                bestMatch = banner;
                continue;
            }
            if (bestMatch.getRating() != null && banner.getRating() != null) {
                if (bestMatch.getRating() < banner.getRating()) {
                    bestMatch = banner;
                }
            }
        }
        return bestMatch;
    }

}
