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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;

import org.opensilk.common.dagger.DaggerService;
import org.opensilk.video.VideoAppComponent;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * Created by drew on 4/1/16.
 */
public class VideosProvider extends ContentProvider {

    @Inject UriMatcher mMatcher;
    @Inject VideosUris mUris;
    @Inject VideosDatabase mDatabase;

    public VideosProvider() {
    }

    @Override
    public boolean onCreate() {
        VideoAppComponent appComponent = DaggerService.getDaggerComponent(getContext());
        VideosProviderModule module = new VideosProviderModule();
        appComponent.newVideosProviderComponent(module).inject(this);
        return true;
    }

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mDatabase.getReadableDatabase();
        if (db == null) {
            Timber.e("Unable to open db");
            return null;
        }
        Cursor c = null;
        String table = null;
        long id = -1;
        switch (mMatcher.match(uri)) {
            case VideosUris.M.TV_SERIES_ONE:
                id = Long.valueOf(uri.getLastPathSegment());
                //fall
            case VideosUris.M.TV_SERIES:
                table = "tv_series";
                break;
            case VideosUris.M.TV_SERIES_SEARCH:
                table = "tv_series_search";
                break;
            case VideosUris.M.TV_EPISODES_ONE:
                id = Long.valueOf(uri.getLastPathSegment());
                //fall
            case VideosUris.M.TV_EPISODES:
                table = "tv_episodes";
                break;
            case VideosUris.M.TV_BANNERS_ONE:
                id = Long.valueOf(uri.getLastPathSegment());
                //fall
            case VideosUris.M.TV_BANNERS:
                table = "tv_banners";
                break;
            case VideosUris.M.TV_ACTORS_ONE:
                id = Long.valueOf(uri.getLastPathSegment());
                //fall
            case VideosUris.M.TV_ACTORS:
                table = "tv_actors";
                break;
            case VideosUris.M.TV_LOOKUPS:
                table = "tv_lookups";
                break;
            case VideosUris.M.TV_EPISODE_DESC_ONE:
                id = Long.valueOf(uri.getLastPathSegment());
                //fall
            case VideosUris.M.TV_EPISODE_DESC:
                table = "tv_episode_series_map";
                break;
            case VideosUris.M.MEDIA_ONE:
                id = Long.valueOf(uri.getLastPathSegment());
                //fall
            case VideosUris.M.MEDIA:
                table = "media";
                break;
            case VideosUris.M.MOVIES_ONE:
                id = Long.valueOf(uri.getLastPathSegment());
                //fall
            case VideosUris.M.MOVIES:
                table = "movies";
                break;
            case VideosUris.M.MOVIE_IMAGES_ONE:
                id = Long.valueOf(uri.getLastPathSegment());
                //fall
            case VideosUris.M.MOVIE_IMAGES:
                table = "movie_images";
                break;
            case VideosUris.M.MOVIE_LOOKUPS:
                table = "movie_lookups";
                break;
            case VideosUris.M.MOVIE_SEARCH:
                table = "movies_search";
                break;
            default:
                Timber.e("Unmatched uri %s", uri);
                break;
        }
        String realSelection = selection;
        if (id != -1) {
            if (selection != null) {
                realSelection = selection + " AND _id=" + id;
            } else {
                realSelection = "_id="+id;
            }
        }
        if (table != null) {
//            Timber.d("Querying table %s proj=%s, sel=%s, args=%s", table,
//                    projection != null ? Arrays.toString(projection) : null,
//                    realSelection, selectionArgs != null ? Arrays.toString(selectionArgs) : null);
            c = db.query(table, projection, realSelection, selectionArgs, null, null, sortOrder);
        }
        return c;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        //        Timber.d("insert %s %s", uri, values);
        SQLiteDatabase db = mDatabase.getWritableDatabase();
        if (db == null) {
            Timber.e("Unable to open db");
            return null;
        }
        switch (mMatcher.match(uri)) {
            case VideosUris.M.TV_SERIES: {
                long id = db.insertWithOnConflict("tv_series", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                return mUris.tvSeries(id);
            }
            case VideosUris.M.TV_EPISODES: {
                long id = db.insertWithOnConflict("tv_episodes", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                return mUris.tvEpisode(id);
            }
            case VideosUris.M.TV_BANNERS: {
                long id = db.insertWithOnConflict("tv_banners", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                return mUris.tvBanner(id);
            }
            case VideosUris.M.TV_ACTORS: {
                long id = db.insertWithOnConflict("tv_actors", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                return mUris.tvActor(id);
            }
            case VideosUris.M.TV_LOOKUPS: {
                long id = db.insertWithOnConflict("tv_lookups", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                return mUris.tvLookups();
            }
            case VideosUris.M.MEDIA: {
                long id = db.insertWithOnConflict("media", null, values, SQLiteDatabase.CONFLICT_FAIL);
                return mUris.media(id);
            }
            case VideosUris.M.MOVIES: {
                long id = db.insertWithOnConflict("movies", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                return mUris.movie(id);
            }
            case VideosUris.M.MOVIE_IMAGES: {
                long id = db.insertWithOnConflict("movie_images", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                return mUris.movieImage(id);
            }
            case VideosUris.M.MOVIE_LOOKUPS: {
                long id = db.insertWithOnConflict("movie_lookups", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                return mUris.movieLookups();
            }
            default:
                Timber.e("Unmatched uri %s", uri);
                return null;
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDatabase.getWritableDatabase();
        if (db == null) {
            Timber.e("Unable to open db");
            return 0;
        }
        Timber.i("delete(%s)", uri);
        switch (mMatcher.match(uri)) {
            case VideosUris.M.MEDIA: {
                return db.delete("media", selection, selectionArgs);
            }
            case VideosUris.M.MEDIA_ONE: {
                if (selection != null) {
                    Timber.w("Ignoring selection %s for delete of %s", selectionArgs, uri);
                }
                return db.delete("media", "_id="+uri.getLastPathSegment(), null);
            }
            default:
                Timber.e("delete not implemented for %s", uri);
                return 0;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDatabase.getWritableDatabase();
        if (db == null) {
            Timber.e("Unable to open db");
            return 0;
        }
        switch (mMatcher.match(uri)) {
            case VideosUris.M.MEDIA: {
                return db.updateWithOnConflict("media", values, selection, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            case VideosUris.M.MEDIA_ONE: {
                long id = Long.valueOf(uri.getLastPathSegment());
                String realSel = selection;
                if (realSel == null) {
                    realSel = "_id="+id;
                } else {
                    realSel = selection + " AND _id="+id;
                }
                return db.updateWithOnConflict("media", values, realSel, selectionArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
            case VideosUris.M.MOVIES: {
                return db.update("movies", values, selection, selectionArgs);
            }
            case VideosUris.M.MOVIE_IMAGES: {
                return db.update("movie_images", values, selection, selectionArgs);
            }
            default:
                Timber.e("Unmatched uri %s", uri);
                return 0;
        }
    }

}
