package org.opensilk.video

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.CancellationSignal
import dagger.Binds
import dagger.Module
import org.opensilk.common.dagger.ForApplication
import org.opensilk.media.MediaRef
import org.opensilk.media.playback.MediaProviderClient
import org.opensilk.tmdb.api.model.Image
import org.opensilk.tmdb.api.model.ImageList
import org.opensilk.tmdb.api.model.Movie
import org.opensilk.tmdb.api.model.TMDbConfig
import org.opensilk.tvdb.api.model.*
import rx.Single
import rx.Observable
import rx.Subscriber
import rx.Subscription
import rx.subscriptions.Subscriptions
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Module
abstract class MediaProviderModule {
    @Binds abstract fun providerClient(providerClient: DatabaseClient): MediaProviderClient
}

/**
 *
 */
class NoSuchItemException: Exception()

fun Int.zeroPad(len: Int): String {
    return this.toString().padStart(len, '0')
}

/**
 * A bridge for testing
 */
internal interface ContentResolverGlue {
    fun insert(uri: Uri, values: ContentValues): Uri?
    fun bulkInsert(uri: Uri, values: Array<ContentValues?>): Int
    fun update(uri: Uri, values: ContentValues, where: String?,
               selectionArgs: Array<String>?): Int
    fun query(uri: Uri, projection: Array<String>?, selection: String?,
              selectionArgs: Array<String>?, sortOrder: String?,
              cancellationSignal: CancellationSignal?): Cursor?
    fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int
}

/**
 * The default implementation passes through to the system ContentResolver
 */
private class DefaultContentResolverGlue(private val mResolver: ContentResolver): ContentResolverGlue {
    override fun insert(uri: Uri, values: ContentValues): Uri? {
        return mResolver.insert(uri, values)
    }

    override fun bulkInsert(uri: Uri, values: Array<ContentValues?>): Int {
        return mResolver.bulkInsert(uri, values)
    }

    override fun update(uri: Uri, values: ContentValues, where: String?,
                        selectionArgs: Array<String>?): Int {
        return mResolver.update(uri, values, where, selectionArgs)
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?,
                       cancellationSignal: CancellationSignal?): Cursor? {
        return mResolver.query(uri, projection, selection, selectionArgs,
                sortOrder, cancellationSignal)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return mResolver.delete(uri, selection, selectionArgs)
    }
}

fun <T> Subscriber<T>.cancellationSignal(): CancellationSignal {
    val cancelation = CancellationSignal()
    this.add(Subscriptions.create { cancelation.cancel() })
    return cancelation
}

/**
 * Created by drew on 7/18/17.
 */
@Singleton
class DatabaseClient
@Inject constructor(
    @ForApplication context: Context,
    private val mUris: DatabaseUris,
    @Named("TVDBRoot") tvdbRootUri: Uri
) : MediaProviderClient {

    internal var mResolver: ContentResolverGlue = DefaultContentResolverGlue(context.contentResolver)
    val tvdb = TVDbClient(tvdbRootUri)
    val tmdb = MovieDbClient()

    override fun getMediaItem(mediaRef: MediaRef): Single<MediaBrowser.MediaItem> {
        TODO("not implemented")
    }

    fun getVideoDescription(mediaRef: MediaRef): Single<VideoDescInfo> {
        return Single.create { s ->
            s.onError(NoSuchItemException())
        }
    }

    inner class TVDbClient(private val tvdbRoot: Uri) {

        fun rootUri(): Uri {
            return tvdbRoot
        }

        fun bannerRootUri(): Uri {
            return Uri.withAppendedPath(tvdbRoot, "banners/")
        }

        fun makeBannerUri(path: String): Uri {
            return Uri.withAppendedPath(bannerRootUri(), path)
        }

        fun makeSubtitle(seriesName: String, seasonNumber: Int, episodeNumber: Int): String {
            return "$seriesName - S${seasonNumber.zeroPad(2)}E${episodeNumber.zeroPad(2)}"
        }

        fun insertAllZipped(allZipped: AllZipped) {
            insertTvSeries(allZipped.series)
            if (allZipped.episodes != null && !allZipped.episodes.isEmpty()) {
                for (episode in allZipped.episodes) {
                    insertTvEpisode(episode)
                }
            }
            if (allZipped.banners != null && !allZipped.banners.isEmpty()) {
                for (banner in allZipped.banners) {
                    insertTvBanner(allZipped.series.id!!, banner)
                }
            }
            if (allZipped.actors != null && !allZipped.actors.isEmpty()) {
                for (actor in allZipped.actors) {
                    insertTvActor(allZipped.series.id!!, actor)
                }
            }
        }

        fun insertTvSeries(series: Series): Uri {
            val values = ContentValues(10)
            values.put("_id", series.id)
            values.put("_display_name", series.seriesName)
            if (!series.overview.isNullOrEmpty()) {
                values.put("overview", series.overview)
            }
            if (!series.firstAired.isNullOrEmpty()) {
                values.put("first_aired", series.firstAired)
            }
            if (!series.posterPath.isNullOrEmpty()) {
                values.put("poster_path", series.posterPath)
            }
            if (!series.fanartPath.isNullOrEmpty()) {
                values.put("backdrop_path", series.fanartPath)
            }
            return mResolver.insert(mUris.tvSeries(), values) ?: Uri.EMPTY
        }

        fun getTvSeries(): Observable<Series> {
            return Observable.create { s ->
                mResolver.query(mUris.tvSeries(),
                        arrayOf("_display_name", "overview", "first_aired",
                                "poster_path", "backdrop_path", "_id"), null, null, null,
                        s.cancellationSignal())?.use { c ->
                    if (c.moveToFirst()) {
                        do {
                            val displayName = c.getString(0)
                            val overview = c.getString(1)
                            val firstAired = c.getString(2)
                            val posterPath = c.getString(3)
                            val backdropPath = c.getString(4)
                            val id = c.getLong(5)
                            s.onNext(Series(id, displayName, overview,
                                    backdropPath, posterPath, firstAired))
                        } while (c.moveToNext())
                        s.onCompleted()
                        return@create
                    }
                }
                s.onError(NoSuchItemException())
            }
        }

        fun insertTvEpisode(episode: Episode): Uri {
            val values = ContentValues(10)
            values.put("_id", episode.id)
            if (!episode.episodeName.isNullOrEmpty()) {
                values.put("_display_name", episode.episodeName)
            }
            if (!episode.overview.isNullOrEmpty()) {
                values.put("overview", episode.overview)
            }
            if (!episode.firstAired.isNullOrEmpty()) {
                values.put("first_aired", episode.firstAired)
            }
            values.put("episode_number", episode.episodeNumber)
            values.put("season_number", episode.seasonNumber)
            if (episode.seasonId != null) {
                values.put("season_id", episode.seasonId)
            }
            values.put("series_id", episode.seriesId)
            return mResolver.insert(mUris.tvEpisodes(), values) ?: Uri.EMPTY
        }

        fun getTvEpisodes(): Observable<Episode> {
            return Observable.create { s ->
                mResolver.query(mUris.tvEpisodes(),
                        arrayOf("_id", "_display_name", "first_aired",
                                "episode_number", "season_number", "series_id",
                                "overview", "season_id"), null, null, null, s.cancellationSignal())?.use { c ->
                    if (c.moveToFirst()) {
                        do {
                            val id = c.getLong(0)
                            val displayName = c.getString(1)
                            val firstAired = c.getString(2)
                            val episode = c.getInt(3)
                            val season = c.getInt(4)
                            val seriesId = c.getLong(5)
                            val overview = c.getString(6)
                            val seasonId = if (c.isNull(7)) null else c.getLong(7)
                            s.onNext(Episode(id, displayName, firstAired, overview,
                                    episode, season, seasonId, seriesId))
                        } while (c.moveToNext())
                        s.onCompleted()
                        return@create
                    }
                }
                s.onError(NoSuchItemException())
            }
        }

        fun insertTvBanner(seriesId: Long, banner: Banner): Uri {
            val values = ContentValues(10)
            values.put("_id", banner.id)
            values.put("path", banner.bannerPath)
            values.put("type", banner.bannerType)
            values.put("type2", banner.bannerType2)
            if (banner.rating != null) {
                values.put("rating", banner.rating)
            }
            if (banner.ratingCount != null) {
                values.put("rating_count", banner.ratingCount)
            }
            if (banner.thumbnailPath != null) {
                values.put("thumb_path", banner.thumbnailPath)
            }
            if (banner.season != null) {
                values.put("season", banner.season)
            }
            values.put("series_id", seriesId)
            return mResolver.insert(mUris.tvBanners(), values) ?: Uri.EMPTY
        }

        fun getTvBanners(series_id: Long, seasonNumber: Int = -1): rx.Observable<Banner> {
            return rx.Observable.create { s ->
                val selection = if (seasonNumber < 0) {
                    "series_id=$series_id"
                } else {
                    "series_id=$series_id AND type='season' AND type2='season' AND season=$seasonNumber"
                }
                mResolver.query(mUris.tvBanners(), arrayOf("path", "type", "type2", "rating",
                        "rating_count", "thumb_path", "season", "_id"),
                        selection, null, "rating DESC", s.cancellationSignal())?.use { c ->
                    if (c.moveToFirst()) {
                        do {
                            val path = c.getString(0)
                            val type = c.getString(1)
                            val type2 = c.getString(2)
                            val rating = if (c.isNull(3)) null else c.getFloat(3)
                            val ratingCount = if (c.isNull(4)) null else c.getInt(4)
                            val thumbPath = c.getString(5)
                            val season = if (c.isNull(6)) null else c.getInt(6)
                            val id = c.getLong(7)
                            s.onNext(Banner(id, path, type, type2, rating,
                                    ratingCount, thumbPath, season))
                        } while (c.moveToNext())
                        s.onCompleted()
                        return@create
                    }
                }
                s.onError(NoSuchItemException())
            }
        }

        fun insertTvActor(seriesId: Long, actor: Actor): Uri {
            val values = ContentValues(10)
            values.put("_id", actor.id)
            values.put("_display_name", actor.name)
            values.put("role", actor.role)
            values.put("sort_order", actor.sortOrder)
            if (!actor.imagePath.isNullOrEmpty()) {
                values.put("image_path", actor.imagePath)
            }
            values.put("series_id", seriesId)
            return mResolver.insert(mUris.tvActors(), values) ?: Uri.EMPTY
        }

        fun getTvActors(series_id: Long): Observable<Actor> {
            return Observable.create { s ->
                mResolver.query(mUris.tvActors(), arrayOf("_id", "_display_name", "role", "sort_order",
                        "image_path"), "series_id=$series_id", null, "sort_order DESC", s.cancellationSignal())?.use { c ->
                    if (c.moveToFirst()) {
                        do {
                            val id = c.getLong(0)
                            val name = c.getString(1)
                            val role = c.getString(2)
                            val sort = if (c.isNull(3)) null else c.getInt(3)
                            val img = c.getString(4)
                            s.onNext(Actor(id, name, role, sort, img))
                        } while (c.moveToNext())
                        s.onCompleted()
                        return@create
                    }
                }
                s.onError(NoSuchItemException())
            }
        }

    }

    inner class MovieDbClient {

        fun makePosterUri(base: String, path: String): Uri {
            return Uri.parse("${base}w342$path")
        }

        fun makeBackdropUri(base: String, path: String): Uri {
            return Uri.parse("${base}w1280$path")
        }

        fun updateConfig(config: TMDbConfig) {
            val values = ContentValues()
            values.put("image_base_url", config.images.baseUrl)
            try {
                mResolver.update(mUris.movies(), values, null, null)
                mResolver.update(mUris.movieImages(), values, null, null)
            } catch (e: SQLiteException) {
                Timber.e(e, "updateConfig %s", config)
            }
        }

        fun setMovieAssociation(q: String, id: Long) {
            val contentValues = ContentValues(2)
            contentValues.put("q", q)
            contentValues.put("movie_id", id)
            mResolver.insert(mUris.movieLookups(), contentValues)
        }

        fun insertMovie(movie: Movie, config: TMDbConfig): Uri {
            val values = ContentValues(10)
            values.put("_id", movie.id)
            values.put("_display_name", movie.title)
            values.put("overview", movie.overview)
            values.put("release_date", movie.releaseDate)
            values.put("poster_path", movie.posterPath)
            values.put("backdrop_path", movie.backdropPath)
            values.put("image_base_url", config.images.secureBaseUrl)
            return mResolver.insert(mUris.movies(), values) ?: Uri.EMPTY
        }

        fun insertImages(imageList: ImageList, config: TMDbConfig) {
            val numPosters = if (imageList.posters != null) imageList.posters.size else 0
            val numBackdrops = if (imageList.posters != null) imageList.backdrops.size else 0
            val contentValues = arrayOfNulls<ContentValues>(numPosters + numBackdrops)
            var idx = 0
            if (numPosters > 0) {
                for (image in imageList.posters) {
                    val values = makeImageValues(image)
                    values.put("movie_id", imageList.id)
                    values.put("image_base_url", config.images.secureBaseUrl)
                    values.put("image_type", "poster")
                    contentValues[idx++] = values
                }
            }
            if (numBackdrops > 0) {
                for (image in imageList.backdrops) {
                    val values = makeImageValues(image)
                    values.put("movie_id", imageList.id)
                    values.put("image_base_url", config.images.secureBaseUrl)
                    values.put("image_type", "backdrop")
                    contentValues[idx++] = values
                }
            }
            mResolver.bulkInsert(mUris.movieImages(), contentValues)
        }

        internal fun makeImageValues(image: Image): ContentValues {
            val values = ContentValues(10)
            values.put("height", image.height)
            values.put("width", image.width)
            values.put("file_path", image.filePath)
            values.put("vote_average", image.voteAverage)
            values.put("vote_count", image.voteCount)
            return values
        }

    }

}
